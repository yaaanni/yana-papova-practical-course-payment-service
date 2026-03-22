package com.example.PaymentService.client;

import java.time.Duration;
import reactor.util.retry.Retry;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PaymentClient {

    private final WebClient webClient;

    public PaymentClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public int getRandomNumber() {
        String response = webClient.get()
                .uri("/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new")
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
                .block();

        return Integer.parseInt(response.trim());
    }
}
