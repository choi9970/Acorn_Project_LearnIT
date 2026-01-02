package com.learnit.learnit.mypage.mapper;

import com.learnit.learnit.mypage.dto.CertificateDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CertificateMapper {
    /**
     * 수료증 상세 조회
     */
    CertificateDTO selectCertificateById(@Param("certificateId") Long certificateId, @Param("userId") Long userId);
}

