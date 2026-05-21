package com.bucher.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bucher.common.enums.ResultCodeEnum;
import com.bucher.common.exception.BusinessException;
import com.bucher.user.dto.*;
import com.bucher.user.entity.AdminClass;
import com.bucher.user.entity.Department;
import com.bucher.user.entity.User;
import com.bucher.user.enums.UserRoleEnum;
import com.bucher.user.mapper.AdminClassMapper;
import com.bucher.user.mapper.DepartmentMapper;
import com.bucher.user.mapper.UserMapper;
import com.bucher.user.service.AuthService;
import com.bucher.user.service.EmailService;
import com.bucher.user.util.JwtUtil;
import com.bucher.user.vo.LoginVO;
import com.bucher.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final AdminClassMapper adminClassMapper;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RedissonClient redissonClient;

    private static final String TOKEN_KEY_PREFIX = "user:token:";

    @Override
    public LoginVO login(LoginDTO dto) {
        // 判断账号类型：包含 @ 则为邮箱，否则为学号/工号
        String account = dto.getAccount();
        User user;
        if (account.contains("@")) {
            // 邮箱登录
            user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, account)
            );
        } else {
            // 学号/工号登录
            user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getUserNo, account)
            );
        }

        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }

        // 校验状态
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCodeEnum.USER_DISABLED);
        }

        // 校验密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.USER_PASSWORD_ERROR);
        }

        // 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUserNo(), user.getRole());

        // 存储 Token 到 Redis（支持踢出登录）
        String tokenKey = TOKEN_KEY_PREFIX + user.getId();
        RBucket<String> tokenBucket = redissonClient.getBucket(tokenKey);
        tokenBucket.set(token, 24, TimeUnit.HOURS);

        // 构建响应
        UserRoleEnum roleEnum = UserRoleEnum.getByCode(user.getRole());

        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .userNo(user.getUserNo())
                .realName(user.getRealName())
                .role(user.getRole())
                .roleName(roleEnum != null ? roleEnum.getDesc() : "")
                .avatarUrl(user.getAvatarUrl())
                .email(user.getEmail())
                .pwdReset(user.getPwdReset())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto) {
        // 校验邮箱验证码
        if (!emailService.verifyCode(dto.getEmail(), dto.getEmailCode())) {
            throw new BusinessException("验证码错误或已过期");
        }

        // 检查邮箱是否已被使用
        User existEmail = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail())
        );
        if (existEmail != null) {
            throw new BusinessException("该邮箱已被注册");
        }

        // 创建用户（自主注册）
        User user = new User();
        user.setId(System.currentTimeMillis()); // 使用时间戳作为临时ID，MyBatis-Plus 会重新生成
        user.setUserNo(null); // 自主注册用户没有学号/工号
        user.setRealName(dto.getRealName());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setRole(UserRoleEnum.STUDENT.getCode()); // 默认为学生
        user.setSource(2); // 自主注册
        user.setStatus(1); // 直接启用

        userMapper.insert(user);

        // 删除验证码
        emailService.deleteCode(dto.getEmail());

        log.info("用户自主注册成功: email={}", dto.getEmail());
    }

    @Override
    public void sendCode(SendCodeDTO dto) {
        // 注册时检查邮箱是否已被使用
        if ("register".equals(dto.getType())) {
            User existUser = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail())
            );
            if (existUser != null) {
                throw new BusinessException("该邮箱已被注册");
            }
        }

        // 重置密码时检查邮箱是否存在
        if ("reset".equals(dto.getType())) {
            User existUser = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail())
            );
            if (existUser == null) {
                throw new BusinessException("该邮箱未注册");
            }
        }

        emailService.sendVerificationCode(dto.getEmail(), dto.getType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordDTO dto) {
        // 校验验证码
        if (!emailService.verifyCode(dto.getEmail(), dto.getEmailCode())) {
            throw new BusinessException("验证码错误或已过期");
        }

        // 查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail())
        );
        if (user == null) {
            throw new BusinessException("该邮箱未注册");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);

        // 删除验证码
        emailService.deleteCode(dto.getEmail());

        // 删除 Redis 中的 Token（强制重新登录）
        String tokenKey = TOKEN_KEY_PREFIX + user.getId();
        redissonClient.getBucket(tokenKey).delete();

        log.info("用户密码重置成功: userId={}", user.getId());
    }

    @Override
    public void logout(Long userId) {
        String tokenKey = TOKEN_KEY_PREFIX + userId;
        redissonClient.getBucket(tokenKey).delete();
        log.info("用户登出成功: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setPwdReset(0);
        userMapper.updateById(user);
        log.info("用户强制修改密码成功: userId={}", userId);
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }

        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUserNo(user.getUserNo());
        vo.setRealName(user.getRealName());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRole(user.getRole());
        vo.setDeptId(user.getDeptId());
        vo.setAdminClassId(user.getAdminClassId());
        vo.setSource(user.getSource());
        vo.setAuditStatus(user.getAuditStatus());
        vo.setStatus(user.getStatus());
        vo.setPwdReset(user.getPwdReset());
        vo.setCreateTime(user.getCreateTime());

        UserRoleEnum roleEnum = UserRoleEnum.getByCode(user.getRole());
        vo.setRoleName(roleEnum != null ? roleEnum.getDesc() : "");

        if (user.getDeptId() != null) {
            Department dept = departmentMapper.selectById(user.getDeptId());
            vo.setDeptName(dept != null ? dept.getName() : null);
        }

        if (user.getAdminClassId() != null) {
            AdminClass adminClass = adminClassMapper.selectById(user.getAdminClassId());
            vo.setAdminClassName(adminClass != null ? adminClass.getName() : null);
        }

        return vo;
    }
}
