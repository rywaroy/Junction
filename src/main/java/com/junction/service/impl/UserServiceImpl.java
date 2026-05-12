package com.junction.service.impl;

import com.junction.common.BusinessException;
import com.junction.dto.UserUpdateRequest;
import com.junction.entity.User;
import com.junction.mapper.UserMapper;
import com.junction.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    @Override
    public void updateProfile(Long id, UserUpdateRequest request) {
        userMapper.updateProfile(id, request.getNickname(), request.getAvatarUrl());
    }
}
