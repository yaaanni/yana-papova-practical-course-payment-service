package com.example.PaymentService.service;

import com.example.PaymentService.client.PaymentClient;
import com.example.PaymentService.dto.payment.PaymentRequest;
import com.example.PaymentService.dto.payment.PaymentResponse;
import com.example.PaymentService.entity.Payment;
import com.example.PaymentService.enums.Status;
import com.example.PaymentService.exception.IllegalStatusException;
import com.example.PaymentService.kafka.dto.OutboxRequest;
import com.example.PaymentService.kafka.service.OutboxService;
import com.example.PaymentService.mapper.PaymentMapper;
import com.example.PaymentService.repository.PaymentRepository;
import com.example.PaymentService.security.model.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentClient paymentClient;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OutboxService outboxService;

    @Transactional
    public PaymentResponse create(PaymentRequest request) {
        Integer randomNumber = paymentClient.getRandomNumber();

        Payment payment = paymentMapper.toEntity(request);

        if (randomNumber % 2 == 0) {
            payment.setStatus(Status.SUCCESS);
        } else {
            payment.setStatus(Status.FAILED);
        }

        Payment saved = paymentRepository.save(payment);

        OutboxRequest event = new OutboxRequest();
        event.setEventType("CREATE_PAYMENT");
        event.setStatus("NEW");
        event.setPayload(Map.of(
                "paymentAmount", saved.getPaymentAmount(),
                "userId", saved.getUserId(),
                "orderId", saved.getOrderId(),
                "paymentStatus", payment.getStatus().name()
        ));


        outboxService.create(event);

        return paymentMapper.toResponse(saved);
    }

    public List<PaymentResponse> search(Long userId, Long orderId, String status, AuthUser authUser) {
        Status enumStatus = null;
        if (status != null) {
            try {
                enumStatus = Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalStatusException();
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            userId = authUser.getUserId();
        }

        List<Payment> payments = paymentRepository.search(userId, orderId, enumStatus);

        return payments.stream()
                .map(p -> paymentMapper.toResponse(p))
                .toList();
    }

    public BigDecimal getTotalPaymentsByUserId(Long userId, Instant from, Instant to, AuthUser authUser) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            userId = authUser.getUserId();
        }

        return paymentRepository.getTotalPaymentsByUserId(userId, from, to);
    }

    public BigDecimal getTotalPayments(Instant from, Instant to) {
        return paymentRepository.getTotalPayments(from, to);
    }
}
