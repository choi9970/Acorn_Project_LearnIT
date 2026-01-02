package com.learnit.learnit.payment.kakao.dto;

import lombok.Data;

@Data
public class KakaoReadyResponse {

    private String tid;     //결제 고유 id
    private String next_redirect_pc_url;
    private String created_at;

    private String partnerOrderId;
}
