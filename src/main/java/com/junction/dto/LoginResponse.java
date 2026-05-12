package com.junction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "会话 token，后续请求 Authorization: Bearer {token}")
    private String token;

    @Schema(description = "用户 ID")
    private Long userId;

    private String nickname;

    private String avatarUrl;

    private String phone;

    @Schema(description = "是否首次登录的新用户")
    private boolean newUser;
}
