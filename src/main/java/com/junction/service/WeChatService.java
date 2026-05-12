package com.junction.service;

/**
 * 微信小程序服务。
 */
public interface WeChatService {

    /** 通过 jscode 换取 openid */
    String getOpenId(String code);

    /** 获取小程序 access_token（带 Redis 缓存） */
    String getAccessToken();

    /** 通过 phoneCode 换取手机号；失败返回 null */
    String getPhoneNumber(String phoneCode);
}
