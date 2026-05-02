package com.omnisource.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.omnisource.service.OssUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
public class OssUploadServiceImpl implements OssUploadService {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Value("${aliyun.oss.folder}")
    private String folder;

    @Value("${aliyun.oss.public-base-url:https://${aliyun.oss.bucket-name}.${aliyun.oss.endpoint}}")
    private String publicBaseUrl;

    @Override
    public String uploadImage(MultipartFile file) {
        return uploadImage(file, folder);
    }

    @Override
    public String uploadImage(MultipartFile file, String targetFolder) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try (InputStream inputStream = file.getInputStream()) {
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String fileName = normalizeFolder(targetFolder) + datePath + "/" + UUID.randomUUID().toString().replace("-", "")
                    + getExtension(file.getOriginalFilename());
            ossClient.putObject(bucketName, fileName, inputStream);
            return normalizeBaseUrl(publicBaseUrl) + "/" + fileName;
        } catch (Exception e) {
            log.error("OSS上传失败", e);
            throw new RuntimeException("图片上传失败: " + e.getMessage());
        } finally {
            ossClient.shutdown();
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String normalizeFolder(String value) {
        String normalized = value == null || value.isBlank() ? folder : value.trim();
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private String normalizeBaseUrl(String value) {
        String normalized = value == null || value.isBlank()
                ? "https://" + bucketName + "." + endpoint
                : value.trim();
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
