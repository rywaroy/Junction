package com.junction.service;

import com.junction.dto.OssSignatureResponse;
import org.springframework.web.multipart.MultipartFile;

public interface OssService {

    /** 服务端代传：上传到 OSS 并返回预签名 URL */
    String uploadImage(MultipartFile file);

    /** 前端直传：返回 STS 临时凭证 + V4 PostPolicy 签名 */
    OssSignatureResponse generateSignature(String ext);
}
