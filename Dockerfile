# ── Stage 1: Build ────────────────────────────────────────────────────────────
#
# maven:3.9-eclipse-temurin-21-alpine has both JDK 21 and Maven 3.9 pre-installed.
# Using the Alpine variant keeps the build image lean (we never ship this layer).
#
# WHY TWO STAGES?
# The build stage needs a full JDK + Maven to compile. The runtime stage only
# needs a JRE to execute the JAR. Keeping them separate means the final image
# ships none of the build tooling — only the compiled application.
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# ── Dependency cache optimisation ─────────────────────────────────────────────
# Copy pom.xml BEFORE source code so Docker can cache the dependency download
# layer independently. When only source files change (not pom.xml), Docker reuses
# the cached layer and skips the full download — build time drops from ~3 min to
# ~30 s on subsequent builds.
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# Now copy source and compile. -DskipTests: tests run in CI, not in Docker build.
COPY src ./src
RUN mvn package -DskipTests --no-transfer-progress


# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
#
# eclipse-temurin:21-jre-alpine — JRE only (no compiler, no Maven).
# ~200 MB final image vs ~380 MB if we kept the JDK.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Pull only the compiled JAR from the build stage.
# Everything else (source, JDK, Maven cache) is discarded.
COPY --from=build /app/target/*.jar app.jar

# Document the port the embedded Tomcat listens on.
# Actual port binding is configured in docker-compose.yml or Railway settings.
EXPOSE 8080

# -XX:+UseContainerSupport (default since Java 11):
# The JVM reads Docker's cgroup memory and CPU limits instead of the host
# machine's totals. Without this, the JVM may allocate too much heap and be
# killed with OOMKilled by the container runtime.
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]
