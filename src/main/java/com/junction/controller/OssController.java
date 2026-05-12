package com.junction.controller;

import com.junction.common.ApiResult;
import com.junction.dto.OssSignatureResponse;
import com.junction.service.OssService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "OSS 签名", description = "前端直传所需的临时签名")
@RestController
@RequestMapping("/api/oss")
public class OssController {

    private final OssService ossService;

    public OssController(OssService ossService) {
        this.ossService = ossService;
    }

    @Operation(summary = "获取 OSS 直传签名", description = "返回 STS 临时凭证 + V4 PostPolicy 签名，前端直接 POST 到 host")
    @GetMapping("/signature")
    public ApiResult<OssSignatureResponse> signature(
            @Parameter(description = "文件扩展名（不含点），如 jpg/png") @RequestParam String ext) {
        return ApiResult.success(ossService.generateSignature(ext));
    }
}
