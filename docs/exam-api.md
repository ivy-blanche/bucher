# 考试模块（exam-service）接口文档

## 基础信息
- 服务名：`exam-service`
- 端口：`8104`
- 基础路径：`/exam`
- 通用请求头：
  - `X-User-Id`: 用户ID
  - `X-User-Role`: 角色（1=学生, 2=教师）

---

## 一、教师端接口

### 1.1 查询教师考试列表

**URL:** `GET /exam/teacher/list`

**请求头：**
```json
X-User-Id: 123456789
X-User-Role: 2
```

**请求参数（Query）：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| filterMode | String | 否 | 筛选模式: 空=全部, 1=进行中, 2=已结束 |
| pageNum | Integer | 否 | 页码（默认1） |
| pageSize | Integer | 否 | 每页条数（默认10） |

**响应示例：**
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "records": [
      {
        "id": 1812345678901234561,
        "title": "期中考试",
        "courseName": "计算机网络",
        "startTime": "2026-06-01T09:00:00",
        "endTime": "2026-06-01T11:00:00",
        "duration": 120,
        "totalScore": 100,
        "status": 1,
        "submitCount": 25,
        "totalCount": 30
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10
  },
  "timestamp": 1717654321000
}
```

---

### 1.2 查询未发布考试列表

**URL:** `GET /exam/teacher/unpublished-list`

**请求头：**
```json
X-User-Id: 123456789
X-User-Role: 2
```

**请求参数（Query）：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认1） |
| pageSize | Integer | 否 | 每页条数（默认10） |

**响应示例：**
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "records": [
      {
        "id": 1812345678901234561,
        "title": "期中考试",
        "courseName": "计算机网络"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10
  },
  "timestamp": 1717654321000
}
```

---

### 1.3 保存考试草稿

**URL:** `POST /exam/teacher/draft`

**请求头：**
```json
X-User-Id: 123456789
X-User-Role: 2
Content-Type: application/json
```

**请求体：**
```json
{
  "title": "期中考试",
  "courseId": 1812345678901234561,
  "sourceBankId": 1812345678901234562,
  "questions": [
    {
      "questionId": 1812345678901234563,
      "score": 10
    },
    {
      "questionId": 1812345678901234564,
      "score": 20
    }
  ]
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "成功",
  "data": null,
  "timestamp": 1717654321000
}
```

---

### 1.4 发布考试

**URL:** `POST /exam/teacher/publish`

**请求头：**
```json
X-User-Id: 123456789
X-User-Role: 2
Content-Type: application/json
```

**请求体：**
```json
{
  "title": "期中考试",
  "courseId": 1812345678901234561,
  "sourceBankId": 1812345678901234562,
  "questions": [
    {
      "questionId": 1812345678901234563,
      "score": 10
    },
    {
      "questionId": 1812345678901234564,
      "score": 20
    }
  ],
  "classIds": [1812345678901234565, 1812345678901234566],
  "startTime": "2026-06-01T09:00:00",
  "duration": 120,
  "earlySubmitMinutes": 30,
  "lateBanMinutes": 15,
  "autoSubmit": 1,
  "passScore": 60,
  "description": "本次考试涵盖前六章内容"
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 是 | 考试标题 |
| courseId | Long | 是 | 课程ID |
| sourceBankId | Long | 是 | 来源题库分组ID |
| questions | Array | 是 | 题目列表（questionId + score） |
| classIds | Array[Long] | 是 | 发布教学班ID列表 |
| startTime | DateTime | 是 | 开始时间 |
| duration | Integer | 是 | 考试时长（分钟） |
| earlySubmitMinutes | Integer | 是 | 最早交卷时间，开考后多少分钟可以交卷 |
| lateBanMinutes | Integer | 否 | 最晚入场时间，开考后多少分钟禁止进入（默认15） |
| autoSubmit | Integer | 否 | 超时是否自动提交（1=是, 0=否，默认1） |
| passScore | Integer | 否 | 及格分 |
| description | String | 否 | 考试说明 |

**响应示例：**
```json
{
  "code": 200,
  "message": "成功",
  "data": null,
  "timestamp": 1717654321000
}
```

---

### 1.5 超时自动提交

**URL:** `POST /exam/teacher/auto-submit`

**请求头：**
```json
X-User-Id: 123456789
X-User-Role: 2
```

**响应示例：**
```json
{
  "code": 200,
  "message": "成功",
  "data": null,
  "timestamp": 1717654321000
}
```

---

## 二、学生端接口

### 2.1 查询学生端考试列表

**URL:** `GET /exam/student/list`

**请求头：**
```json
X-User-Id: 987654321
X-User-Role: 1
```

**请求参数（Query）：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| courseId | Long | 是 | 课程ID |

**响应示例：**
```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "id": 1812345678901234561,
      "title": "期中考试",
      "startTime": "2026-06-01T09:00:00",
      "endTime": "2026-06-01T11:00:00",
      "duration": 120,
      "status": 1
    }
  ],
  "timestamp": 1717654321000
}
```

**学生端 status 说明：**
| 值 | 含义 |
|----|------|
| 0 | 未开始 |
| 1 | 进行中（未提交） |
| 2 | 已提交 |
| 3 | 已结束（超时未提交） |

---

### 2.2 获取做题页面数据

**URL:** `GET /exam/student/{examId}/do`

**请求头：**
```json
X-User-Id: 987654321
X-User-Role: 1
```

**路径参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| examId | Long | 考试ID |

**响应示例：**
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "examId": 1812345678901234561,
    "title": "期中考试",
    "description": "本次考试涵盖前六章内容",
    "courseName": "计算机网络",
    "totalScore": 100,
    "duration": 120,
    "earlySubmitMinutes": 30,
    "startTime": "2026-06-01T09:00:00",
    "endTime": "2026-06-01T11:00:00",
    "status": 1,
    "submitted": false,
    "remainingSeconds": 7200,
    "questions": [
      {
        "questionId": 1812345678901234563,
        "questionType": "single",
        "content": "<p>TCP/IP协议中，IP协议位于哪一层？</p>",
        "score": 10,
        "sortOrder": 0,
        "answer": null,
        "options": [
          {"label": "A", "content": "应用层"},
          {"label": "B", "content": "传输层"},
          {"label": "C", "content": "网络层"},
          {"label": "D", "content": "数据链路层"}
        ]
      },
      {
        "questionId": 1812345678901234564,
        "questionType": "fill",
        "content": "<p>HTTP默认端口号是____。</p>",
        "score": 5,
        "sortOrder": 1,
        "answer": null
      }
    ]
  },
  "timestamp": 1717654321000
}
```

**安全校验：**
- 考试未开始：返回错误码 `3002`
- 考试已结束：返回错误码 `3003`
- 已过禁止入场时间（lateBanMinutes）：返回 `"考试已开始XX分钟，禁止进入"`
- 不在发布班级内：返回 `"您没有权限参加该考试"`

---

### 2.3 暂存单题答案

**URL:** `POST /exam/student/answer/save`

**请求头：**
```json
X-User-Id: 987654321
X-User-Role: 1
Content-Type: application/json
```

**请求体：**
```json
{
  "examId": 1812345678901234561,
  "questionId": 1812345678901234563,
  "answer": "C"
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "成功",
  "data": null,
  "timestamp": 1717654321000
}
```

---

### 2.4 提交全部答案

**URL:** `POST /exam/student/submit`

**请求头：**
```json
X-User-Id: 987654321
X-User-Role: 1
Content-Type: application/json
```

**请求体：**
```json
{
  "examId": 1812345678901234561
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "成功",
  "data": null,
  "timestamp": 1717654321000
}
```

**业务校验：**
- 考试不存在：`3001`
- 考试未开始：`3002`
- 考试已结束：`3003`
- 早于最早交卷时间：`"考试开始XX分钟后才能交卷"`
- 已提交：`"考试已提交，请勿重复提交"`
- 无作答记录：`"暂无作答记录，请先作答后再提交"`

**自动评分逻辑：**
- 单选题（type=1）：精确匹配正确答案，得满分或0分
- 多选题（type=2）：排序后比较，完全正确得满分，否则0分
- 填空/简答（type=3/4）：需人工批改
- 编程题（type=5）：由判题系统异步评分

---

## 三、数据库表结构

### exam 表（新增字段）

```sql
ALTER TABLE `exam`
    ADD COLUMN `course_name` VARCHAR(200) NULL COMMENT '课程名称',
    ADD COLUMN `grading_status` TINYINT NOT NULL DEFAULT 0 COMMENT '批改状态',
    ADD COLUMN `early_submit_minutes` INT NOT NULL DEFAULT 0 COMMENT '最早交卷时间(分钟)',
    ADD COLUMN `late_ban_minutes` INT NOT NULL DEFAULT 15 COMMENT '最晚入场时间(分钟)',
    ADD COLUMN `auto_submit` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否自动提交';
```

### exam_class 表（新建）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| exam_id | BIGINT | 考试ID |
| course_class_id | BIGINT | 教学班ID |
| create_time | DATETIME | 创建时间 |

### exam_submission 表（新建）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| exam_id | BIGINT | 考试ID |
| student_id | BIGINT | 学生ID |
| score | INT | 得分 |
| teacher_comment | TEXT | 教师评语 |
| submit_time | DATETIME | 提交时间 |
| grade_time | DATETIME | 批改时间 |
| grade_status | TINYINT | 批改状态 |
| status | TINYINT | 提交状态 |

### exam_answer 表（新建）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| exam_id | BIGINT | 考试ID |
| student_id | BIGINT | 学生ID |
| question_id | BIGINT | 题目ID |
| answer | TEXT | 学生答案 |
| score | INT | 本题得分 |
| is_correct | TINYINT | 是否正确 |

---

## 四、核心功能说明

### 15分钟禁止入场

教师发布考试时设置 `lateBanMinutes`（默认15分钟），开考后超过此时间的学生尝试进入考试将收到错误提示。已提交的学生仍可查看考试页面（只读）。

### 超时自动提交

- 考试 `endTime = startTime + duration`
- 调用 `POST /exam/teacher/auto-submit` 触发自动提交
- 自动查找所有进行中且 `endTime < NOW` 的考试
- 遍历考试中所有未提交的学生，执行强制提交
- 提交后更新考试状态为 `已结束(2)`
- 建议通过定时任务（如 xxl-job 或 @Scheduled）每5分钟调用一次

### 最早交卷时间

考生必须在考试开始后经过 `earlySubmitMinutes` 分钟才能交卷，防止"秒交卷"行为。

### 状态流转

```
未发布(0) --发布--> 进行中(1) --结束/自动提交--> 已结束(2)
```
