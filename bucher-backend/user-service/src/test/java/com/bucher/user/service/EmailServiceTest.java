package com.bucher.user.service;

import com.bucher.common.exception.BusinessException;
import com.bucher.user.service.impl.EmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * EmailService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBucket<String> codeBucket;

    @Mock
    private RBucket<Integer> limitBucket;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "from", "test@example.com");
        ReflectionTestUtils.setField(emailService, "codeExpire", 300);
        ReflectionTestUtils.setField(emailService, "codeInterval", 60);
        ReflectionTestUtils.setField(emailService, "codeLength", 6);
    }

    @Test
    @DisplayName("发送验证码成功")
    void testSendVerificationCodeSuccess() {
        doReturn(limitBucket).when(redissonClient).getBucket(startsWith("user:email:limit:"));
        when(limitBucket.isExists()).thenReturn(false);
        doReturn(codeBucket).when(redissonClient).getBucket(startsWith("user:email:code:"));
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailService.sendVerificationCode("test@example.com", "register"));

        verify(codeBucket).set(anyString(), eq(300L), any());
        verify(limitBucket).set(eq(1), eq(60L), any());
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("发送验证码失败-发送过于频繁")
    void testSendVerificationCodeTooFrequent() {
        doReturn(limitBucket).when(redissonClient).getBucket(startsWith("user:email:limit:"));
        when(limitBucket.isExists()).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> emailService.sendVerificationCode("test@example.com", "register"));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("验证码验证成功")
    void testVerifyCodeSuccess() {
        doReturn(codeBucket).when(redissonClient).getBucket(startsWith("user:email:code:"));
        when(codeBucket.isExists()).thenReturn(true);
        when(codeBucket.get()).thenReturn("123456");

        boolean result = emailService.verifyCode("test@example.com", "123456");

        assertTrue(result);
    }

    @Test
    @DisplayName("验证码验证失败-验证码不存在")
    void testVerifyCodeFailNotExists() {
        doReturn(codeBucket).when(redissonClient).getBucket(startsWith("user:email:code:"));
        when(codeBucket.isExists()).thenReturn(false);

        boolean result = emailService.verifyCode("test@example.com", "123456");

        assertFalse(result);
    }

    @Test
    @DisplayName("验证码验证失败-验证码不匹配")
    void testVerifyCodeFailMismatch() {
        doReturn(codeBucket).when(redissonClient).getBucket(startsWith("user:email:code:"));
        when(codeBucket.isExists()).thenReturn(true);
        when(codeBucket.get()).thenReturn("654321");

        boolean result = emailService.verifyCode("test@example.com", "123456");

        assertFalse(result);
    }

    @Test
    @DisplayName("删除验证码成功")
    void testDeleteCodeSuccess() {
        doReturn(codeBucket).when(redissonClient).getBucket(startsWith("user:email:code:"));

        assertDoesNotThrow(() -> emailService.deleteCode("test@example.com"));

        verify(codeBucket).delete();
    }
}
