package com.junction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录请求")
public class LoginRequest {

    @Schema(description = "微信小程序登录临时凭证")
    private String code;

    @Schema(description = "获取手机号的临时凭证（可选）")
    private String phoneCode;
}
