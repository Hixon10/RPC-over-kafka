package ru.hixon.rpcoverkafka;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.Callable;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RpcOverKafkaApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void contextLoads() {
    }

    @Test
    public void testHello() {
        webTestClient
                // Create a GET request to test an endpoint
                .post().uri("/api/echo")
                .body(Mono.fromCallable(new Callable<byte[]>() {
                    @Override
                    public byte[] call() throws Exception {
                        return "hello".getBytes();
                    }
                }), byte[].class)
                .exchange()
                // and use the dedicated DSL to test assertions against the response
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("Message from kafka");
    }
}
