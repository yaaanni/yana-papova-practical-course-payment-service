package com.example.PaymentService.client;

import org.springframework.beans.factory.annotation.Qualifier;
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
                .block();

        return Integer.parseInt(response.trim());
    }
}
