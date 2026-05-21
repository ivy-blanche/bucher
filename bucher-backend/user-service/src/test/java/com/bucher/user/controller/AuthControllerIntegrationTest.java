package com.bucher.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.bucher.user.dto.LoginDTO;
import com.bucher.user.dto.RegisterDTO;
import com.bucher.user.dto.SendCodeDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 集成测试
 *
 * 注意：运行此测试需要配置测试数据库和 Redis
 * 可以使用 Testcontainers 或嵌入式数据库
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("登录接口参数校验-缺少account")
    void testLoginValidationMissingAccount() throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setPassword("password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("登录接口参数校验-缺少password")
    void testLoginValidationMissingPassword() throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setAccount("user@example.com");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("注册接口参数校验-邮箱格式错误")
    void testRegisterValidationInvalidEmail() throws Exception {
        RegisterDTO dto = new RegisterDTO();
        dto.setEmail("invalid-email");
        dto.setEmailCode("123456");
        dto.setRealName("张三");
        dto.setPassword("Password123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("注册接口参数校验-密码格式错误")
    void testRegisterValidationInvalidPassword() throws Exception {
        RegisterDTO dto = new RegisterDTO();
        dto.setEmail("test@example.com");
        dto.setEmailCode("123456");
        dto.setRealName("张三");
        dto.setPassword("123"); // 不符合密码规则

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("注册接口参数校验-缺少姓名")
    void testRegisterValidationMissingRealName() throws Exception {
        RegisterDTO dto = new RegisterDTO();
        dto.setEmail("test@example.com");
        dto.setEmailCode("123456");
        dto.setPassword("Password123");
        // 缺少 realName

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("发送验证码接口参数校验-邮箱为空")
    void testSendCodeValidationEmptyEmail() throws Exception {
        SendCodeDTO dto = new SendCodeDTO();
        dto.setType("register");

        mockMvc.perform(post("/auth/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
