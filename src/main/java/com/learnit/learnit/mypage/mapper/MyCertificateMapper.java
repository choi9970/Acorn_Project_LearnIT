package com.learnit.learnit.mypage.mapper;

import com.learnit.learnit.mypage.dto.MyCertificateDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MyCertificateMapper {
    /**
     * 수료증 상세 조회
     */
    MyCertificateDTO selectCertificateById(@Param("certificateId") Long certificateId, @Param("userId") Long userId);
    
    /**
     * 수료증 생성 (완강 시 자동 발급)
     */
    void insertCertificate(@Param("enrollmentId") Long enrollmentId, 
                          @Param("certificateNumber") String certificateNumber);

    /**
     * 수강 ID로 수료증 조회
     */
    MyCertificateDTO selectCertificateByEnrollmentId(@Param("enrollmentId") Long enrollmentId);
}
