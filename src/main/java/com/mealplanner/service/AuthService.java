package com.mealplanner.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mealplanner.config.JwtUtil;
import com.mealplanner.dto.AuthResponse;
import com.mealplanner.dto.LoginRequest;
import com.mealplanner.dto.RegisterRequest;
import com.mealplanner.entity.User;
import com.mealplanner.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest req) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername())) > 0) {
            throw new RuntimeException("用户名已被注册");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname() != null ? req.getNickname() : req.getUsername());
        userMapper.insert(user);

        String token = jwtUtil.generate(user.getId());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getNickname(), null);
    }

    public AuthResponse login(LoginRequest req) {
        // 需要 password 字段，用独立查询
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, req.getUsername())
            .select(User::getId, User::getUsername, User::getPassword,
                    User::getNickname, User::getFamilyId));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        String token = jwtUtil.generate(user.getId());
        return new AuthResponse(token, user.getId(), user.getUsername(),
                user.getNickname(), user.getFamilyId());
    }

    public AuthResponse me(Long userId) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
            .eq(User::getId, userId)
            .select(User::getId, User::getUsername, User::getNickname, User::getFamilyId));
        if (user == null) throw new RuntimeException("用户不存在");
        String token = jwtUtil.generate(userId); // 刷新 token
        return new AuthResponse(token, user.getId(), user.getUsername(),
                user.getNickname(), user.getFamilyId());
    }

    @Transactional
    public AuthResponse updateNickname(Long userId, String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new RuntimeException("昵称不能为空");
        }
        User update = new User();
        update.setId(userId);
        update.setNickname(nickname.trim());
        userMapper.updateById(update);
        return me(userId);
    }

    @Transactional
    public void updatePassword(Long userId, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("新密码至少 6 位");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
            .eq(User::getId, userId)
            .select(User::getId, User::getPassword));
        if (user == null) throw new RuntimeException("用户不存在");
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("当前密码错误");
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(update);
    }
}
