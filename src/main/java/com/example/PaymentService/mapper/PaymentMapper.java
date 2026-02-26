package com.example.PaymentService.mapper;

import com.example.PaymentService.dto.PaymentRequest;
import com.example.PaymentService.dto.PaymentResponse;
import com.example.PaymentService.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    Payment toEntity(PaymentRequest request);

    PaymentResponse toResponse(Payment payment);
}
