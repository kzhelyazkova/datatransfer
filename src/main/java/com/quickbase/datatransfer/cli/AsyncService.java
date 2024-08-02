package com.quickbase.datatransfer.cli;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Service
public class AsyncService {

    public Mono<String> performAsyncTask() {
        /*System.out.println("Simulate a long-running task");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Mono.just("Task Completed!");*/

        return Mono.fromCallable(() -> {
            System.out.println("Simulate a long-running task");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Task Completed!";
        });
    }
}
