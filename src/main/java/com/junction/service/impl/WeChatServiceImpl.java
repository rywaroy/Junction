package com.junction.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.junction.common.BusinessException;
import com.junction.config.WeChatConfig;
import com.junction.service.WeChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 微信小程序服务实现。
 *
 * - jscode2session 换 openid
 * - access_token Redis 缓存（提前 200 秒过期，避免边界）
 * - getuserphonenumber 拿手机号
 */
@Slf4j
@Service
public class WeChatServiceImpl implements WeChatService {

    private static final String ACCESS_TOKEN_KEY = "wechat:access_token";
    private static final long ACCESS_TOKEN_EXPIRE_SECONDS = 7000;

    private final WeChatConfig wechatConfig;
    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeChatServiceImpl(WeChatConfig wechatConfig, StringRedisTemplate redisTemplate) {
        this.wechatConfig = wechatConfig;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String getOpenId(String code) {
        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                wechatConfig.getAppId(), wechatConfig.getAppSecret(), code);
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode json = objectMapper.readTree(response);
            if (json.has("errcode") && json.get("errcode").asInt() != 0) {
                log.error("WeChat login error: {}", response);
                throw new BusinessException("微信登录失败: " + json.get("errmsg").asText());
            }
            return json.get("openid").asText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("WeChat API call failed", e);
            throw new BusinessException("微信登录服务异常");
        }
    }

    @Override
    public String getAccessToken() {
        String cachedToken = redisTemplate.opsForValue().get(ACCESS_TOKEN_KEY);
        if (cachedToken != null) {
            return cachedToken;
        }

        String url = String.format(
                "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                wechatConfig.getAppId(), wechatConfig.getAppSecret());
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode json = objectMapper.readTree(response);
            if (json.has("errcode") && json.get("errcode").asInt() != 0) {
                log.error("WeChat get access_token error: {}", response);
                throw new BusinessException("获取微信 access_token 失败: " + json.get("errmsg").asText());
            }
            String accessToken = json.get("access_token").asText();
            redisTemplate.opsForValue().set(ACCESS_TOKEN_KEY, accessToken, ACCESS_TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
            return accessToken;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("WeChat get access_token API call failed", e);
            throw new BusinessException("微信 access_token 服务异常");
        }
    }

    @Override
    public String getPhoneNumber(String phoneCode) {
        if (phoneCode == null || phoneCode.isBlank()) {
            return null;
        }
        try {
            String accessToken = getAccessToken();
            String url = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + accessToken;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String jsonBody = "{\"code\":\"" + phoneCode + "\"}";
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            String response = restTemplate.postForObject(url, request, String.class);
            log.info("WeChat phone API response: {}", response);
            JsonNode json = objectMapper.readTree(response);

            if (json.has("errcode") && json.get("errcode").asInt() != 0) {
                log.warn("WeChat get phone number error: {}", response);
                return null;
            }

            JsonNode phoneInfo = json.get("phone_info");
            if (phoneInfo != null && phoneInfo.has("purePhoneNumber")) {
                return phoneInfo.get("purePhoneNumber").asText();
            }
            return null;
        } catch (Exception e) {
            log.warn("WeChat get phone number failed", e);
            return null;
        }
    }
}
