package com.example.PaymentService.exception;

public class IllegalStatusException extends RuntimeException {
    public IllegalStatusException() {
        super("Invalid request status");
    }
}
