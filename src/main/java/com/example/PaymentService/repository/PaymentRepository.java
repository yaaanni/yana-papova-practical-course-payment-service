package com.example.PaymentService.repository;

import com.example.PaymentService.entity.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentRepository extends MongoRepository<Payment, String>, PaymentCustomRepository{


}
