# bucher-backend

智学平台后端服务，基于 Spring Cloud 微服务架构构建。

## 技术栈

- JDK 21
- Spring Boot 3.2.5
- Spring Cloud 2023.0.1
- Spring Cloud Alibaba 2023.0.1.0
- MyBatis-Plus 3.5.5
- Nacos 2.5.2（注册中心/配置中心）
- Redis + Redisson
- MySQL 8.0
- RabbitMQ
- MinIO
- Elasticsearch 8.x

## 项目结构

```
bucher-backend/
├── common/                   # 公共模块
│   ├── result/               # 统一响应
│   ├── exception/            # 异常处理
│   ├── enums/                # 枚举类
│   └── util/                 # 工具类
├── gateway-service/          # 网关服务（待创建）
├── user-service/             # 用户服务（待创建）
├── course-service/           # 课程服务（待创建）
├── question-service/         # 题库服务（待创建）
├── exam-service/             # 考试服务（待创建）
├── homework-service/         # 作业服务（待创建）
├── grade-service/            # 成绩服务（待创建）
├── message-service/          # 消息服务（待创建）
├── ai-service/               # AI服务（待创建）
└── api/                      # Feign接口（待创建）
```

## 环境要求

- JDK 21+
- Maven 3.9+
- MySQL 8.0+
- Redis 7.0+
- Nacos 2.5.2+

## 编译项目

```bash
mvn clean install
```

## 配置文件说明

- `application.yml` - 通用配置（可提交）
- `application-local.yml` - 本地开发配置，包含敏感信息（禁止提交）
- `application-prod.yml` - 生产环境配置

## 开发规范

详见项目根目录 CLAUDE.md 文件。
