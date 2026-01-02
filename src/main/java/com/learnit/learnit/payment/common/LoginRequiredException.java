package com.learnit.learnit.payment.common;

public class LoginRequiredException extends PaymentException {

    public LoginRequiredException(String message) {
        super(message);
    }
}
