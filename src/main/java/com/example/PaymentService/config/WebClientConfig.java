package com.example.PaymentService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient paymentServiceClient() {

        return WebClient.builder()
                .baseUrl("https://www.random.org")
                .build();
    }
}
