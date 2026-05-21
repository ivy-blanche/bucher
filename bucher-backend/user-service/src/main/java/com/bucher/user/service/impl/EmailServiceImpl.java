package com.bucher.user.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.bucher.common.exception.BusinessException;
import com.bucher.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 邮件服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final RedissonClient redissonClient;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${code.email.expire:300}")
    private Integer codeExpire;

    @Value("${code.email.interval:60}")
    private Integer codeInterval;

    @Value("${code.email.length:6}")
    private Integer codeLength;

    private static final String CODE_KEY_PREFIX = "user:email:code:";
    private static final String LIMIT_KEY_PREFIX = "user:email:limit:";

    @Override
    public void sendVerificationCode(String email, String type) {
        // 检查发送频率
        String limitKey = LIMIT_KEY_PREFIX + email;
        RBucket<Integer> limitBucket = redissonClient.getBucket(limitKey);
        if (limitBucket.isExists()) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }

        // 生成验证码
        String code = RandomUtil.randomNumbers(codeLength);

        // 存入 Redis
        String codeKey = CODE_KEY_PREFIX + email;
        RBucket<String> codeBucket = redissonClient.getBucket(codeKey);
        codeBucket.set(code, codeExpire, TimeUnit.SECONDS);

        // 设置发送频率限制
        limitBucket.set(1, codeInterval, TimeUnit.SECONDS);

        // 发送邮件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email);
            message.setSubject(getSubject(type));
            message.setText(getContent(code, type));
            mailSender.send(message);
            log.info("验证码发送成功: email={}", email);
        } catch (Exception e) {
            log.error("验证码发送失败: email={}, error={}", email, e.getMessage());
            // 删除已存储的验证码
            codeBucket.delete();
            limitBucket.delete();
            throw new BusinessException("验证码发送失败，请稍后再试");
        }
    }

    @Override
    public boolean verifyCode(String email, String code) {
        String codeKey = CODE_KEY_PREFIX + email;
        RBucket<String> codeBucket = redissonClient.getBucket(codeKey);

        if (!codeBucket.isExists()) {
            return false;
        }

        String storedCode = codeBucket.get();
        return storedCode.equals(code);
    }

    @Override
    public void deleteCode(String email) {
        String codeKey = CODE_KEY_PREFIX + email;
        redissonClient.getBucket(codeKey).delete();
    }

    /**
     * 获取邮件主题
     */
    private String getSubject(String type) {
        return switch (type) {
            case "register" -> "【智学平台】注册验证码";
            case "reset" -> "【智学平台】密码重置验证码";
            default -> "【智学平台】验证码";
        };
    }

    /**
     * 获取邮件内容
     */
    private String getContent(String code, String type) {
        String action = switch (type) {
            case "register" -> "注册账号";
            case "reset" -> "重置密码";
            default -> "验证";
        };
        return String.format("您正在%s，验证码为：%s，有效期%d分钟，请勿泄露给他人。", action, code, codeExpire / 60);
    }
}
