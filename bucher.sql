-- =====================================================
-- 智学平台 - 数据库建表语句
-- =====================================================

-- ---------------------------------------------------
-- user-service 数据库
-- ---------------------------------------------------

CREATE DATABASE IF NOT EXISTS `bucher_user` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `bucher_user`;

-- ---------------------------------------------------
-- 1. department（院系/部门表）
-- ---------------------------------------------------
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department` (
    `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
    `name` VARCHAR(100) NOT NULL COMMENT '院系/部门名称',
    `type` TINYINT NOT NULL DEFAULT 1 COMMENT '类型：1-学校院系，2-企业部门',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`),
    KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='院系/部门表';

-- ---------------------------------------------------
-- 2. admin_class（行政班级表）
-- ---------------------------------------------------
DROP TABLE IF EXISTS `admin_class`;
CREATE TABLE `admin_class` (
    `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
    `name` VARCHAR(100) NOT NULL COMMENT '行政班级名称',
    `dept_id` BIGINT NOT NULL COMMENT '所属院系/部门ID',
    `year` INT NOT NULL COMMENT '入学年份',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_year` (`year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行政班级表';

-- ---------------------------------------------------
-- 3. course（课程表）
-- ---------------------------------------------------
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
    `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
    `name` VARCHAR(200) NOT NULL COMMENT '课程名称',
    `teacher_id` BIGINT NOT NULL COMMENT '创建课程的教师ID',
    `semester` VARCHAR(50) NOT NULL COMMENT '开设学期（如 2025-2026-1）',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '课程描述',
    `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图片URL（MinIO路径）',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-进行中，0-已结课',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_teacher_id` (`teacher_id`),
    KEY `idx_status` (`status`),
    KEY `idx_semester` (`semester`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程表';

-- ---------------------------------------------------
-- 4. course_class（课程班级表）
-- ---------------------------------------------------
DROP TABLE IF EXISTS `course_class`;
CREATE TABLE `course_class` (
    `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
    `course_id` BIGINT NOT NULL COMMENT '所属课程ID',
    `name` VARCHAR(100) NOT NULL COMMENT '班级名称',
    `teacher_id` BIGINT NOT NULL COMMENT '教师ID',
    `invite_code` VARCHAR(20) DEFAULT NULL COMMENT '邀请码（唯一）',
    `invite_expire_time` DATETIME DEFAULT NULL COMMENT '邀请码过期时间（NULL表示永不过期）',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-已关闭',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_invite_code` (`invite_code`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程班级表';

-- ---------------------------------------------------
-- 5. course_class_member（课程班级成员表）
-- ---------------------------------------------------
DROP TABLE IF EXISTS `course_class_member`;
CREATE TABLE `course_class_member` (
    `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
    `course_class_id` BIGINT NOT NULL COMMENT '课程班级ID',
    `student_id` BIGINT NOT NULL COMMENT '学生用户ID',
    `join_type` TINYINT NOT NULL DEFAULT 1 COMMENT '加入方式：1-邀请码加入，2-教师添加',
    `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-已移除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_course_class_id` (`course_class_id`),
    KEY `idx_student_id` (`student_id`),
    UNIQUE KEY `uk_class_student` (`course_class_id`, `student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程班级成员表';

-- ---------------------------------------------------
-- 6. user（用户表）
-- ---------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
    `user_no` VARCHAR(50) DEFAULT NULL COMMENT '工号/学号（管理员添加的用户必填，自主注册用户为空）',
    `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `email` VARCHAR(100) NOT NULL COMMENT '邮箱（自主注册用户的登录账号）',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像URL（MinIO路径）',
    `role` TINYINT NOT NULL DEFAULT 3 COMMENT '角色：1-管理员，2-教师，3-学生',
    `dept_id` BIGINT DEFAULT NULL COMMENT '所属院系/部门ID',
    `admin_class_id` BIGINT DEFAULT NULL COMMENT '所属行政班级ID（仅学生有值）',
    `source` TINYINT NOT NULL DEFAULT 1 COMMENT '用户来源：1-管理员添加，2-自主注册',
    `audit_status` TINYINT DEFAULT NULL COMMENT '教师申请审核状态：0-待审核，1-通过，2-拒绝，NULL-未申请',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_no` (`user_no`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_admin_class_id` (`admin_class_id`),
    KEY `idx_role` (`role`),
    KEY `idx_status` (`status`),
    KEY `idx_source` (`source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ---------------------------------------------------
-- ALTER: 用户表新增 pwd_reset 字段
-- ---------------------------------------------------
ALTER TABLE `user`
    ADD COLUMN `pwd_reset` TINYINT(1) NOT NULL DEFAULT 0
    COMMENT '密码是否已被管理员重置：0-否，1-需强制修改密码'
    AFTER `status`;

-- ---------------------------------------------------
-- ALTER: department 表新增 dept_code 业务编号字段
-- ---------------------------------------------------
ALTER TABLE `department`
    ADD COLUMN `dept_code` VARCHAR(20) DEFAULT NULL
    COMMENT '业务编号（如 13、01，管理员手动填写）'
    AFTER `name`;

-- ---------------------------------------------------
-- ALTER: 导入用户不需要邮箱
-- ---------------------------------------------------
ALTER TABLE `user`
    MODIFY COLUMN `email` VARCHAR(100) DEFAULT NULL
    COMMENT '邮箱（自主注册用户的登录账号，导入用户为空）';
