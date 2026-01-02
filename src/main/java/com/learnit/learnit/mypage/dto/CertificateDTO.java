package com.learnit.learnit.mypage.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CertificateDTO {
    private Long certificateId;
    private Long courseId;
    private String courseTitle;
    private LocalDate issuedDate;
    private String certificateUrl;
    private String userName;
    private String certificateNumber;
}

