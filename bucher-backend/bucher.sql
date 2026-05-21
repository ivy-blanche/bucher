-- =====================================================
-- 智学平台（ZhiXue Platform）数据库建表语句
-- 适用数据库：MySQL 8.0+
-- =====================================================

-- ----------------------------
-- 表结构变更记录
-- ----------------------------

-- 2026-05-19: course 表添加 course_code 字段（课程码）
ALTER TABLE `course`
    ADD COLUMN `course_code` VARCHAR(10) NOT NULL COMMENT '课程码（6位字母数字，全局唯一）' AFTER `cover_url`,
    ADD UNIQUE INDEX `uk_course_code` (`course_code`);

-- 2026-05-20: course_code 改为可空（新创建课程不再生成课程码，改为班级码）
ALTER TABLE `course`
    MODIFY COLUMN `course_code` VARCHAR(10) NULL COMMENT '课程码（6位字母数字，已废弃，使用班级码代替）';

-- 2026-05-19: 新建课程选课记录表
CREATE TABLE IF NOT EXISTS `course_enrollment` (
    `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
    `course_id` BIGINT NOT NULL COMMENT '课程ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态：1=已选课，0=退选',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_course_id` (`course_id`),
    INDEX `idx_student_id` (`student_id`),
    UNIQUE INDEX `uk_course_student` (`course_id`, `student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程选课记录表';

-- 2026-05-19: 新建题库分组表
CREATE TABLE IF NOT EXISTS `question_group` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `teacher_id` BIGINT NOT NULL COMMENT '所属教师ID',
    `name` VARCHAR(100) NOT NULL COMMENT '分组名称',
    `description` VARCHAR(500) NULL COMMENT '分组描述',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题库分组';

-- 2026-05-19: question_group 表添加题量字段
ALTER TABLE `question_group`
    ADD COLUMN `question_count` INT NOT NULL DEFAULT 0 COMMENT '题量（该分组下的题目数量）' AFTER `sort_order`;

-- 2026-05-19: 新建题目表
CREATE TABLE IF NOT EXISTS `question` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `teacher_id` BIGINT NOT NULL COMMENT '所属教师ID',
    `group_id` BIGINT NULL COMMENT '所属分组ID',
    `type` TINYINT NOT NULL COMMENT '题目类型: 1=单选, 2=多选, 3=填空, 4=简答',
    `content` TEXT NOT NULL COMMENT '题目内容(HTML格式)',
    `answer` TEXT NULL COMMENT '正确答案: 单选存"A", 多选存"ABC", 填空存JSON数组, 简答存参考文字',
    `analysis` TEXT NULL COMMENT '题目解析',
    `difficulty` TINYINT NOT NULL DEFAULT 1 COMMENT '难度: 1=简单, 2=中等, 3=困难',
    `score` INT NOT NULL DEFAULT 0 COMMENT '默认分值',
    `allow_attachment` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '简答题是否允许上传附件',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1=启用, 0=停用',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_teacher_id` (`teacher_id`),
    INDEX `idx_group_id` (`group_id`),
    INDEX `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目';

-- 2026-05-19: 新建题目选项表（单选/多选）
CREATE TABLE IF NOT EXISTS `question_option` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `question_id` BIGINT NOT NULL COMMENT '所属题目ID',
    `label` VARCHAR(10) NOT NULL COMMENT '选项标签(A/B/C/D等)',
    `content` TEXT NOT NULL COMMENT '选项内容',
    `is_correct` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为正确答案',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目选项';

-- 2026-05-19: 新建考试表
CREATE TABLE IF NOT EXISTS `exam` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `course_id` BIGINT NOT NULL COMMENT '所属课程ID',
    `teacher_id` BIGINT NOT NULL COMMENT '创建教师ID',
    `course_class_id` BIGINT NULL COMMENT '所属教学班ID(NULL=全部班级)',
    `title` VARCHAR(200) NOT NULL COMMENT '考试标题',
    `description` TEXT NULL COMMENT '考试说明',
    `compose_type` TINYINT NOT NULL COMMENT '组卷方式: 1=从题库组选题, 2=手动组卷',
    `group_id` BIGINT NULL COMMENT '来源题库分组ID(compose_type=1时)',
    `duration` INT NOT NULL COMMENT '考试时长(分钟)',
    `total_score` INT NOT NULL COMMENT '总分',
    `pass_score` INT NULL COMMENT '及格分',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0=未发布, 1=进行中, 2=已结束',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_course_id` (`course_id`),
    INDEX `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考试';

-- 2026-05-19: 新建考试题目关联表
CREATE TABLE IF NOT EXISTS `exam_question` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `exam_id` BIGINT NOT NULL COMMENT '所属考试ID',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `score` INT NOT NULL COMMENT '本题分值',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_exam_id` (`exam_id`),
    INDEX `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考试题目关联';

-- 2026-05-19: 新建作业表
CREATE TABLE IF NOT EXISTS `homework` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `course_id` BIGINT NOT NULL COMMENT '所属课程ID',
    `teacher_id` BIGINT NOT NULL COMMENT '创建教师ID',
    `course_class_id` BIGINT NULL COMMENT '所属教学班ID(NULL=全部班级)',
    `title` VARCHAR(200) NOT NULL COMMENT '作业标题',
    `description` TEXT NULL COMMENT '作业说明',
    `compose_type` TINYINT NOT NULL COMMENT '组卷方式: 1=从题库组选题, 2=手动组卷',
    `group_id` BIGINT NULL COMMENT '来源题库分组ID(compose_type=1时)',
    `total_score` INT NOT NULL COMMENT '总分',
    `deadline` DATETIME NOT NULL COMMENT '截止时间',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0=未发布, 1=进行中, 2=已截止',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_course_id` (`course_id`),
    INDEX `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业';

-- 2026-05-19: 新建作业题目关联表
CREATE TABLE IF NOT EXISTS `homework_question` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `homework_id` BIGINT NOT NULL COMMENT '所属作业ID',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `score` INT NOT NULL COMMENT '本题分值',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_homework_id` (`homework_id`),
    INDEX `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业题目关联';

-- 2026-05-20: 新建作业教学班关联表（支持多教学班发布）
CREATE TABLE IF NOT EXISTS `homework_class` (
    `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
    `homework_id` BIGINT NOT NULL COMMENT '作业ID',
    `course_class_id` BIGINT NOT NULL COMMENT '教学班ID',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_homework_id` (`homework_id`),
    INDEX `idx_course_class_id` (`course_class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业教学班关联';

-- 2026-05-19: homework 表新增课程名称和批改状态字段
ALTER TABLE `homework`
    ADD COLUMN `course_name` VARCHAR(200) NULL COMMENT '课程名称' AFTER `course_id`,
    ADD COLUMN `grading_status` TINYINT NOT NULL DEFAULT 0 COMMENT '批改状态: 0=无需批改/全部已批, 1=待批改' AFTER `status`;

-- 2026-05-19: 新建作业提交记录表
CREATE TABLE IF NOT EXISTS `homework_submission` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `homework_id` BIGINT NOT NULL COMMENT '作业ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `score` INT NULL COMMENT '得分',
    `teacher_comment` TEXT NULL COMMENT '教师评语',
    `submit_time` DATETIME NULL COMMENT '提交时间',
    `grade_time` DATETIME NULL COMMENT '批改时间',
    `grade_status` TINYINT NOT NULL DEFAULT 0 COMMENT '批改状态: 0=未批改, 1=已批改',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_homework_id` (`homework_id`),
    INDEX `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业提交记录';

-- 2026-05-20: homework_submission 表添加提交状态字段
ALTER TABLE `homework_submission`
    ADD COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '提交状态: 0=未提交, 1=已提交, 2=已批改' AFTER `grade_status`;

-- 2026-05-20: 新建课程资料表（教师上传的文件，学生可查看/下载）
CREATE TABLE IF NOT EXISTS `course_material` (
    `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
    `course_id` BIGINT NOT NULL COMMENT '所属课程ID',
    `teacher_id` BIGINT NOT NULL COMMENT '上传教师ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名（含扩展名）',
    `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
    `file_type` VARCHAR(50) NOT NULL COMMENT 'MIME类型（如 application/pdf, video/mp4）',
    `file_ext` VARCHAR(20) NOT NULL COMMENT '文件扩展名（如 .pdf, .mp4, .jpg）',
    `object_name` VARCHAR(255) NOT NULL COMMENT 'MinIO对象名（含路径）',
    `bucket_name` VARCHAR(50) NOT NULL DEFAULT 'course-bucket' COMMENT 'MinIO桶名',
    `duration` INT NULL COMMENT '视频时长（秒，仅视频文件有值）',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1=正常, 0=已删除',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_course_id` (`course_id`),
    INDEX `idx_teacher_id` (`teacher_id`),
    INDEX `idx_file_type` (`file_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程资料';

-- 2026-05-20: 新建通知表
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT NOT NULL COMMENT '雪花ID',
    `user_id` BIGINT NOT NULL COMMENT '接收人ID（学生）',
    `type` TINYINT NOT NULL COMMENT '通知类型：1-作业通知 2-考试通知',
    `related_id` BIGINT NULL DEFAULT NULL COMMENT '关联业务ID（homework_id/exam_id）',
    `teacher_name` VARCHAR(100) NOT NULL COMMENT '教师姓名',
    `course_name` VARCHAR(200) NOT NULL COMMENT '课程名称',
    `biz_title` VARCHAR(200) NOT NULL COMMENT '作业/考试标题',
    `deadline` DATETIME NULL DEFAULT NULL COMMENT '作业截止时间/考试结束时间',
    `start_time` DATETIME NULL DEFAULT NULL COMMENT '考试开始时间（仅考试类型）',
    `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读 1-已读',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- 2026-05-20: 新建作业作答记录表（学生每道题的答案）
CREATE TABLE IF NOT EXISTS `homework_answer` (
    `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
    `homework_id` BIGINT NOT NULL COMMENT '作业ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `answer` TEXT NULL COMMENT '学生答案（选择题存"A"/"ABC"格式，填空题存JSON数组，简答存文本）',
    `score` INT NULL COMMENT '本题得分（客观题自动评分）',
    `is_correct` TINYINT(1) NULL COMMENT '客观题是否正确：0=错误, 1=正确',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_homework_id` (`homework_id`),
    INDEX `idx_student_id` (`student_id`),
    INDEX `idx_question_id` (`question_id`),
    UNIQUE INDEX `uk_homework_student_question` (`homework_id`, `student_id`, `question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业作答记录';

-- 2026-05-21: question 表 type 字段新增编程题类型
ALTER TABLE `question`
    MODIFY COLUMN `type` TINYINT NOT NULL COMMENT '题目类型: 1=单选, 2=多选, 3=填空, 4=简答, 5=编程';

-- 2026-05-21: 新建编程题配置表
CREATE TABLE IF NOT EXISTS `question_programming` (
    `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
    `question_id` BIGINT NOT NULL COMMENT '关联题目ID',
    `template_code` TEXT NULL COMMENT '初始代码模板（学生在此代码基础上编写）',
    `judge0_language_id` INT NOT NULL COMMENT 'Judge0语言ID（例如 71=Python3, 62=Java, 50=C, 54=C++）',
    `time_limit` INT NOT NULL DEFAULT 3000 COMMENT '时间限制（毫秒）',
    `memory_limit` INT NOT NULL DEFAULT 256000 COMMENT '内存限制（KB）',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='编程题配置';

-- 2026-05-21: 新建编程题测试用例表
CREATE TABLE IF NOT EXISTS `question_test_case` (
    `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
    `question_id` BIGINT NOT NULL COMMENT '关联题目ID',
    `input` TEXT NULL COMMENT '测试输入',
    `expected_output` TEXT NOT NULL COMMENT '期望输出',
    `is_sample` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为样例（1=学生可见, 0=仅后台判分用）',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='编程题测试用例';

-- 2026-05-21: 新建编程题判题结果表（作业场景）
CREATE TABLE IF NOT EXISTS `homework_answer_judge` (
    `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
    `answer_id` BIGINT NOT NULL COMMENT '关联作答记录ID（homework_answer.id）',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `judge0_token` VARCHAR(100) NULL COMMENT 'Judge0 提交令牌',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '判题状态: 0=待判题, 1=队列中, 2=判题中, 3=已完成, 4=失败',
    `result` TEXT NULL COMMENT '判题结果JSON: {passed:N, total:N, time:ms, memory:kb, testCases:[{id, passed, output, time, memory}]}',
    `compile_output` TEXT NULL COMMENT '编译输出（编译错误时）',
    `score` INT NULL COMMENT '自动评分（通过的测试用例比例×分值）',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_answer_id` (`answer_id`),
    INDEX `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='编程题判题结果';

-- 2026-05-21: 扩展 judge0_token 字段以支持批量提交（多个 token 存为 JSON 数组）
ALTER TABLE `homework_answer_judge`
    MODIFY COLUMN `judge0_token` TEXT NULL COMMENT 'Judge0 提交令牌（单次时存UUID，批量提交时为JSON数组）';

-- 2026-05-21: 更新 homework_answer_judge.status COMMENT 与实际状态值对齐
ALTER TABLE `homework_answer_judge`
    MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '判题状态: 0=待判题, 1=判题中, 2=已完成, 3=失败';
