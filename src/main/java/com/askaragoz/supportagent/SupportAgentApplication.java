package com.askaragoz.supportagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Application entry point.
 *
 * @SpringBootApplication is a convenience annotation that combines three annotations:
 *   - @Configuration:          this class can declare @Bean methods
 *   - @EnableAutoConfiguration: Spring Boot scans the classpath and auto-creates beans
 *                               (e.g. DataSource from PostgreSQL driver + application.yml,
 *                                ChatModel from Spring AI Anthropic starter + API key, etc.)
 *   - @ComponentScan:          scans this package and all sub-packages for Spring-managed
 *                               components (@Service, @Repository, @RestController, @Component)
 *
 * @EnableAsync activates Spring's asynchronous method execution support.
 * Without it, any method annotated with @Async would run synchronously — the annotation
 * would be silently ignored. The actual executor (thread pool) is configured in AsyncConfig,
 * which we add in Phase 3 alongside the RagService implementation.
 */
@SpringBootApplication
@EnableAsync
public class SupportAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupportAgentApplication.class, args);
    }
}
