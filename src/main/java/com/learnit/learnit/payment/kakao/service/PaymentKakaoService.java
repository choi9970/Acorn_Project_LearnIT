package com.learnit.learnit.payment.kakao.service;

import com.learnit.learnit.payment.common.PaymentException;
import com.learnit.learnit.payment.common.dto.PaymentRequestDTO;
import com.learnit.learnit.payment.common.entity.PaymentPrepare;
import com.learnit.learnit.payment.common.enums.PaymentMethod;
import com.learnit.learnit.payment.common.repository.PaymentPrepareRepository;
import com.learnit.learnit.payment.common.service.PaymentService;
import com.learnit.learnit.payment.kakao.dto.KakaoApproveResponse;
import com.learnit.learnit.payment.kakao.dto.KakaoReadyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

//카카오 API 통신

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentKakaoService {

    @Value("${kakao.pay.admin-key}")
    private String adminKey;

    @Value("${kakao.pay.cid}")
    private String cid;

    @Value("${app.base-url:https://learnit24.com}")
    private String baseUrl;

    private final PaymentPrepareRepository paymentPrepareRepository;
    private final PaymentService paymentService;


    //결제 준비 ready API
    @Transactional
    public KakaoReadyResponse ready(Long userId, int amount, List<Long> courseIds, Long couponId){

        //1. 주문번호 생성 (UUID)
        String orderNo = "K_" + UUID.randomUUID();

        log.info("[KAKAO READY START] userId: {}, orderNo: {}, amount: {}", userId, orderNo, amount);

        //2. 카카오 API 요청 헤더
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "KakaoAK " + adminKey);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        //3. 카카오 ready 파라미터
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", cid);
        params.add("partner_order_id", orderNo);
        params.add("partner_user_id", String.valueOf(userId));
        params.add("item_name", "learnIT 강의 결제");
        params.add("quantity", "1");
        params.add("total_amount", String.valueOf(amount));
        params.add("tax_free_amount", "0");

        params.add("approval_url", baseUrl + "/payments/kakao/success?orderNo=" + orderNo);
        params.add("cancel_url", baseUrl + "/payments/kakao/cancel?orderNo=" + orderNo);
        params.add("fail_url", baseUrl + "/payments/kakao/fail?orderNo=" + orderNo);

        //4. 요청 객체 생성
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);


        //5. 카카오 ready API 호출
        try{
            RestTemplate restTemplate = new RestTemplate();
            KakaoReadyResponse response = restTemplate.postForObject(
                    "https://kapi.kakao.com/v1/payment/ready",
                    request,
                    KakaoReadyResponse.class
            );

            if (response == null || response.getTid() == null) {
                throw new PaymentException("카카오페이로부터 응답(TID)을 받지 못했습니다.");
            }

            //6. 결제 준비 정보 DB 저장 (카카오 tid 저장)
            PaymentPrepare prepare = PaymentPrepare.ready(orderNo, userId, response.getTid(), amount, courseIds, couponId, PaymentMethod.KAKAOPAY);
            paymentPrepareRepository.save(prepare);

            log.info("[KAKAO READY SUCCESS] orderNo: {}, tid: {}", orderNo, response.getTid());
            response.setPartnerOrderId(orderNo);
            return response;

        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("카카오페이 Ready API 호출 중 예외 발생: {}", e.getMessage(), e);
            throw new PaymentException("카카오페이 결제 준비 중 오류가 발생했습니다.");
        }

    }

    //결제 승인 approve API
    @Transactional
    public void approve(String pgToken, String orderNo){

        log.info("[KAKAO APPROVE START] orderNo: {}, pgToken: {}", orderNo, pgToken);

        //1. 결제 준비 정보 조회
        PaymentPrepare prepare = paymentPrepareRepository
                .findByOrderNo(orderNo)
                .orElseThrow(() -> new PaymentException("결제 준비 정보를 찾을 수 없습니다."));


        // 2. 카카오 API 요청 헤더
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "KakaoAK " + adminKey);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // 3. 카카오 approve 파라미터
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", cid);
        params.add("tid", prepare.getPaymentKey());   // tid
        params.add("partner_order_id", orderNo);
        params.add("partner_user_id", String.valueOf(prepare.getUserId()));
        params.add("pg_token", pgToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);


        // 4. 카카오 approve API 호출
        try{
            RestTemplate restTemplate = new RestTemplate();
            KakaoApproveResponse response = restTemplate.postForObject(
                    "https://kapi.kakao.com/v1/payment/approve",
                    request,
                    KakaoApproveResponse.class
            );

            //5. 결제 준비 상태 변경
            prepare.success();

            //6. 공통 결제 DTO로 변환
            paymentService.processPayment(PaymentRequestDTO.fromPrepare(prepare));

            log.info("[KAKAO APPROVE SUCCESS] orderNo: {}", orderNo);

        }catch (PaymentException e){
            throw e;
        } catch (Exception e) {
            log.error("카카오페이 Approve API 호출 중 예외 발생: {}", e.getMessage(), e);
            throw new PaymentException("카카오 결제 승인 처리 중 오류가 발생했습니다.");
        }

    }

    //결제 취소 API
    @Transactional
    public void cancel(String orderNo) {
        log.info("[KAKAO CANCEL] orderNo: {}", orderNo);
        PaymentPrepare prepare = paymentPrepareRepository
                .findByOrderNo(orderNo)
                .orElseThrow(() -> new PaymentException("결제 준비 정보가 없습니다."));

        prepare.cancel(); // 상태만 변경
    }

    //결제 실패 API
    @Transactional
    public void fail(String orderNo) {
        log.error("[KAKAO FAIL] orderNo: {}", orderNo);
        PaymentPrepare prepare = paymentPrepareRepository
                .findByOrderNo(orderNo)
                .orElseThrow(() -> new PaymentException("결제 준비 정보가 없습니다."));

        prepare.fail(); // 상태만 변경
    }
}
