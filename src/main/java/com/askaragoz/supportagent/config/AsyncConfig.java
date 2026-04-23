package com.askaragoz.supportagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.util.concurrent.Executor;

/**
 * Configures the executor used for @Async method calls.
 *
 * AsyncConfigurer is a Spring interface that lets you override the default @Async executor.
 * Implementing getAsyncExecutor() here tells Spring to use this executor for every method
 * annotated with @Async in the application.
 *
 * WHY VIRTUAL THREADS:
 * Traditional thread pools (e.g. ThreadPoolExecutor with 10-200 threads) block an OS thread
 * for the entire duration of an async task. When that task calls Claude's API and waits for
 * a response, that OS thread sits idle — wasting a scarce resource.
 *
 * Virtual threads (Java 21 Project Loom) are JVM-managed coroutines. They are:
 *   - Cheap: you can have hundreds of thousands simultaneously (vs ~1000s of OS threads)
 *   - Blocking-friendly: when a virtual thread blocks (e.g. HTTP call to Claude), the JVM
 *     parks it and reuses the underlying OS carrier thread for other work — no wasted capacity
 *
 * Combined with spring.threads.virtual.enabled=true (which also applies virtual threads to
 * Tomcat's HTTP request handling), the entire application runs on virtual threads end-to-end.
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("ai-async-");
        // setVirtualThreads(true) tells the executor to spawn a new virtual thread
        // for each @Async invocation instead of using a pooled OS thread.
        executor.setVirtualThreads(true);
        return executor;
    }
}
