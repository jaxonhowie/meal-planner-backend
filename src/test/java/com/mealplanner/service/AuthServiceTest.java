package com.mealplanner.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mealplanner.config.JwtUtil;
import com.mealplanner.dto.AuthResponse;
import com.mealplanner.dto.LoginRequest;
import com.mealplanner.dto.RegisterRequest;
import com.mealplanner.entity.User;
import com.mealplanner.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @Test
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setPassword("password123");
        req.setNickname("测试用户");

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("password123")).thenReturn("encoded123");
        when(userMapper.insert(any(User.class))).thenReturn(1);
        when(jwtUtil.generate(any())).thenReturn("token123");

        AuthResponse result = authService.register(req);

        assertThat(result.getToken()).isEqualTo("token123");
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getNickname()).isEqualTo("测试用户");
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void register_defaultNickname() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setPassword("password123");
        req.setNickname(null);

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("password123")).thenReturn("encoded123");
        when(userMapper.insert(any(User.class))).thenReturn(1);
        when(jwtUtil.generate(any())).thenReturn("token123");

        AuthResponse result = authService.register(req);

        assertThat(result.getNickname()).isEqualTo("testuser");
    }

    @Test
    void register_duplicateUsername() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("existing");
        req.setPassword("password123");

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("用户名已被注册");
    }

    @Test
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("password123");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("encoded123");
        user.setNickname("测试用户");
        user.setFamilyId(100L);

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("password123", "encoded123")).thenReturn(true);
        when(jwtUtil.generate(1L)).thenReturn("token123");

        AuthResponse result = authService.login(req);

        assertThat(result.getToken()).isEqualTo("token123");
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getFamilyId()).isEqualTo(100L);
    }

    @Test
    void login_userNotFound() {
        LoginRequest req = new LoginRequest();
        req.setUsername("nouser");
        req.setPassword("password123");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("用户名或密码错误");
    }

    @Test
    void login_wrongPassword() {
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("wrong");

        User user = new User();
        user.setId(1L);
        user.setPassword("encoded123");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("wrong", "encoded123")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("用户名或密码错误");
    }

    @Test
    void me_success() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setNickname("测试用户");
        user.setFamilyId(100L);

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(jwtUtil.generate(1L)).thenReturn("newtoken");

        AuthResponse result = authService.me(1L);

        assertThat(result.getToken()).isEqualTo("newtoken");
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    void me_userNotFound() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> authService.me(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("用户不存在");
    }

    @Test
    void updateNickname_success() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setNickname("新昵称");
        user.setFamilyId(100L);

        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(jwtUtil.generate(1L)).thenReturn("token");

        AuthResponse result = authService.updateNickname(1L, "  新昵称  ");

        assertThat(result.getNickname()).isEqualTo("新昵称");
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void updateNickname_blank() {
        assertThatThrownBy(() -> authService.updateNickname(1L, "  "))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("昵称不能为空");

        assertThatThrownBy(() -> authService.updateNickname(1L, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("昵称不能为空");
    }

    @Test
    void updatePassword_success() {
        User user = new User();
        user.setId(1L);
        user.setPassword("oldEncoded");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("oldPass", "oldEncoded")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("newEncoded");
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        authService.updatePassword(1L, "oldPass", "newPass123");

        verify(passwordEncoder).encode("newPass123");
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void updatePassword_wrongCurrentPassword() {
        User user = new User();
        user.setId(1L);
        user.setPassword("oldEncoded");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("wrong", "oldEncoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.updatePassword(1L, "wrong", "newPass123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("当前密码错误");
    }

    @Test
    void updatePassword_shortNewPassword() {
        assertThatThrownBy(() -> authService.updatePassword(1L, "old", "short"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("新密码至少 6 位");
    }
}
