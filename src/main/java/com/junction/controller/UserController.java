package com.junction.controller;

import com.junction.common.ApiResult;
import com.junction.common.UserContext;
import com.junction.dto.UserUpdateRequest;
import com.junction.entity.User;
import com.junction.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户管理", description = "当前登录用户相关接口")
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "查询当前用户信息")
    @GetMapping("/info")
    public ApiResult<User> info() {
        Long userId = UserContext.getUserId();
        return ApiResult.success(userService.getById(userId));
    }

    @Operation(summary = "更新昵称/头像")
    @PostMapping("/update-profile")
    public ApiResult<Void> updateProfile(@RequestBody UserUpdateRequest request) {
        Long userId = UserContext.getUserId();
        userService.updateProfile(userId, request);
        return ApiResult.success();
    }
}
