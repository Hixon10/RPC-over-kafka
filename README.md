# Simple example of RPC over Apache Kafka

## Using technologies:
1. Spring 5 WebFlux (Reactive programming)
2. CompletableFuture 
3. Apache Kafka

## Main advantages
1. Non blocking solution
2. Minimal amount of Threads (like 8 thread in Netty, 3 thread for Kafka producer, 1 thread for Kafka consumer and 1 thread for CompletableFuture cleaner)
