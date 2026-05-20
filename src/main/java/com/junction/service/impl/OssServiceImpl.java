package com.junction.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.auth.sts.AssumeRoleRequest;
import com.aliyuncs.auth.sts.AssumeRoleResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.junction.common.BusinessException;
import com.junction.config.OssConfig;
import com.junction.dto.OssSignatureResponse;
import com.junction.service.OssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 阿里云 OSS 服务实现。
 *
 * 提供两种上传方式：
 * 1) 服务端代传：uploadImage(file) -> 返回预签名 GET URL
 * 2) 前端直传：generateSignature(ext) -> 返回 STS 临时凭证 + PostPolicy 签名
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "oss", name = "enabled", havingValue = "true")
public class OssServiceImpl implements OssService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final long SIGNED_URL_EXPIRATION_MS = 30 * 60 * 1000L;
    private static final long SIGNATURE_EXPIRATION_HOURS = 3;
    private static final String UPLOAD_PATH_PREFIX = "uploads";

    private final OSS ossClient;
    private final OssConfig ossConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OssServiceImpl(OSS ossClient, OssConfig ossConfig) {
        this.ossClient = ossClient;
        this.ossConfig = ossConfig;
    }

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException("仅支持上传图片文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过 5MB");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String objectKey = String.format("%s/%s/%s.%s", UPLOAD_PATH_PREFIX, datePath, UUID.randomUUID(), extension);

        try {
            ossClient.putObject(ossConfig.getBucketName(), objectKey, file.getInputStream());

            Date expiration = new Date(System.currentTimeMillis() + SIGNED_URL_EXPIRATION_MS);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    ossConfig.getBucketName(), objectKey);
            request.setExpiration(expiration);
            URL signedUrl = ossClient.generatePresignedUrl(request);

            log.info("Image uploaded successfully: {}", signedUrl);
            return signedUrl.toString();
        } catch (IOException e) {
            log.error("Failed to upload image to OSS", e);
            throw new BusinessException("图片上传失败，请稍后重试");
        }
    }

    @Override
    public OssSignatureResponse generateSignature(String ext) {
        if (ext == null || ext.isBlank()) {
            throw new BusinessException("请指定文件扩展名");
        }
        String normalizedExt = ext.toLowerCase().trim();
        if (!ALLOWED_EXTENSIONS.contains(normalizedExt)) {
            throw new BusinessException("仅支持图片文件 (jpg, jpeg, png, gif, webp)");
        }

        StsCredentials sts = assumeRole();

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String key = String.format("%s/%s/%s.%s", UPLOAD_PATH_PREFIX, datePath, UUID.randomUUID(), normalizedExt);

        long now = System.currentTimeMillis() / 1000;
        ZonedDateTime dtObj = ZonedDateTime.ofInstant(Instant.ofEpochSecond(now), ZoneId.of("UTC"));
        ZonedDateTime dtObjPlusHours = dtObj.plusHours(SIGNATURE_EXPIRATION_HOURS);

        String xOssDate = dtObj.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        String dateStr = dtObj.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String expirationTime = dtObjPlusHours.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

        String region = ossConfig.getRegion();
        String xOssCredential = String.format("%s/%s/%s/oss/aliyun_v4_request",
                sts.accessKeyId, dateStr, region);

        String policyJson = buildPolicyWithSecurityToken(expirationTime, key, xOssCredential, xOssDate, sts.securityToken);
        String policyBase64 = Base64.getEncoder().encodeToString(policyJson.getBytes());
        String signature = calculateV4Signature(policyBase64, dateStr, region, sts.accessKeySecret);

        String host = String.format("https://%s.%s", ossConfig.getBucketName(), ossConfig.getEndpoint());

        return OssSignatureResponse.builder()
                .host(host)
                .key(key)
                .policy(policyBase64)
                .xOssSignatureVersion("OSS4-HMAC-SHA256")
                .xOssCredential(xOssCredential)
                .xOssDate(xOssDate)
                .xOssSignature(signature)
                .securityToken(sts.securityToken)
                .build();
    }

    private StsCredentials assumeRole() {
        String region = ossConfig.getRegion();
        DefaultProfile profile = DefaultProfile.getProfile(region, ossConfig.getAccessKeyId(), ossConfig.getAccessKeySecret());
        IAcsClient client = new DefaultAcsClient(profile);

        AssumeRoleRequest request = new AssumeRoleRequest();
        request.setRoleArn(ossConfig.getRoleArn());
        request.setRoleSessionName(ossConfig.getRoleSessionName());
        request.setDurationSeconds(ossConfig.getDurationSeconds());

        try {
            AssumeRoleResponse response = client.getAcsResponse(request);
            AssumeRoleResponse.Credentials c = response.getCredentials();
            return new StsCredentials(c.getAccessKeyId(), c.getAccessKeySecret(), c.getSecurityToken());
        } catch (ClientException e) {
            log.error("STS AssumeRole failed: errCode={}, errMsg={}", e.getErrCode(), e.getErrMsg(), e);
            throw new BusinessException("获取上传凭证失败，请稍后重试");
        }
    }

    private String buildPolicyWithSecurityToken(String expirationTime, String key, String xOssCredential,
                                                String xOssDate, String securityToken) {
        try {
            Map<String, Object> policy = new HashMap<>();
            policy.put("expiration", expirationTime);

            List<Object> conditions = new ArrayList<>();
            conditions.add(Map.of("bucket", ossConfig.getBucketName()));
            conditions.add(Map.of("key", key));
            conditions.add(Map.of("x-oss-signature-version", "OSS4-HMAC-SHA256"));
            conditions.add(Map.of("x-oss-credential", xOssCredential));
            conditions.add(Map.of("x-oss-date", xOssDate));
            conditions.add(Map.of("x-oss-security-token", securityToken));
            conditions.add(Arrays.asList("content-length-range", 1, MAX_FILE_SIZE));

            policy.put("conditions", conditions);
            return objectMapper.writeValueAsString(policy);
        } catch (JsonProcessingException e) {
            throw new BusinessException("生成签名失败");
        }
    }

    private String calculateV4Signature(String stringToSign, String dateStr, String region, String accessKeySecret) {
        try {
            byte[] dateKey = hmacSha256(("aliyun_v4" + accessKeySecret).getBytes(), dateStr);
            byte[] dateRegionKey = hmacSha256(dateKey, region);
            byte[] dateRegionServiceKey = hmacSha256(dateRegionKey, "oss");
            byte[] signingKey = hmacSha256(dateRegionServiceKey, "aliyun_v4_request");
            byte[] result = hmacSha256(signingKey, stringToSign);
            return BinaryUtil.toHex(result);
        } catch (Exception e) {
            log.error("Failed to calculate V4 signature", e);
            throw new BusinessException("生成签名失败");
        }
    }

    private byte[] hmacSha256(byte[] key, String data) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
            return mac.doFinal(data.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC-SHA256", e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private record StsCredentials(String accessKeyId, String accessKeySecret, String securityToken) {}
}
