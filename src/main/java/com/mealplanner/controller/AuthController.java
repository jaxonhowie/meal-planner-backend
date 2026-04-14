package com.mealplanner.controller;

import com.mealplanner.dto.ApiResponse;
import com.mealplanner.dto.AuthResponse;
import com.mealplanner.dto.LoginRequest;
import com.mealplanner.dto.RegisterRequest;
import com.mealplanner.dto.UpdateNicknameRequest;
import com.mealplanner.dto.UpdatePasswordRequest;
import com.mealplanner.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@RequestBody RegisterRequest req) {
        return ApiResponse.success(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest req) {
        return ApiResponse.success(authService.login(req));
    }

    /** 刷新用户信息（前端启动时调用，验证 token 有效性） */
    @GetMapping("/me")
    public ApiResponse<AuthResponse> me() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(authService.me(userId));
    }

    @PutMapping("/nickname")
    public ApiResponse<AuthResponse> updateNickname(@RequestBody UpdateNicknameRequest req) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(authService.updateNickname(userId, req.getNickname()));
    }

    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(@RequestBody UpdatePasswordRequest req) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        authService.updatePassword(userId, req.getCurrentPassword(), req.getNewPassword());
        return ApiResponse.success();
    }
}
