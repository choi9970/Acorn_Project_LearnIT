package com.learnit.learnit.payment;

import com.learnit.learnit.payment.common.LoginRequiredException;
import com.learnit.learnit.payment.common.PaymentException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@ControllerAdvice
public class GlobalPaymentExceptionHandler {

    //로그인이 필요한 경우
    @ExceptionHandler(LoginRequiredException.class)
    public Object handleLoginRequired(LoginRequiredException ex,
                                      HttpServletRequest request,
                                      RedirectAttributes rttr){

        String accept = request.getHeader("Accept");

        //(fetch) 요청인 경우
        if(accept != null && accept.contains("application/json")){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", ex.getMessage()));
        }

        rttr.addFlashAttribute("loginMessage", ex.getMessage());
        return "redirect:/login";
    }

    // 일반 결제 오류
    @ExceptionHandler(PaymentException.class)
    public Object handlePaymentError(PaymentException ex,
                                     HttpServletRequest request,
                                     RedirectAttributes rttr){

        String accept = request.getHeader("Accept");

        //js(fetch) 요청인 경우
        if(accept != null && accept.contains("application/json")){
            return ResponseEntity.badRequest()
                    .body(Map.of("message", ex.getMessage()));
        }

        rttr.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/payment/fail";
    }
}
