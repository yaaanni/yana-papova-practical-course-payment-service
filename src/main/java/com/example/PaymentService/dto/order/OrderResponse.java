package com.example.PaymentService.dto.order;

import com.example.PaymentService.dto.orderItemResponse.OrderItemResponse;
import com.example.PaymentService.dto.user.UserResponse;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
public class OrderResponse {

    private Long id;
    private String status;
    private BigDecimal totalPrice;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemResponse> items;
    private UserResponse user;
}
