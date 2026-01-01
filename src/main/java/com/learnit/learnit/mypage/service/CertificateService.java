package com.learnit.learnit.mypage.service;

import com.learnit.learnit.mypage.dto.CertificateDTO;
import com.learnit.learnit.mypage.mapper.CertificateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateMapper certificateMapper;

    /**
     * 수료증 조회
     */
    public CertificateDTO getCertificate(Long certificateId, Long userId) {
        if (certificateId == null || userId == null) {
            throw new IllegalArgumentException("수료증 ID 또는 사용자 ID가 없습니다.");
        }
        return certificateMapper.selectCertificateById(certificateId, userId);
    }

    /**
     * 수료증 PDF 생성
     */
    public byte[] generateCertificatePdf(CertificateDTO certificate) throws IOException {
        if (certificate == null) {
            throw new IllegalArgumentException("수료증 정보가 없습니다.");
        }

        log.info("Generating PDF certificate: certificateId={}, courseTitle={}, userName={}", 
            certificate.getCertificateId(), certificate.getCourseTitle(), certificate.getUserName());

        try (PDDocument document = new PDDocument()) {
            // A4 크기 페이지 생성
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            // 한글 폰트 로드 (resources/fonts/ 폴더에서)
            PDType0Font koreanFontBold = null;
            PDType0Font koreanFontRegular = null;
            try {
                InputStream fontStream = getClass().getClassLoader()
                    .getResourceAsStream("fonts/NanumGothic.ttf");
                if (fontStream != null) {
                    koreanFontRegular = PDType0Font.load(document, fontStream);
                    // 볼드 폰트가 따로 없으므로 Regular 폰트를 볼드로도 사용
                    koreanFontBold = koreanFontRegular;
                    fontStream.close();
                    log.info("Korean font loaded successfully");
                } else {
                    log.warn("Korean font file not found at fonts/NanumGothic.ttf, using default fonts");
                }
            } catch (Exception e) {
                log.warn("Failed to load Korean font: {}, using default fonts", e.getMessage());
            }

            // 한글 폰트가 없을 경우 기본 폰트 사용
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                PDRectangle pageSize = page.getMediaBox();
                float pageWidth = pageSize.getWidth();
                float pageHeight = pageSize.getHeight();

                // 배경색 (선택사항)
                contentStream.setNonStrokingColor(0.95f, 0.95f, 0.95f);
                contentStream.addRect(0, 0, pageWidth, pageHeight);
                contentStream.fill();

                // 제목
                contentStream.setNonStrokingColor(0.04f, 0.29f, 0.48f); // #0A4A7A
                contentStream.beginText();
                if (koreanFontBold != null) {
                    contentStream.setFont(koreanFontBold, 36);
                    String title = "수료증";
                    float titleWidth = koreanFontBold.getStringWidth(title) / 1000 * 36;
                    contentStream.newLineAtOffset((pageWidth - titleWidth) / 2, pageHeight - 150);
                    contentStream.showText(title);
                } else {
                    contentStream.setFont(fontBold, 36);
                    String title = "Certificate";
                    float titleWidth = fontBold.getStringWidth(title) / 1000 * 36;
                    contentStream.newLineAtOffset((pageWidth - titleWidth) / 2, pageHeight - 150);
                    contentStream.showText(title);
                }
                contentStream.endText();

                // 본문
                contentStream.setNonStrokingColor(0, 0, 0);
                contentStream.beginText();
                if (koreanFontRegular != null) {
                    contentStream.setFont(koreanFontRegular, 16);
                    String text1 = "본 수료증은 다음 학습자가";
                    float text1Width = koreanFontRegular.getStringWidth(text1) / 1000 * 16;
                    contentStream.newLineAtOffset((pageWidth - text1Width) / 2, pageHeight - 250);
                    contentStream.showText(text1);
                } else {
                    contentStream.setFont(fontRegular, 16);
                    String text1 = "This certificate is awarded to";
                    float text1Width = fontRegular.getStringWidth(text1) / 1000 * 16;
                    contentStream.newLineAtOffset((pageWidth - text1Width) / 2, pageHeight - 250);
                    contentStream.showText(text1);
                }
                contentStream.endText();

                // 사용자 이름
                contentStream.beginText();
                if (koreanFontBold != null) {
                    contentStream.setFont(koreanFontBold, 24);
                    String userName = certificate.getUserName() != null ? certificate.getUserName() : "학습자";
                    float userNameWidth = koreanFontBold.getStringWidth(userName) / 1000 * 24;
                    contentStream.newLineAtOffset((pageWidth - userNameWidth) / 2, pageHeight - 300);
                    contentStream.showText(userName);
                } else {
                    contentStream.setFont(fontBold, 24);
                    String userName = certificate.getUserName() != null ? certificate.getUserName() : "Student";
                    // 한글이 포함된 경우 영문으로 대체
                    if (!userName.matches("^[a-zA-Z0-9\\s]+$")) {
                        userName = "Student";
                    }
                    float userNameWidth = fontBold.getStringWidth(userName) / 1000 * 24;
                    contentStream.newLineAtOffset((pageWidth - userNameWidth) / 2, pageHeight - 300);
                    contentStream.showText(userName);
                }
                contentStream.endText();

                // 강의명
                contentStream.beginText();
                if (koreanFontRegular != null) {
                    contentStream.setFont(koreanFontRegular, 16);
                    String text2 = "다음 강의를 성공적으로 수료하였음을 증명합니다.";
                    float text2Width = koreanFontRegular.getStringWidth(text2) / 1000 * 16;
                    contentStream.newLineAtOffset((pageWidth - text2Width) / 2, pageHeight - 350);
                    contentStream.showText(text2);
                } else {
                    contentStream.setFont(fontRegular, 16);
                    String text2 = "for successfully completing the course";
                    float text2Width = fontRegular.getStringWidth(text2) / 1000 * 16;
                    contentStream.newLineAtOffset((pageWidth - text2Width) / 2, pageHeight - 350);
                    contentStream.showText(text2);
                }
                contentStream.endText();

                contentStream.beginText();
                if (koreanFontBold != null) {
                    contentStream.setFont(koreanFontBold, 20);
                    String courseTitle = certificate.getCourseTitle() != null ? certificate.getCourseTitle() : "강의명";
                    float courseTitleWidth = koreanFontBold.getStringWidth(courseTitle) / 1000 * 20;
                    contentStream.newLineAtOffset((pageWidth - courseTitleWidth) / 2, pageHeight - 400);
                    contentStream.showText(courseTitle);
                } else {
                    contentStream.setFont(fontBold, 20);
                    String courseTitle = certificate.getCourseTitle() != null ? certificate.getCourseTitle() : "Course";
                    // 한글이 포함된 경우 영문으로 대체
                    if (!courseTitle.matches("^[a-zA-Z0-9\\s]+$")) {
                        courseTitle = "Course Title";
                    }
                    float courseTitleWidth = fontBold.getStringWidth(courseTitle) / 1000 * 20;
                    contentStream.newLineAtOffset((pageWidth - courseTitleWidth) / 2, pageHeight - 400);
                    contentStream.showText(courseTitle);
                }
                contentStream.endText();

                // 발급일
                contentStream.beginText();
                if (koreanFontRegular != null) {
                    contentStream.setFont(koreanFontRegular, 14);
                    String issuedDate = certificate.getIssuedDate() != null 
                        ? certificate.getIssuedDate().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))
                        : "";
                    String dateText = "발급일: " + issuedDate;
                    float dateTextWidth = koreanFontRegular.getStringWidth(dateText) / 1000 * 14;
                    contentStream.newLineAtOffset((pageWidth - dateTextWidth) / 2, pageHeight - 500);
                    contentStream.showText(dateText);
                } else {
                    contentStream.setFont(fontRegular, 14);
                    String issuedDate = certificate.getIssuedDate() != null 
                        ? certificate.getIssuedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        : "";
                    String dateText = "Issued Date: " + issuedDate;
                    float dateTextWidth = fontRegular.getStringWidth(dateText) / 1000 * 14;
                    contentStream.newLineAtOffset((pageWidth - dateTextWidth) / 2, pageHeight - 500);
                    contentStream.showText(dateText);
                }
                contentStream.endText();

                // 수료증 번호
                if (certificate.getCertificateNumber() != null) {
                    contentStream.beginText();
                    if (koreanFontRegular != null) {
                        contentStream.setFont(koreanFontRegular, 12);
                        String certNumberText = "수료증 번호: " + certificate.getCertificateNumber();
                        float certNumberWidth = koreanFontRegular.getStringWidth(certNumberText) / 1000 * 12;
                        contentStream.newLineAtOffset((pageWidth - certNumberWidth) / 2, pageHeight - 550);
                        contentStream.showText(certNumberText);
                    } else {
                        contentStream.setFont(fontRegular, 12);
                        String certNumberText = "Certificate No: " + certificate.getCertificateNumber();
                        float certNumberWidth = fontRegular.getStringWidth(certNumberText) / 1000 * 12;
                        contentStream.newLineAtOffset((pageWidth - certNumberWidth) / 2, pageHeight - 550);
                        contentStream.showText(certNumberText);
                    }
                    contentStream.endText();
                }

                // 하단 서명란
                contentStream.beginText();
                contentStream.setFont(fontRegular, 14);
                String signatureText = "LearnIT";
                float signatureWidth = fontRegular.getStringWidth(signatureText) / 1000 * 14;
                contentStream.newLineAtOffset((pageWidth - signatureWidth) / 2, 100);
                contentStream.showText(signatureText);
                contentStream.endText();
            }

            // PDF를 바이트 배열로 변환
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            byte[] pdfBytes = outputStream.toByteArray();
            
            log.info("PDF certificate generated successfully: size={} bytes", pdfBytes.length);
            return pdfBytes;
        } catch (Exception e) {
            log.error("Error generating PDF certificate: {}", e.getMessage(), e);
            throw new IOException("PDF 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}

