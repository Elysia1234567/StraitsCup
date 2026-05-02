package com.omnisource.service;

import org.springframework.web.multipart.MultipartFile;

public interface OssUploadService {
    String uploadImage(MultipartFile file);
    String uploadImage(MultipartFile file, String folder);
}
