package com.whu.distributed.seckill.user.controller;

import com.whu.distributed.seckill.common.ApiResponse;
import com.whu.distributed.seckill.user.dto.LoginRequest;
import com.whu.distributed.seckill.user.dto.RegisterRequest;
import com.whu.distributed.seckill.user.dto.UserAuthResponse;
import com.whu.distributed.seckill.user.dto.UserProfileResponse;
import com.whu.distributed.seckill.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<UserAuthResponse> register(@RequestBody RegisterRequest request) {
        return ApiResponse.ok(userService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<UserAuthResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok(userService.login(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserProfileResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(userService.getProfile(id));
    }
}
