package com.example.PaymentService.service.outbox;

import com.example.PaymentService.kafka.dto.OutboxRequest;
import com.example.PaymentService.kafka.entity.Outbox;
import com.example.PaymentService.kafka.enums.EventStatus;
import com.example.PaymentService.kafka.repository.OutboxRepository;
import com.example.PaymentService.kafka.service.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
public class OutboxServiceIntegrationTest {

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private OutboxService outboxService;

    @Container
    private static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @Container
    static WireMockContainer wiremock =
            new WireMockContainer("wiremock/wiremock:3.13.1")
                    .withMapping("randomNumber", """
                            {
                              "request": {
                                "method": "GET",
                                "url": "/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new"
                              },
                              "response": {
                                "status": 200,
                                "body": "10",
                                "headers": {
                                  "Content-Type": "text/plain"
                                }
                              }
                            }
                            """);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("external.payment-service.url", wiremock::getBaseUrl);
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String randomBase64Secret = Base64.getEncoder().encodeToString(randomBytes);
        registry.add("JWT_SECRET", () -> randomBase64Secret);
    }

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Test
    void create_shouldCreateEvent() {
        OutboxRequest request = new OutboxRequest();
        request.setEventType("CREATE_PAYMENT");
        request.setStatus("NEW");
        request.setPayload(Map.of(
                "paymentAmount", 10,
                "userId", 1L,
                "orderId", 5L,
                "paymentStatus", "SUCCESS"
        ));

        outboxService.create(request);

        List<Outbox> events = outboxRepository.findAll();

        assertEquals(1, events.size());

        Outbox savedOutbox = events.get(0);

        assertNotNull(savedOutbox.getId());

        assertEquals("CREATE_PAYMENT", savedOutbox.getEventType());
        assertEquals(EventStatus.NEW, savedOutbox.getStatus());
        assertEquals(10, savedOutbox.getPayload().get("paymentAmount"));
    }

    @Test
    void getFailedEvents_shouldReturnListOfEvents() {
        Outbox newEvent = new Outbox();
        newEvent.setStatus(EventStatus.NEW);
        newEvent.setEventType("SUCCESS");
        newEvent.setPayload(Map.of(
                "paymentAmount", 10,
                "userId", 1L,
                "orderId", 5L,
                "paymentStatus", "SUCCESS"
        ));
        outboxRepository.save(newEvent);

        Outbox failedEvent = new Outbox();
        failedEvent.setStatus(EventStatus.FAILED);
        failedEvent.setEventType("FAILED");
        failedEvent.setPayload(Map.of(
                "paymentAmount", 10,
                "userId", 1L,
                "orderId", 5L,
                "paymentStatus", "SUCCESS"
        ));
        outboxRepository.save(failedEvent);

        List<Outbox> result = outboxService.getFailedEvents();

        assertEquals(1, result.size());
        assertEquals(EventStatus.FAILED, result.get(0).getStatus());
        assertEquals("FAILED", result.get(0).getEventType());
    }

    @Test
    void getNewEvents_shouldReturnListOfEvents() {
        Outbox newEvent = new Outbox();
        newEvent.setStatus(EventStatus.NEW);
        newEvent.setEventType("SUCCESS");
        newEvent.setPayload(Map.of(
                "paymentAmount", 10,
                "userId", 1L,
                "orderId", 5L,
                "paymentStatus", "SUCCESS"
        ));
        outboxRepository.save(newEvent);

        Outbox failedEvent = new Outbox();
        failedEvent.setStatus(EventStatus.FAILED);
        failedEvent.setEventType("FAILED");
        failedEvent.setPayload(Map.of(
                "paymentAmount", 10,
                "userId", 1L,
                "orderId", 5L,
                "paymentStatus", "FAILED"
        ));
        outboxRepository.save(failedEvent);

        List<Outbox> result = outboxService.getFailedEvents();

        assertEquals(1, result.size());
        assertEquals(EventStatus.FAILED, result.get(0).getStatus());
        assertEquals("FAILED", result.get(0).getEventType());
    }

    @Test
    void markAsSent_shouldOverwriteStatusToCanceled() {
        Outbox newEvent = new Outbox();
        newEvent.setStatus(EventStatus.NEW);
        newEvent.setEventType("SUCCESS");
        newEvent.setPayload(Map.of(
                "paymentAmount", 10,
                "userId", 1L,
                "orderId", 5L,
                "paymentStatus", "SUCCESS"
        ));
        Outbox saved = outboxRepository.save(newEvent);

        outboxService.markAsSent(saved.getId());

        Outbox savedAfterSetSend = outboxRepository.findById(saved.getId()).get();

        assertEquals(EventStatus.CANCELLED, savedAfterSetSend.getStatus());
    }

    @Test
    void markAsSent_shouldThrowException_whenIdNotFound() {
        assertThrows(NoSuchElementException.class, () -> outboxService.markAsSent("id"));
    }

    @Test
    void markAsFailed_shouldOverwriteStatusToFailed() {
        Outbox newEvent = new Outbox();
        newEvent.setStatus(EventStatus.NEW);
        newEvent.setEventType("FAILED");
        newEvent.setPayload(Map.of(
                "paymentAmount", 10,
                "userId", 1L,
                "orderId", 5L,
                "paymentStatus", "FAILED"
        ));
        Outbox saved = outboxRepository.save(newEvent);

        outboxService.markAsFailed(saved.getId());

        Outbox savedAfterSetSend = outboxRepository.findById(saved.getId()).get();

        assertEquals(EventStatus.FAILED, savedAfterSetSend.getStatus());
    }

    @Test
    void markAsFailed_shouldThrowException_whenIdNotFound() {
        assertThrows(NoSuchElementException.class, () -> outboxService.markAsFailed("id"));
    }
}
