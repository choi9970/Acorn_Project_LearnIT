package com.learnit.learnit.mypage.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket:learnit-certificates-prod}")
    private String bucket;

    public void uploadFile(String path, String fileName, byte[] fileData) {
        String fullPath = path + fileName;
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(fileData.length);
        metadata.setContentType("application/pdf");

        try (InputStream inputStream = new ByteArrayInputStream(fileData)) {
            amazonS3.putObject(new PutObjectRequest(bucket, fullPath, inputStream, metadata));
            log.info("S3 Upload success: {}", fullPath);
        } catch (Exception e) {
            log.error("S3 Upload failed: {}", e.getMessage());
        }
    }

    /**
     * S3 객체를 바이트 배열로 다운로드합니다.
     *
     * <p>버킷/오브젝트가 Private이어도 서버(IAM User/Role)에 s3:GetObject 권한이 있으면 정상 동작합니다.</p>
     */
    public byte[] downloadAsBytes(String key) {
        try (S3Object s3Object = amazonS3.getObject(bucket, key);
             InputStream is = s3Object.getObjectContent()) {
            byte[] data = is.readAllBytes();
            log.info("S3 Download success: key={}, size={} bytes", key, data.length);
            return data;
        } catch (Exception e) {
            log.error("S3 Download failed: key={}, error={}", key, e.getMessage(), e);
            throw new RuntimeException("S3 다운로드 실패: " + key, e);
        }
    }
}
