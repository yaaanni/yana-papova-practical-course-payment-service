package com.example.PaymentService.kafka.entity;

import com.example.PaymentService.kafka.enums.EventStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "outbox")
public class Outbox {

    @Id
    private String id;

    @NotBlank
    @Field("event_type")
    private String eventType;

    @NotNull
    private Map<String, Object> payload;

    @NotBlank
    private EventStatus status;

    @CreatedDate
    private Instant createdAt;
}
