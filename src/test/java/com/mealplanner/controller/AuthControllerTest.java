package com.mealplanner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealplanner.dto.LoginRequest;
import com.mealplanner.dto.RegisterRequest;
import com.mealplanner.dto.UpdateNicknameRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken(String username, String password) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
            .get("data").get("token").asText();
    }

    @Test
    void register_success() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setPassword("password123");
        req.setNickname("测试用户");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.token").isNotEmpty())
            .andExpect(jsonPath("$.data.username").value("testuser"))
            .andExpect(jsonPath("$.data.nickname").value("测试用户"));
    }

    @Test
    void register_duplicateUsername() throws Exception {
        registerAndGetToken("testuser", "password123");

        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setPassword("password456");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("用户名已被注册"));
    }

    @Test
    void login_success() throws Exception {
        registerAndGetToken("testuser", "password123");

        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.token").isNotEmpty())
            .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void login_wrongPassword() throws Exception {
        registerAndGetToken("testuser", "password123");

        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("wrong");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void me_success() throws Exception {
        String token = registerAndGetToken("testuser", "password123");

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void me_withoutToken() throws Exception {
        // /api/v1/auth/** is permitAll, but me() requires valid JWT to get userId
        // Without token, the cast to Long fails, resulting in 400
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void updateNickname_success() throws Exception {
        String token = registerAndGetToken("testuser", "password123");

        UpdateNicknameRequest req = new UpdateNicknameRequest();
        req.setNickname("新昵称");

        mockMvc.perform(put("/api/v1/auth/nickname")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.nickname").value("新昵称"));
    }

    @Test
    void updatePassword_success() throws Exception {
        String token = registerAndGetToken("testuser", "password123");

        String body = "{\"currentPassword\":\"password123\",\"newPassword\":\"newpassword456\"}";

        mockMvc.perform(put("/api/v1/auth/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }
}
