package com.learnit.learnit.payment.common;

//결제 전용 예외 클래스
public class PaymentException extends RuntimeException{

    public PaymentException(String message) {
        super(message);
    }
}
