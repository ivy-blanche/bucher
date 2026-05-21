package com.bucher.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bucher.common.enums.ResultCodeEnum;
import com.bucher.common.exception.BusinessException;
import com.bucher.user.dto.LoginDTO;
import com.bucher.user.dto.RegisterDTO;
import com.bucher.user.dto.ResetPasswordDTO;
import com.bucher.user.dto.SendCodeDTO;
import com.bucher.user.entity.AdminClass;
import com.bucher.user.entity.Department;
import com.bucher.user.entity.User;
import com.bucher.user.enums.UserRoleEnum;
import com.bucher.user.mapper.AdminClassMapper;
import com.bucher.user.mapper.DepartmentMapper;
import com.bucher.user.mapper.UserMapper;
import com.bucher.user.service.impl.AuthServiceImpl;
import com.bucher.user.util.JwtUtil;
import com.bucher.user.vo.LoginVO;
import com.bucher.user.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private AdminClassMapper adminClassMapper;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBucket<String> tokenBucket;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private User selfRegisterUser;
    private Department testDept;
    private AdminClass testAdminClass;

    @BeforeEach
    void setUp() {
        // 管理员添加的用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserNo("2024001");
        testUser.setRealName("张三");
        testUser.setPassword("encoded_password");
        testUser.setEmail("zhangsan@example.com");
        testUser.setRole(3);
        testUser.setDeptId(1L);
        testUser.setAdminClassId(1L);
        testUser.setSource(1);
        testUser.setStatus(1);

        // 自主注册的用户
        selfRegisterUser = new User();
        selfRegisterUser.setId(2L);
        selfRegisterUser.setUserNo(null);
        selfRegisterUser.setRealName("李四");
        selfRegisterUser.setPassword("encoded_password");
        selfRegisterUser.setEmail("lisi@example.com");
        selfRegisterUser.setRole(3);
        selfRegisterUser.setSource(2);
        selfRegisterUser.setStatus(1);

        testDept = new Department();
        testDept.setId(1L);
        testDept.setName("计算机学院");
        testDept.setType(1);

        testAdminClass = new AdminClass();
        testAdminClass.setId(1L);
        testAdminClass.setName("2024级计算机1班");
        testAdminClass.setDeptId(1L);
    }

    // ==================== 登录测试 ====================

    @Test
    @DisplayName("登录成功-学号/工号登录")
    void testLoginSuccessWithUserNo() {
        LoginDTO dto = new LoginDTO();
        dto.setAccount("2024001");
        dto.setPassword("password123");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "2024001", 3)).thenReturn("test_token");
        doReturn(tokenBucket).when(redissonClient).getBucket(anyString());

        LoginVO result = authService.login(dto);

        assertNotNull(result);
        assertEquals("test_token", result.getToken());
        assertEquals(1L, result.getUserId());
        assertEquals("2024001", result.getUserNo());
        assertEquals("张三", result.getRealName());
    }

    @Test
    @DisplayName("登录成功-邮箱登录")
    void testLoginSuccessWithEmail() {
        LoginDTO dto = new LoginDTO();
        dto.setAccount("lisi@example.com");
        dto.setPassword("password123");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(selfRegisterUser);
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(jwtUtil.generateToken(2L, null, 3)).thenReturn("test_token");
        doReturn(tokenBucket).when(redissonClient).getBucket(anyString());

        LoginVO result = authService.login(dto);

        assertNotNull(result);
        assertEquals("test_token", result.getToken());
        assertEquals(2L, result.getUserId());
        assertNull(result.getUserNo());
        assertEquals("李四", result.getRealName());
    }

    @Test
    @DisplayName("登录失败-用户不存在")
    void testLoginFailUserNotFound() {
        LoginDTO dto = new LoginDTO();
        dto.setAccount("notexist@example.com");
        dto.setPassword("password123");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login(dto));

        assertEquals(ResultCodeEnum.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("登录失败-用户已禁用")
    void testLoginFailUserDisabled() {
        testUser.setStatus(0);
        LoginDTO dto = new LoginDTO();
        dto.setAccount("2024001");
        dto.setPassword("password123");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login(dto));

        assertEquals(ResultCodeEnum.USER_DISABLED.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("登录失败-密码错误")
    void testLoginFailWrongPassword() {
        LoginDTO dto = new LoginDTO();
        dto.setAccount("2024001");
        dto.setPassword("wrong_password");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
        when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login(dto));

        assertEquals(ResultCodeEnum.USER_PASSWORD_ERROR.getCode(), exception.getCode());
    }

    // ==================== 注册测试 ====================

    @Test
    @DisplayName("注册成功")
    void testRegisterSuccess() {
        RegisterDTO dto = new RegisterDTO();
        dto.setEmail("newuser@example.com");
        dto.setEmailCode("123456");
        dto.setRealName("王五");
        dto.setPassword("Password123");

        when(emailService.verifyCode("newuser@example.com", "123456")).thenReturn(true);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded_new_password");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        assertDoesNotThrow(() -> authService.register(dto));

        verify(emailService).verifyCode("newuser@example.com", "123456");
        verify(emailService).deleteCode("newuser@example.com");
        verify(userMapper).insert(any(User.class));
    }

    @Test
    @DisplayName("注册失败-验证码错误")
    void testRegisterFailWrongCode() {
        RegisterDTO dto = new RegisterDTO();
        dto.setEmail("newuser@example.com");
        dto.setEmailCode("wrong_code");
        dto.setRealName("王五");
        dto.setPassword("Password123");

        when(emailService.verifyCode("newuser@example.com", "wrong_code")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(dto));

        assertEquals("验证码错误或已过期", exception.getMessage());
    }

    @Test
    @DisplayName("注册失败-邮箱已被注册")
    void testRegisterFailEmailExists() {
        RegisterDTO dto = new RegisterDTO();
        dto.setEmail("zhangsan@example.com");
        dto.setEmailCode("123456");
        dto.setRealName("王五");
        dto.setPassword("Password123");

        when(emailService.verifyCode("zhangsan@example.com", "123456")).thenReturn(true);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(dto));

        assertEquals("该邮箱已被注册", exception.getMessage());
    }

    // ==================== 发送验证码测试 ====================

    @Test
    @DisplayName("发送验证码-注册场景")
    void testSendCodeRegister() {
        SendCodeDTO dto = new SendCodeDTO();
        dto.setEmail("new@example.com");
        dto.setType("register");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        doNothing().when(emailService).sendVerificationCode("new@example.com", "register");

        assertDoesNotThrow(() -> authService.sendCode(dto));

        verify(emailService).sendVerificationCode("new@example.com", "register");
    }

    @Test
    @DisplayName("发送验证码-邮箱已被注册")
    void testSendCodeEmailExists() {
        SendCodeDTO dto = new SendCodeDTO();
        dto.setEmail("zhangsan@example.com");
        dto.setType("register");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.sendCode(dto));

        assertEquals("该邮箱已被注册", exception.getMessage());
    }

    // ==================== 重置密码测试 ====================

    @Test
    @DisplayName("重置密码成功")
    void testResetPasswordSuccess() {
        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setEmail("zhangsan@example.com");
        dto.setEmailCode("123456");
        dto.setNewPassword("NewPassword123");

        when(emailService.verifyCode("zhangsan@example.com", "123456")).thenReturn(true);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
        when(passwordEncoder.encode("NewPassword123")).thenReturn("new_encoded_password");
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        doReturn(tokenBucket).when(redissonClient).getBucket(anyString());

        assertDoesNotThrow(() -> authService.resetPassword(dto));

        verify(emailService).deleteCode("zhangsan@example.com");
        verify(userMapper).updateById(any(User.class));
        verify(tokenBucket).delete();
    }

    // ==================== 登出测试 ====================

    @Test
    @DisplayName("登出成功")
    void testLogoutSuccess() {
        doReturn(tokenBucket).when(redissonClient).getBucket(anyString());

        assertDoesNotThrow(() -> authService.logout(1L));

        verify(tokenBucket).delete();
    }

    // ==================== 获取当前用户测试 ====================

    @Test
    @DisplayName("获取当前用户信息-管理员添加的用户")
    void testGetCurrentUserAdminAdded() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(departmentMapper.selectById(1L)).thenReturn(testDept);
        when(adminClassMapper.selectById(1L)).thenReturn(testAdminClass);

        UserVO result = authService.getCurrentUser(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("2024001", result.getUserNo());
        assertEquals("张三", result.getRealName());
        assertEquals(1, result.getSource());
        assertNull(result.getAuditStatus());
        assertEquals("计算机学院", result.getDeptName());
        assertEquals("2024级计算机1班", result.getAdminClassName());
    }

    @Test
    @DisplayName("获取当前用户信息-自主注册的用户")
    void testGetCurrentUserSelfRegistered() {
        when(userMapper.selectById(2L)).thenReturn(selfRegisterUser);

        UserVO result = authService.getCurrentUser(2L);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertNull(result.getUserNo());
        assertEquals("李四", result.getRealName());
        assertEquals(2, result.getSource());
        assertNull(result.getAuditStatus());
        assertNull(result.getDeptName());
        assertNull(result.getAdminClassName());
    }
}
