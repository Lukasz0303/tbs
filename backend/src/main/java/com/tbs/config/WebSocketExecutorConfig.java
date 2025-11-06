package com.tbs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

@Configuration
public class WebSocketExecutorConfig {

    @Bean(name = "webSocketScheduler")
    public ScheduledExecutorService webSocketScheduler() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(4, new ThreadFactory() {
            private int counter = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "websocket-handler-" + counter++);
                t.setDaemon(true);
                return t;
            }
        });
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }
}

