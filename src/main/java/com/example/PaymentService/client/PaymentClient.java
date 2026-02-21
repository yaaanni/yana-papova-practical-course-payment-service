package com.example.PaymentService.client;

import com.example.PaymentService.config.WebClientConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class PaymentClient {

    private final WebClient webClient;

    public int getRandomNumber() {
        String response = webClient.get()
                .uri("/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return Integer.parseInt(response.trim());
    }
}
