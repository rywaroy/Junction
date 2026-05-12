package com.junction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "OSS 直传签名响应")
public class OssSignatureResponse {

    private String host;
    private String key;
    private String policy;

    @Schema(description = "签名版本，固定 OSS4-HMAC-SHA256")
    private String xOssSignatureVersion;

    private String xOssCredential;
    private String xOssDate;
    private String xOssSignature;

    @Schema(description = "STS 临时凭证 token")
    private String securityToken;
}
