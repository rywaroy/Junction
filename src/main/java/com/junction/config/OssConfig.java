package com.junction.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConditionalOnProperty(prefix = "oss", name = "enabled", havingValue = "true")
@ConfigurationProperties(prefix = "oss")
public class OssConfig {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String region;

    /** STS AssumeRole 配置（前端直传时使用临时凭证） */
    private String roleArn;
    private String roleSessionName = "oss-upload-session";
    private Long durationSeconds = 3600L;

    @Bean
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}
