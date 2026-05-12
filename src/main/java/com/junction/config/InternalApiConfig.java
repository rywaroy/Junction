package com.junction.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 内部接口 API Key 配置。
 *
 * 用于 /api/internal/** 类接口的鉴权（如管理后台、报表导出等）。
 * 空字符串表示允许任意请求（仅限开发阶段）。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "internal-api")
public class InternalApiConfig {

    private String apiKey = "";
}
