package com.junction.controller;

import com.junction.common.ApiResult;
import com.junction.service.OssService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文件上传", description = "服务端代传文件到 OSS")
@RestController
@ConditionalOnProperty(prefix = "oss", name = "enabled", havingValue = "true")
@RequestMapping("/api/upload")
public class UploadController {

    private final OssService ossService;

    public UploadController(OssService ossService) {
        this.ossService = ossService;
    }

    @Operation(summary = "上传图片", description = "服务端代传图片到 OSS，返回预签名 URL")
    @PostMapping("/image")
    public ApiResult<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return ApiResult.success(ossService.uploadImage(file));
    }
}
