package com.example.PaymentService.dto.user;

import com.example.PaymentService.dto.card.CardResponse;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
public class UserResponse implements Serializable {
    private Long id;
    private String name;
    private String surname;
    private LocalDate birthDay;
    private String email;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CardResponse> cards;
}
