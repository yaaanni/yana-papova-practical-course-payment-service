package com.example.PaymentService.dto.payment;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class PaymentResponse {

    private String id;
    private Long orderId;
    private Long userId;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private BigDecimal paymentAmount;
}
