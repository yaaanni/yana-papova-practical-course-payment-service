package com.example.PaymentService.service;

import com.example.PaymentService.client.PaymentClient;
import com.example.PaymentService.dto.PaymentRequest;
import com.example.PaymentService.dto.PaymentResponse;
import com.example.PaymentService.entity.Payment;
import com.example.PaymentService.enums.Status;
import com.example.PaymentService.exception.IllegalStatusException;
import com.example.PaymentService.kafka.event.PaymentCreatedEvent;
import com.example.PaymentService.kafka.producer.PaymentProducer;
import com.example.PaymentService.mapper.PaymentMapper;
import com.example.PaymentService.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentClient paymentClient;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentProducer paymentProducer;

    @Transactional
    public PaymentResponse create(PaymentRequest request) {
        Integer randomNumber = paymentClient.getRandomNumber();

        System.out.println("random number: " + randomNumber);

        Payment payment = paymentMapper.toEntity(request);

        if (randomNumber % 2 == 0) {
            payment.setStatus(Status.SUCCESS);
        } else {
            payment.setStatus(Status.FAILED);
        }

        Payment saved = paymentRepository.save(payment);

        PaymentCreatedEvent event = new PaymentCreatedEvent(saved.getId(), saved.getStatus().name());

        paymentProducer.sendPaymentCreatedEvent(event);

        return paymentMapper.toResponse(saved);
    }

    public List<PaymentResponse> search(Long userId, Long orderId, String status) {
        Status enumStatus = null;
        if (status != null) {
            try {
                enumStatus = Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalStatusException("Invalid request status");
            }
        }

        List<Payment> payments = paymentRepository.search(userId, orderId, enumStatus);

        return payments.stream()
                .map(p -> paymentMapper.toResponse(p))
                .toList();
    }

    public BigDecimal getTotalPaymentsByUserId(Long id, Instant from, Instant to) {
        return paymentRepository.getTotalPaymentsByUserId(id, from, to);
    }

    public BigDecimal getTotalPayments(Instant from, Instant to) {
        return paymentRepository.getTotalPayments(from, to);
    }
}
