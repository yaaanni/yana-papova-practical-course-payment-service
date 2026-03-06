package com.example.PaymentService.service.payment;

import com.example.PaymentService.client.PaymentClient;
import com.example.PaymentService.dto.payment.PaymentRequest;
import com.example.PaymentService.dto.payment.PaymentResponse;
import com.example.PaymentService.entity.Payment;
import com.example.PaymentService.enums.Status;
import com.example.PaymentService.exception.IllegalStatusException;
import com.example.PaymentService.kafka.dto.OutboxRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.example.PaymentService.kafka.service.OutboxService;
import com.example.PaymentService.mapper.PaymentMapper;
import com.example.PaymentService.repository.PaymentRepository;
import com.example.PaymentService.security.model.AuthUser;
import com.example.PaymentService.service.PaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private OutboxService outboxService;

    @Captor
    private ArgumentCaptor<OutboxRequest> outboxCaptor;

    @InjectMocks
    private PaymentService paymentService;

    private void mockSecurityContext(Long userId, String role) {
        AuthUser authUser = new AuthUser(userId, role);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                authUser,
                null,
                Collections.singleton(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPayment_shouldSavePaymentWithSuccessStatus_whenRandomNumberIsEven() {
        PaymentRequest request = new PaymentRequest();

        Payment payment = new Payment();

        PaymentResponse paymentResponse = new PaymentResponse();

        Payment saved = new Payment();
        saved.setUserId(10L);
        saved.setOrderId(5L);
        saved.setStatus(Status.SUCCESS);
        saved.setPaymentAmount(new BigDecimal(10));

        when(paymentClient.getRandomNumber()).thenReturn(10);
        when(paymentMapper.toEntity(request)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(saved);
        when(paymentMapper.toResponse(saved)).thenReturn(paymentResponse);

        paymentService.create(request);

        assertEquals(Status.SUCCESS, payment.getStatus());

        verify(outboxService).create(outboxCaptor.capture());
        OutboxRequest capturedEvent = outboxCaptor.getValue();

        assertEquals("CREATE_PAYMENT", capturedEvent.getEventType());
        assertEquals("NEW", capturedEvent.getStatus());

        Map<String, Object> payload = capturedEvent.getPayload();
        assertEquals(new BigDecimal("10"), payload.get("paymentAmount"));
        assertEquals(10L, payload.get("userId"));
        assertEquals(5L, payload.get("orderId"));
        assertEquals("SUCCESS", payload.get("paymentStatus"));
    }

    @Test
    void createPayment_shouldSavePaymentWithSuccessStatus_whenRandomNumberIsOdd() {
        PaymentRequest request = new PaymentRequest();

        Payment payment = new Payment();

        PaymentResponse paymentResponse = new PaymentResponse();

        Payment saved = new Payment();
        saved.setUserId(10L);
        saved.setOrderId(5L);
        saved.setStatus(Status.FAILED);
        saved.setPaymentAmount(new BigDecimal(10));

        when(paymentClient.getRandomNumber()).thenReturn(11);
        when(paymentMapper.toEntity(request)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(saved);
        when(paymentMapper.toResponse(saved)).thenReturn(paymentResponse);

        paymentService.create(request);

        assertEquals(Status.FAILED, payment.getStatus());

        verify(outboxService).create(outboxCaptor.capture());
        OutboxRequest capturedEvent = outboxCaptor.getValue();

        assertEquals("CREATE_PAYMENT", capturedEvent.getEventType());
        assertEquals("NEW", capturedEvent.getStatus());

        Map<String, Object> payload = capturedEvent.getPayload();
        assertEquals(new BigDecimal("10"), payload.get("paymentAmount"));
        assertEquals(10L, payload.get("userId"));
        assertEquals(5L, payload.get("orderId"));
        assertEquals("FAILED", payload.get("paymentStatus"));
    }

    @Test
    void search_shouldUseProvidedUserId_whenUserIsAdmin() {
        Long myAuthId = 999L;
        Long requestedUserId = 10L;
        Long orderId = 5L;

        AuthUser authUser = new AuthUser(myAuthId, "ROLE_ADMIN");

        mockSecurityContext(myAuthId, "ROLE_ADMIN");

        Payment payment = new Payment();
        PaymentResponse response = new PaymentResponse();

        when(paymentRepository.search(requestedUserId, orderId, Status.SUCCESS))
                .thenReturn(List.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        List<PaymentResponse> result = paymentService.search(requestedUserId, orderId, "SUCCESS", authUser);

        assertEquals(1, result.size());
        assertEquals(response, result.get(0));

        verify(paymentRepository).search(requestedUserId, orderId, Status.SUCCESS);
    }

    @Test
    void search_shouldOverwriteUserIdWithAuthId_whenUserIsNotAdmin() {
        Long myAuthId = 999L;
        Long requestedUserId = 10L;
        Long orderId = 5L;

        AuthUser authUser = new AuthUser(myAuthId, "ROLE_USER");

        mockSecurityContext(myAuthId, "ROLE_USER");

        Payment payment = new Payment();
        PaymentResponse response = new PaymentResponse();

        when(paymentRepository.search(myAuthId, orderId, Status.SUCCESS))
                .thenReturn(List.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        List<PaymentResponse> result = paymentService.search(requestedUserId, orderId, "SUCCESS", authUser);

        assertEquals(1, result.size());

        verify(paymentRepository).search(myAuthId, orderId, Status.SUCCESS);
    }

    @Test
    void search_shouldThrowIllegalStatusException_whenIncorrectStatusInput() {
        Long myAuthId = 999L;
        Long requestedUserId = 10L;
        Long orderId = 5L;

        AuthUser authUser = new AuthUser(myAuthId, "ROLE_USER");

        mockSecurityContext(myAuthId, "ROLE_USER");

        assertThrows(IllegalStatusException.class,
                () -> paymentService.search(requestedUserId, orderId, "INCORRECT", authUser));
    }

    @Test
    void getTotalPaymentsByUserId_shouldReturnTotal_whenUserIsAdmin() {
        Long myAuthId = 999L;
        Long requestedUserId = 10L;
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-12-31T23:59:59Z");
        BigDecimal expectedTotal = new BigDecimal("1500.50");

        AuthUser authUser = new AuthUser(myAuthId, "ROLE_ADMIN");

        mockSecurityContext(myAuthId, "ROLE_ADMIN");

        when(paymentRepository.getTotalPaymentsByUserId(requestedUserId, from, to))
                .thenReturn(expectedTotal);

        BigDecimal total = paymentService.getTotalPaymentsByUserId(requestedUserId, from, to, authUser);

        assertEquals(expectedTotal, total);
        verify(paymentRepository).getTotalPaymentsByUserId(requestedUserId, from, to);
    }

    @Test
    void getTotalPaymentsByUserId_shouldReturnTotalForOverwriteUser_whenUserIsUser() {
        Long myAuthId = 999L;
        Long requestedUserId = 10L;
        Instant from = Instant.parse("2023-01-01T00:00:00Z");
        Instant to = Instant.parse("2023-12-31T23:59:59Z");
        BigDecimal expectedTotal = new BigDecimal("1500.50");

        AuthUser authUser = new AuthUser(myAuthId, "ROLE_USER");

        mockSecurityContext(myAuthId, "ROLE_USER");

        when(paymentRepository.getTotalPaymentsByUserId(myAuthId, from, to))
                .thenReturn(expectedTotal);

        BigDecimal total = paymentService.getTotalPaymentsByUserId(requestedUserId, from, to, authUser);

        assertEquals(expectedTotal, total);
        verify(paymentRepository).getTotalPaymentsByUserId(myAuthId, from, to);
    }

    @Test
    void getTotalPayments_shouldReturnTotalForAllPayments_whenUserIsAdmin() {
        Instant from = Instant.parse("2023-01-01T00:00:00Z");
        Instant to = Instant.parse("2023-12-31T23:59:59Z");
        BigDecimal expectedTotal = new BigDecimal("1500.50");

        when(paymentRepository.getTotalPayments(from, to)).thenReturn(expectedTotal);

        BigDecimal total = paymentService.getTotalPayments(from, to);

        assertEquals(expectedTotal, total);
        verify(paymentRepository).getTotalPayments(from, to);
    }
}
