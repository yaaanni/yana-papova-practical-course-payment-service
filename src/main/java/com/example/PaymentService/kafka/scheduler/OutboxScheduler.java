package com.example.PaymentService.kafka.scheduler;

import com.example.PaymentService.kafka.entity.Outbox;
import com.example.PaymentService.kafka.producer.PaymentProducer;
import com.example.PaymentService.kafka.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class OutboxScheduler {

    private final OutboxService outboxService;
    private final PaymentProducer paymentProducer;

    @Scheduled(fixedDelay = 10_000)
    public void run() {
        processNewEvents();
        processFailedEvents();
    }

    private void processNewEvents() {
        var events = outboxService.getNewEvents();
        process(events);
    }

    private void processFailedEvents() {
        var events = outboxService.getFailedEvents();
        process(events);
    }

    private void process(List<Outbox> events) {
        for (Outbox event : events) {
            try {
                paymentProducer.send(event.getEventType(), event.getId(), event.getPayload());
                outboxService.markAsSent(event.getId());
            } catch (Exception e) {
                outboxService.markAsFailed(event.getId());
            }
        }
    }
}
