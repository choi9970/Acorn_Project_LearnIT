package com.learnit.learnit.mypage.controller;

import com.learnit.learnit.mypage.dto.CertificateDTO;
import com.learnit.learnit.mypage.service.CertificateService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    /**
     * 수료증 PDF 다운로드
     */
    @GetMapping("/mypage/certificates/{certificateId}/download")
    public ResponseEntity<byte[]> downloadCertificate(
            @PathVariable Long certificateId,
            HttpSession session) {
        
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        
        if (userId == null) {
            log.warn("Certificate download attempted without login: certificateId={}", certificateId);
            return ResponseEntity.status(401).build();
        }

        try {
            log.info("Certificate download requested: certificateId={}, userId={}", certificateId, userId);
            
            // 수료증 조회
            CertificateDTO certificate = certificateService.getCertificate(certificateId, userId);
            
            if (certificate == null) {
                log.warn("Certificate not found: certificateId={}, userId={}", certificateId, userId);
                return ResponseEntity.notFound().build();
            }

            log.debug("Certificate found: courseTitle={}, userName={}, issuedDate={}", 
                certificate.getCourseTitle(), certificate.getUserName(), certificate.getIssuedDate());

            // PDF 생성
            byte[] pdfBytes = certificateService.generateCertificatePdf(certificate);
            
            if (pdfBytes == null || pdfBytes.length == 0) {
                log.error("PDF generation failed: empty bytes for certificateId={}", certificateId);
                return ResponseEntity.internalServerError().build();
            }

            log.info("PDF generated successfully: size={} bytes, certificateId={}", pdfBytes.length, certificateId);

            // 파일명 생성
            String fileName = "Certificate_" + 
                (certificate.getCourseTitle() != null ? certificate.getCourseTitle() : "Course") + "_" +
                certificateId + ".pdf";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

            // 응답 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", encodedFileName);
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);

        } catch (IllegalArgumentException e) {
            log.error("Invalid argument for certificate download: certificateId={}, userId={}, error={}", 
                certificateId, userId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("IO error during PDF generation: certificateId={}, userId={}, error={}", 
                certificateId, userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Unexpected error during certificate download: certificateId={}, userId={}, error={}", 
                certificateId, userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

