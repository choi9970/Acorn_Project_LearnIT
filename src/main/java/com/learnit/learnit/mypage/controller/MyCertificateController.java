package com.learnit.learnit.mypage.controller;

import com.learnit.learnit.mypage.dto.MyCertificateDTO;
import com.learnit.learnit.mypage.service.MyCertificateService;
import com.learnit.learnit.mypage.service.S3Service;
import com.learnit.learnit.user.util.SessionUtils;
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
public class MyCertificateController {

    private final MyCertificateService certificateService;
    private final S3Service s3Service;

    /**
     * 수료증 PDF 다운로드 (무조건 다운로드 보장)
     *
     * <p>흐름</p>
     * <ol>
     *   <li>로그인 사용자 확인</li>
     *   <li>certificateId + userId로 수료증 권한/승인(is_approved='Y') 검증</li>
     *   <li>S3(pdf)에서 먼저 다운로드 시도 (Private이어도 서버 IAM 권한이면 OK)</li>
     *   <li>S3에 없으면 PDF를 즉시 생성해서 attachment로 반환 (업로드는 best-effort)</li>
     * </ol>
     */
    @GetMapping("/mypage/certificates/{certificateId}/download")
    public ResponseEntity<byte[]> downloadCertificate(
            @PathVariable Long certificateId,
            HttpSession session) {

        Long userId = SessionUtils.getUserId(session);
        if (userId == null) {
            log.warn("Certificate download attempted without login: certificateId={}", certificateId);
            return ResponseEntity.status(401).build();
        }

        try {
            log.info("Certificate download requested: certificateId={}, userId={}", certificateId, userId);

            // 1) 권한/승인 검증 포함 조회
            MyCertificateDTO certificate = certificateService.getCertificate(certificateId, userId);
            if (certificate == null) {
                log.warn("Certificate not found: certificateId={}, userId={}", certificateId, userId);
                return ResponseEntity.notFound().build();
            }

            // 2) S3 Key
            String s3FileName = "Certificate_" + certificate.getCourseId() + "_" + userId + ".pdf";
            String s3Key = "certificates/pdf/" + s3FileName;

            byte[] pdfBytes;

            // 3) S3에서 먼저 읽기 (GetObject 권한 필요)
            try {
                pdfBytes = s3Service.downloadAsBytes(s3Key);
            } catch (Exception s3ReadFail) {
                // 4) S3에서 못 읽으면 즉시 생성하여 반환 (다운로드는 항상 되게)
                log.warn("S3 read failed. Will generate PDF on-the-fly. key={}, reason={}", s3Key, s3ReadFail.getMessage());
                pdfBytes = certificateService.generateCertificatePdf(certificate);

                // 업로드는 best-effort (PutObject 권한이 없으면 실패해도 무시)
                try {
                    certificateService.uploadCertificateToS3(certificateId, userId);
                } catch (Exception ignore) {
                    log.warn("S3 upload skipped/failed (best-effort). certificateId={}, userId={}, reason={}",
                            certificateId, userId, ignore.getMessage());
                }
            }

            if (pdfBytes == null || pdfBytes.length == 0) {
                log.error("PDF bytes empty: certificateId={}, userId={}", certificateId, userId);
                return ResponseEntity.internalServerError().build();
            }

            // 5) 파일명은 요구사항대로 (CourseId_UserId)
            String downloadFileName = "Certificate_" + certificate.getCourseId() + "_" + userId + ".pdf";
            String encodedFileName = URLEncoder.encode(downloadFileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName);
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

    /**
     * 수료증 썸네일(PNG) 제공 (서버 경유)
     *
     * <p>S3 thumb 객체가 없으면 404 반환 → 프론트에서 onerror 처리로 아이콘 숨김/대체</p>
     */
    @GetMapping("/mypage/certificates/{certificateId}/thumb")
    public ResponseEntity<byte[]> certificateThumb(
            @PathVariable Long certificateId,
            HttpSession session) {

        Long userId = SessionUtils.getUserId(session);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            MyCertificateDTO certificate = certificateService.getCertificate(certificateId, userId);
            if (certificate == null) {
                return ResponseEntity.notFound().build();
            }

            String s3FileName = "Certificate_" + certificate.getCourseId() + "_" + userId + ".png";
            String s3Key = "certificates/thumb/" + s3FileName;

            byte[] pngBytes = s3Service.downloadAsBytes(s3Key);
            if (pngBytes == null || pngBytes.length == 0) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(pngBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pngBytes);

        } catch (Exception e) {
            // thumb가 없는 케이스가 흔하므로 404로 정리
            return ResponseEntity.notFound().build();
        }
    }
}
