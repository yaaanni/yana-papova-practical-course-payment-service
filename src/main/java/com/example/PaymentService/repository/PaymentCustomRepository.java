package com.example.PaymentService.repository;

import com.example.PaymentService.entity.Payment;
import com.example.PaymentService.enums.Status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface PaymentCustomRepository {
    List<Payment> search(Long userId, Long orderId, Status status);

    BigDecimal getTotalPaymentsByUserId(Long id, Instant from, Instant to);

    BigDecimal getTotalPayments(Instant from, Instant to);
}
