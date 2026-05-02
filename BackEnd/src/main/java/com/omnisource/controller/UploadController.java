package com.omnisource.controller;

import com.omnisource.service.OssUploadService;
import com.omnisource.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final OssUploadService ossUploadService;

    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = ossUploadService.uploadImage(file);
        return Result.success(Map.of(
                "url", url,
                "filename", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown"
        ));
    }
}
