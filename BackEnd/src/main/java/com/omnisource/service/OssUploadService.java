package com.omnisource.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface OssUploadService {
    String uploadImage(MultipartFile file);
    String uploadImage(MultipartFile file, String folder);
    String uploadImage(InputStream inputStream, String originalFilename, String folder);
}
