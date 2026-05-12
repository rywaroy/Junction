package com.junction.mapper;

import com.junction.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User selectById(@Param("id") Long id);

    User selectByOpenId(@Param("openId") String openId);

    int insert(User user);

    int updateProfile(@Param("id") Long id,
                      @Param("nickname") String nickname,
                      @Param("avatarUrl") String avatarUrl);

    int updatePhone(@Param("id") Long id, @Param("phone") String phone);
}
