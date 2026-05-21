# 智学平台 API 文档访问指南

本项目使用 Knife4j 作为 API 文档工具，基于 OpenAPI 3.0 规范。

---

## 服务端口规划

| 服务名称 | 端口 | 服务名 |
|---------|------|--------|
| gateway-service | 8080 | 网关服务 |
| user-service | 8101 | 用户服务 |
| course-service | 8102 | 课程服务 |
| question-service | 8103 | 题库服务 |
| exam-service | 8104 | 考试服务 |
| homework-service | 8105 | 作业服务 |
| grade-service | 8106 | 成绩服务 |
| message-service | 8107 | 消息服务 |
| ai-service | 8108 | AI服务 |

---

## 访问方式

### 方式一：直接访问各服务（推荐开发调试）

启动对应服务后，直接访问该服务的文档地址：

| 服务 | Knife4j 文档地址 |
|------|-----------------|
| user-service | http://localhost:8101/doc.html |
| course-service | http://localhost:8102/doc.html |
| question-service | http://localhost:8103/doc.html |
| exam-service | http://localhost:8104/doc.html |
| homework-service | http://localhost:8105/doc.html |
| grade-service | http://localhost:8106/doc.html |
| message-service | http://localhost:8107/doc.html |
| ai-service | http://localhost:8108/doc.html |

### 方式二：通过网关访问（生产环境）

网关统一入口：http://localhost:8080

通过网关访问各服务的 API 路径：

| 服务 | API 前缀 |
|------|---------|
| user-service | `/api/user/**` |
| course-service | `/api/course/**` |
| question-service | `/api/question/**` |
| exam-service | `/api/exam/**` |
| homework-service | `/api/homework/**` |
| grade-service | `/api/grade/**` |
| message-service | `/api/message/**` |
| ai-service | `/api/ai/** |

---

## 使用说明

### 1. 启动服务

```bash
# 启动单个服务
cd bucher-backend/user-service
mvn spring-boot:run

# 或在 IDE 中运行对应的 Application 类
```

### 2. 访问文档

浏览器打开对应服务的文档地址，例如用户服务：
```
http://localhost:8101/doc.html
```

### 3. 调试接口

1. 在文档页面找到需要测试的接口
2. 点击「调试」按钮
3. 填写参数
4. 点击「发送」查看响应

### 4. 认证接口

需要登录的接口，在请求头中添加：
```
Authorization: Bearer {your_token}
```

或通过网关时，网关会自动验证 JWT 并传递用户信息到下游服务。

---

## 当前已实现的接口

### user-service（端口 8101）

| 接口 | 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|------|
| 登录 | POST | /auth/login | 用户登录（支持邮箱或学号/工号）| 否 |
| 注册 | POST | /auth/register | 自主注册（邮箱+验证码+姓名+密码）| 否 |
| 发送验证码 | POST | /auth/send-code | 发送邮箱验证码 | 否 |
| 重置密码 | POST | /auth/reset-password | 忘记密码重置 | 否 |
| 登出 | POST | /auth/logout | 用户登出 | 是 |
| 当前用户 | GET | /auth/me | 获取当前登录用户信息 | 是 |

> 注：自主注册用户使用邮箱登录，管理员添加的用户使用学号/工号登录。

---

## 注意事项

1. **白名单接口**：登录、注册、发送验证码等接口无需认证
2. **JWT 配置**：确保 `application-local.yml` 中的 JWT 密钥与 gateway-service 一致
3. **跨域问题**：本地开发时已配置 CORS，可直接在前端调用

---

## 扩展阅读

- [Knife4j 官方文档](https://doc.xiaominfo.com/)
- [SpringDoc OpenAPI](https://springdoc.org/)
