package com.example.PaymentService.kafka.repository;

import com.example.PaymentService.kafka.entity.Outbox;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OutboxRepository extends MongoRepository<Outbox, String> {
    List<Outbox> findTop100ByStatusOrderByCreatedAtAsc(String status);
}
