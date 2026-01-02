package com.learnit.learnit.payment.kakao.dto;

import lombok.Data;

@Data
public class KakaoApproveResponse {

    private String tid;
    private String cid;
    private String partner_order_id;
    private String partner_user_id;
    private Amount amount;

    @Data
    public static class Amount {
        private int total;     //실제 결제 금액
    }
}
