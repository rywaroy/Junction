package com.junction.service.impl;

import com.junction.common.BusinessException;
import com.junction.dto.LoginRequest;
import com.junction.dto.LoginResponse;
import com.junction.entity.User;
import com.junction.mapper.UserMapper;
import com.junction.service.AuthService;
import com.junction.service.RiskControlService;
import com.junction.service.WeChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 登录服务实现。
 *
 * 流程：
 * 1) IP 风控（异常 IP 拒绝）
 * 2) jscode -> openid
 * 3) 不存在则创建用户，记录注册 IP
 * 4) 已被标记作弊则拒绝
 * 5) 可选拉取手机号
 * 6) 生成 token 存 Redis（7 天）
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final long TOKEN_TTL_DAYS = 7;

    private final WeChatService weChatService;
    private final UserMapper userMapper;
    private final RiskControlService riskControlService;
    private final StringRedisTemplate redisTemplate;

    public AuthServiceImpl(WeChatService weChatService,
                           UserMapper userMapper,
                           RiskControlService riskControlService,
                           StringRedisTemplate redisTemplate) {
        this.weChatService = weChatService;
        this.userMapper = userMapper;
        this.riskControlService = riskControlService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String clientIp) {
        if (riskControlService.isAbnormalIp(clientIp)) {
            log.warn("Abnormal IP detected during login: {}", clientIp);
            throw new BusinessException("当前网络环境异常，请稍后再试");
        }

        String openId = weChatService.getOpenId(request.getCode());

        User user = userMapper.selectByOpenId(openId);
        boolean isNewUser = (user == null);

        if (isNewUser) {
            user = new User();
            user.setOpenId(openId);
            user.setNickname("");
            user.setAvatarUrl("");
            userMapper.insert(user);
            riskControlService.recordRegistration(clientIp, user.getId());
        }

        if (riskControlService.isCheater(user.getId())) {
            throw new BusinessException("账号异常，请联系客服");
        }

        if (request.getPhoneCode() != null && !request.getPhoneCode().isBlank()) {
            String phone = weChatService.getPhoneNumber(request.getPhoneCode());
            if (phone != null) {
                userMapper.updatePhone(user.getId(), phone);
                user.setPhone(phone);
            }
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set("auth:token:" + token,
                String.valueOf(user.getId()), TOKEN_TTL_DAYS, TimeUnit.DAYS);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setNickname(user.getNickname());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setPhone(user.getPhone());
        response.setNewUser(isNewUser);
        return response;
    }
}
