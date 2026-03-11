package com.whu.distributed.seckill.user.mapper;

import com.whu.distributed.seckill.user.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("SELECT id, username, password_hash, phone, created_at, updated_at FROM users WHERE username = #{username} LIMIT 1")
    User findByUsername(@Param("username") String username);

    @Select("SELECT id, username, password_hash, phone, created_at, updated_at FROM users WHERE id = #{id}")
    User findById(@Param("id") Long id);

    @Insert("""
            INSERT INTO users(username, password_hash, phone, created_at, updated_at)
            VALUES(#{username}, #{passwordHash}, #{phone}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
}
