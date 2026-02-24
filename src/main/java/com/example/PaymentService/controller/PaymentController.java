package com.example.PaymentService.controller;

import com.example.PaymentService.dto.payment.PaymentRequest;
import com.example.PaymentService.dto.payment.PaymentResponse;
import com.example.PaymentService.security.model.AuthUser;
import com.example.PaymentService.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<PaymentResponse>> search(@RequestParam(required = false) Long userId, @RequestParam(required = false) Long orderId, @RequestParam(required = false) String status, @AuthenticationPrincipal AuthUser authUser) {
        List<PaymentResponse> payments = paymentService.search(userId, orderId, status, authUser);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/total")
    public ResponseEntity<BigDecimal> getTotalPaymentsByUserId(@RequestParam Long id, @RequestParam Instant from, @RequestParam Instant to, @AuthenticationPrincipal AuthUser authUser) {
        BigDecimal total = paymentService.getTotalPaymentsByUserId(id, from, to, authUser);
        return ResponseEntity.ok(total);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/total/all")
    public ResponseEntity<BigDecimal> getTotalPayments(@RequestParam Instant from, @RequestParam Instant to) {
        BigDecimal total = paymentService.getTotalPayments(from, to);
        return ResponseEntity.ok(total);
    }
}
