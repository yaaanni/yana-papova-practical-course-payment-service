package com.example.PaymentService.dto.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class PaymentRequest {

    @NotNull(message = "Order id is required")
    private Long orderId;

    @NotNull(message = "User id is required")
    private Long userId;

    @NotNull(message = "Payment amount is required")
    @PositiveOrZero
    private BigDecimal paymentAmount;
}
