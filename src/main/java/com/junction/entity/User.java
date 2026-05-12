package com.junction.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体。
 */
@Data
public class User {

    private Long id;

    /** 微信 openid（用户唯一标识） */
    private String openId;

    private String nickname;

    private String avatarUrl;

    /** 手机号（纯数字，不含区号） */
    private String phone;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
