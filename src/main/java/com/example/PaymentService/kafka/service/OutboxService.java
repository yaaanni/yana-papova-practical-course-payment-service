package com.example.PaymentService.kafka.service;

import com.example.PaymentService.kafka.dto.OutboxRequest;
import com.example.PaymentService.kafka.entity.Outbox;
import com.example.PaymentService.kafka.enums.EventStatus;
import com.example.PaymentService.kafka.mapper.OutboxMapper;
import com.example.PaymentService.kafka.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final OutboxMapper outboxMapper;

    public void create(OutboxRequest request) {
        Outbox outbox = outboxMapper.toEntity(request);

        outboxRepository.save(outbox);
    }

    public List<Outbox> getNewEvents() {
        return outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(EventStatus.NEW.name());
    }

    public List<Outbox> getFailedEvents() {
        return outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(EventStatus.FAILED.name());
    }

    public void markAsSent(String id) {
        Outbox outbox = outboxRepository.findById(id).orElseThrow();
        outbox.setStatus(EventStatus.CANCELLED);
        outboxRepository.save(outbox);
    }

    public void markAsFailed(String id) {
        Outbox outbox = outboxRepository.findById(id).orElseThrow();
        outbox.setStatus(EventStatus.FAILED);
        outboxRepository.save(outbox);
    }
}
