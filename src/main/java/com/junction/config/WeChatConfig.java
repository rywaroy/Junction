package com.junction.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 微信小程序配置。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wechat.mini")
public class WeChatConfig {

    private String appId;
    private String appSecret;
}
