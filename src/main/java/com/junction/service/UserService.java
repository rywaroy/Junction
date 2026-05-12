package com.junction.service;

import com.junction.entity.User;
import com.junction.dto.UserUpdateRequest;

public interface UserService {

    User getById(Long id);

    void updateProfile(Long id, UserUpdateRequest request);
}
