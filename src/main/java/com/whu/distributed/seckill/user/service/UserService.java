package com.whu.distributed.seckill.user.service;

import com.whu.distributed.seckill.user.dto.LoginRequest;
import com.whu.distributed.seckill.user.dto.RegisterRequest;
import com.whu.distributed.seckill.user.dto.UserAuthResponse;
import com.whu.distributed.seckill.user.dto.UserProfileResponse;
import com.whu.distributed.seckill.user.entity.User;
import com.whu.distributed.seckill.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public UserAuthResponse register(RegisterRequest request) {
        validateRegisterRequest(request);

        User existed = userMapper.findByUsername(request.getUsername());
        if (existed != null) {
            throw new IllegalArgumentException("username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPhone(request.getPhone() == null ? null : request.getPhone().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userMapper.insert(user);

        return new UserAuthResponse(user.getId(), user.getUsername(), generateToken());
    }

    public UserAuthResponse login(LoginRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("username and password are required");
        }

        User user = userMapper.findByUsername(request.getUsername().trim());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("invalid username or password");
        }

        return new UserAuthResponse(user.getId(), user.getUsername(), generateToken());
    }

    public UserProfileResponse getProfile(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("invalid user id");
        }

        User user = userMapper.findById(id);
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }

        return new UserProfileResponse(user.getId(), user.getUsername(), user.getPhone());
    }

    private static String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static void validateRegisterRequest(RegisterRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("username and password are required");
        }

        if (request.getPassword().length() < 6) {
            throw new IllegalArgumentException("password must be at least 6 characters");
        }
    }
}
