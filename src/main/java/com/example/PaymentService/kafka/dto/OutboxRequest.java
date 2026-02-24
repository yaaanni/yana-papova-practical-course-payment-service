package com.example.PaymentService.kafka.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class OutboxRequest {

    @NotBlank
    private String eventType;

    @NotNull
    private Map<String, Object> payload;

    @NotBlank
    private String status;
}
