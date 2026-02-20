package com.example.PaymentService.entity;

import com.example.PaymentService.enums.Status;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "payments")
public class Payment {

    @Id
    private String id;

    @Field("order_id")
    @NotNull
    private Long orderId;

    @Field("user_id")
    @NotNull
    private Long userId;

    @NotNull
    private Status status;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Field("payment_amount")
    @NotNull
    @PositiveOrZero
    private BigDecimal paymentAmount;
}
