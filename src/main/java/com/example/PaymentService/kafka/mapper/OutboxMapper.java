package com.example.PaymentService.kafka.mapper;

import com.example.PaymentService.kafka.dto.OutboxRequest;
import com.example.PaymentService.kafka.entity.Outbox;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OutboxMapper {

    Outbox toEntity(OutboxRequest request);
}
