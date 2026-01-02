package com.learnit.learnit.payment.card.service;

import com.learnit.learnit.payment.common.PaymentException;
import com.learnit.learnit.payment.common.entity.PaymentPrepare;
import com.learnit.learnit.payment.common.enums.PaymentMethod;
import com.learnit.learnit.payment.common.repository.PaymentPrepareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;


//포트원 API 통신
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCardService {

    private final PaymentPrepareRepository paymentPrepareRepository;

    @Value("${portone.api.key}")
    private String apiKey;

    @Value("${portone.api.secret}")
    private String apiSecret;


    //카드 결제 READY
    @Transactional
    public PaymentPrepare ready(Long userId, int amount, List<Long> courseIds, Long couponId) {
        String orderNo = "C_" + UUID.randomUUID();

        log.info("[CARD READY] userId: {}, orderNo: {}, amount: {}", userId, orderNo, amount);

        PaymentPrepare prepare = PaymentPrepare.ready(
                orderNo,
                userId,
                "TEMP",   // 아직 impUid 없음
                amount,
                courseIds,
                couponId,
                PaymentMethod.CARD
        );

        return paymentPrepareRepository.save(prepare);
    }


    //카드 결제 승인
    @Transactional
    public PaymentPrepare approve(String orderNo, String impUid) {

        log.info("[CARD APPROVE START] orderNo: {}, impUid: {}", orderNo, impUid);

        // 1. 결제 준비 정보 조회
        PaymentPrepare prepare = paymentPrepareRepository
                .findByOrderNoAndPayType(orderNo, PaymentMethod.CARD)
                .orElseThrow(() -> new IllegalStateException("결제 준비 정보가 없습니다. 주문번호: " + orderNo));

        // 2. 포트원 Access Token 발급
        try{
            RestTemplate restTemplate = new RestTemplate();

            // 2. PortOne Access Token 발급 요청
            String accessToken = getPortOneToken(restTemplate);

            // 3. PortOne 결제 정보 단건 조회
            Map<String, Object> paymentData = getPortOnePaymentData(restTemplate, accessToken, impUid);

            // 4. 결제 데이터 검증
            String status = (String) paymentData.get("status");
            Integer paidAmount = (Integer) paymentData.get("amount");

            log.info("[PortOne Response] status: {}, paidAmount: {}, expectedAmount: {}",
                    status, paidAmount, prepare.getAmount());

            // 결제 상태 확인
            if (!"paid".equals(status)) {
                throw new PaymentException("결제가 완료되지 않았습니다. (현재 상태: " + status + ")");
            }

            // 결제 금액 위변조 확인
            if (paidAmount == null || !paidAmount.equals(prepare.getAmount())) {
                log.error("CRITICAL: 결제 금액 불일치 발생! 주문번호: {}, 결제액: {}, 예상액: {}",
                        orderNo, paidAmount, prepare.getAmount());
                throw new PaymentException("결제 금액이 일치하지 않습니다. 위변조가 의심됩니다.");
            }

            // 5. 검증 성공 시 DB 업데이트 (impUid 저장 및 승인 상태로 변경)
            prepare.updatePaymentKey(impUid);
            prepare.success();

            log.info("[CARD APPROVE SUCCESS] orderNo: {}", orderNo);
            return prepare;

        }catch (PaymentException e){
            throw e;
        }catch (Exception e){
            log.error("PortOne API 통신 중 에러 발생: {}", e.getMessage(), e);
            throw new PaymentException("카드 결제 검증 중 서버 오류가 발생했습니다.");
        }

    }

    //포트원 Access Token 발급
    private String getPortOneToken(RestTemplate restTemplate) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
                "imp_key", apiKey,
                "imp_secret", apiSecret
        );

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.iamport.kr/users/getToken", entity, Map.class);

        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        if (responseBody == null || responseBody.get("response") == null) {
            throw new PaymentException("PortOne 인증 토큰을 발급받을 수 없습니다.");
        }

        Map<String, Object> authResponse = (Map<String, Object>) responseBody.get("response");
        return (String) authResponse.get("access_token");
    }

    //포트원 결제 상세 정보 조회
    private Map<String, Object> getPortOnePaymentData(RestTemplate restTemplate, String accessToken, String impUid) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.iamport.kr/payments/" + impUid,
                HttpMethod.GET,
                entity,
                Map.class
        );

        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        if (responseBody == null || responseBody.get("response") == null) {
            throw new PaymentException("결제 상세 정보를 가져올 수 없습니다. impUid: " + impUid);
        }

        return (Map<String, Object>) responseBody.get("response");
    }

}
