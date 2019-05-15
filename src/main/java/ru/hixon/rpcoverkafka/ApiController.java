package ru.hixon.rpcoverkafka;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

@RestController
@RequestMapping(path = "/api")
public class ApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiController.class);

    private final ExecutorService completableFutureExecutorService = Executors.newSingleThreadExecutor();

    private final ExecutorService producerExecutorService = Executors.newFixedThreadPool(3);

    private final ConcurrentHashMap<String, CompletableFuture<byte[]>> awaitingCompletableFutures = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        Thread kafkaConsumerThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    LOGGER.info("Consuming from Kafka...");

                    awaitingCompletableFutures.computeIfPresent("42", new BiFunction<String, CompletableFuture<byte[]>, CompletableFuture<byte[]>>() {
                        @Override
                        public CompletableFuture<byte[]> apply(String messageId, CompletableFuture<byte[]> completableFuture) {
                            LOGGER.info("Needed response is received");
                            completableFuture.complete("Message from kafka".getBytes());
                            return null;
                        }
                    });

                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        kafkaConsumerThread.setDaemon(true);
        kafkaConsumerThread.setName("Kafka-consumer-thread");
        kafkaConsumerThread.start();
    }

    private static class KafkaProducerTask implements Runnable {

        private final byte[] message;

        private KafkaProducerTask(byte[] message) {
            this.message = message;
        }

        @Override
        public void run() {
            String messageAsString = new String(message);
            LOGGER.info("Publish a message to Kafka: {}", messageAsString);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                LOGGER.error("Cannot publish a message to Kafka: {}", messageAsString, e);
            }
        }
    }

    @PostMapping(path = "echo")
    public Mono<byte[]> echo(@RequestBody byte[] message){

        producerExecutorService.submit(new KafkaProducerTask(message));

        final CompletableFuture<byte[]> completableFuture = new CompletableFuture<>();
        completableFuture.completeOnTimeout("timeout error".getBytes(), 3, TimeUnit.SECONDS);

        final String messageId = "42";

        awaitingCompletableFutures.put(messageId, completableFuture);

        completableFuture.thenRunAsync(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Remove awaiting completable future");
                awaitingCompletableFutures.remove(messageId);
            }
        }, completableFutureExecutorService);

        return Mono.fromCompletionStage(completableFuture);
    }

}
