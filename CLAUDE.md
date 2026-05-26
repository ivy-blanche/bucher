# 智学平台（ZhiXue Platform）— 全局开发约束

## 项目定位

面向学校和企业的在线学习平台 角色：管理员 / 教师 / 学生。

---

## 构建与运行

### 后端（JDK 21 + Maven）

```bash
# 编译整个后端（含 common + 所有微服务）
cd bucher-backend
mvn clean install -DskipTests

# 运行指定服务（需先启动 Nacos）
mvn -pl user-service spring-boot:run -Dspring-boot.run.profiles=local

# 运行测试（单个服务）
mvn -pl user-service test

# 运行单个测试类
mvn -pl user-service test -Dtest=AuthControllerIntegrationTest
```

### 前端（Node.js + Vite 5）

```bash
# 安装依赖
cd bucher-frontend
npm install

# 开发模式（默认 http://localhost:5173，自动代理 /api 到网关）
npm run dev

# 构建生产版本
npm run build

# 代码检查
npm run lint
npm run format
```

### Nacos 启动

```bash
# 确保本地 Nacos 2.5.2+ 已启动
startup.cmd -m standalone
```

各服务端口见 `api.md` 五、服务端口章节。

---

## 架构概览

### 模块组织

后端以 `bucher-backend` 为 Maven 父 POM，各子模块：

| 模块 | 说明 |
|------|------|
| `common` | 公共模块：`Result<T>`、`BusinessException`、全局异常处理、枚举、雪花 ID 工具 |
| `user-service` | 用户服务：认证（登录/注册/JWT）、用户 CRUD、院系、行政班级、Excel 导入 |
| `course-service` | 课程服务：课程管理、课程班级（教学分组）、课程资料/视频（MinIO） |
| `question-service` | 题库服务：题目 CRUD（单选/多选/填空/简答）、题库分组、Excel 导入题目 |
| `exam-service` | 考试服务：考试管理、组卷 |
| `homework-service` | 作业服务：作业发布/提交/附件、RabbitMQ 事件 |
| `message-service` | 消息服务：站内通知、RabbitMQ 消费者 |
| `gateway-service` | 网关：路由转发、JWT 校验（AuthGlobalFilter）、Sentinel 限流 |

### 内部服务调用模式

- 服务间 OpenFeign 调用统一通过 `InternalXxxController` 暴露（如 `InternalUserController`、`InternalCourseController`）
- Feign 接口定义在同模块的 `feign/` 包下（如 `UserFeignClient`）
- 跨服务不做事务兜底，通过 RabbitMQ 保证最终一致性

### 分层约定

```
controller/    → 仅参数校验 + 调用 service（feign/ 存放内部调用接口）
dto/           → 请求/响应数据传输对象
entity/        → 数据库实体（MyBatis-Plus）
enums/         → 枚举类
mapper/        → MyBatis-Plus Mapper 或 XML Mapper
service/       → 接口
service/impl/  → 实现（@Transactional 标注在此层）
vo/            → 视图层响应对象
config/        → 配置类
```

### 包名规范

- 所有服务根包：`com.bucher.{service}`，如 `com.bucher.user`、`com.bucher.course`

------

## 架构

Spring Cloud 微服务，服务列表：

| 服务             | 职责                                             |
| ---------------- | ------------------------------------------------ |
| gateway-service  | 路由、JWT 校验、Sentinel 限流                    |
| user-service     | 用户、角色、院系、行政班级                       |
| course-service   | 课程、课程班级、成员、资料、视频                 |
| question-service | 题库、题目（单选/多选/填空/简答/编程）           |
| exam-service     | 考试、组卷、作答、自动评分                       |
| homework-service | 作业、提交、附件（docx/pdf/zip）                 |
| grade-service    | 阅卷、成绩汇总                                   |
| message-service  | 站内通知，消费 RabbitMQ 消息                     |
| ai-service       | AI 复盘助手，LangChain4j + RAG，每课程独立知识库 |

------

## 技术栈

- 注册/配置：Nacos 2.x
- JDK：21
- 网关：Spring Cloud Gateway
- 服务通信：OpenFeign（同步）+ RabbitMQ（异步通知/评分/向量化）
- 持久层：MySQL + MyBatis-Plus（优先），复杂多表查询用 MyBatis XML
- 缓存：Redis（Redisson）
- 文件：MinIO
- 搜索：Elasticsearch
- AI：LangChain4j 1.x + PGVector（RAG）
- 前端：Vue 3 + Element Plus

------

## 组织结构约定

### 两级部门体系（行政）

- 一级：院系（学校）/ 部门（企业），存于 `department` 表
- 二级：行政班级（学校）/ 小组（企业），存于 `admin_class` 表，有 `dept_id` 外键指向一级
- 教师属于某个一级部门；学生属于某个一级部门 + 某个二级行政班级

### 两套班级体系完全独立，禁止混用

- **行政班级**（`admin_class` 表，在 user-service）：学校分配的自然班，管理员维护
- **课程班级**（`course_class` 表，在 course-service）：教师在课程下创建的教学分组，与行政班级无关
- 变量名、注释、表名中行政班级统一用 `adminClass`，课程班级统一用 `courseClass`

### 用户标识

- 学生登录账号是**学号**，教师登录账号是**工号**，字段名统一为 `user_no`
- `user_no` 全局唯一，由管理员导入时设置，不可修改
- 禁止用 `username` 替代 `user_no`

------

## 持久层约束

- 单表 CRUD、简单条件查询用 MyBatis-Plus（LambdaQueryWrapper / IService）
- 多表关联、分页聚合、复杂动态 SQL 用 MyBatis XML，放 `resources/mapper/` 下
- XML namespace 必须与 Mapper 接口全限定名一致
- 每个微服务独立 database，禁止跨库 join，跨服务数据通过 Feign 获取

------

## 事务约束

- `@Transactional` 标注在 Service 实现类方法上，禁止标注在接口上
- 跨服务操作用 RabbitMQ 保证最终一致性，禁止用本地事务兜底

------

## 安全约束

- JWT 校验统一在 gateway-service，下游服务不重复鉴权
- 下游服务从请求头获取：`X-User-Id`、`X-User-Role`、`X-User-No`
- 密码用 BCryptPasswordEncoder，禁止 MD5 或明文

------

## 服务间通信

- 同步调用用 OpenFeign，Feign 接口放对应服务的 api 模块
- 异步场景（通知、评分、文档向量化）用 RabbitMQ
- Feign 调用必须配置 fallback 或 fallbackFactory

------

## 文件存储

- MinIO bucket 按服务划分：course-bucket / homework-bucket / ai-bucket
- 视频用 Range 请求流式传输，禁止一次性加载

------

## 缓存

- Redis key：`{服务名}:{业务}:{id}`，例如 `course:info:1001`
- 所有业务缓存必须设置过期时间
- 考试倒计时 key：`exam:countdown:{examId}:{userId}`

------

## AI 服务

- 每门课程独立 RAG 知识库，`course_id` 作为 namespace 隔离
- 全局文档 namespace = `"global"`，由管理员上传
- RAG 检索合并 course namespace + global namespace，course 优先
- 权限：学生只能对话；教师管本课程文档；管理员管全局文档
- 知识库上传资料流程：存 MinIO → RabbitMQ → 异步向量化 → 存 PGVector

------

## 代码规范

- 包名：`com.bucher.{服务名}`
- 主键：雪花 ID（`IdUtil.getSnowflakeNextId()`），禁止自增 INT
- 时间：`LocalDateTime`，禁止 `Date`
- 响应体：`Result<T>`（code / message / data）
- Controller 只做参数校验 + 响应封装，业务逻辑全在 Service

------

## 数据库字段约定

- 主键：BIGINT 雪花 ID
- 不分库，所有的业务数据都存在一个库里，即bucher_user，写application的数据库配置时候要写这个。
- 必须有：`create_time DATETIME`、`update_time DATETIME`
- 软删除：`is_deleted TINYINT(1) DEFAULT 0`
- 所有字段必须写 COMMENT

------

## 配置安全

严格遵守以下关于项目配置文件的规范和 Git 提交原则：

### 1. 配置文件体系架构

 本项目采用 Spring Boot 的多环境配置机制，配置文件分为以下三类： 

 **通用配置 (`application.yml` 或 `application.properties`)**：存放与环境无关、非敏感的通用配置（如项目端口、Spring 基础设置、业务通用常量等）。

**本地开发与密钥配置 (`application-local.yml`)**：存放本地开发环境的配置，以及所有**敏感密钥、数据库密码、第三方 API Key、Token 密钥**等。通过激活 `local` 环境来加载。 

**生产环境配置 (`application-prod.yml`)**：存放线上生产环境的特定配置，敏感信息通常采用环境变量引用的方式。**

### 2. 代码生成与修改原则 

**禁止向 `application` 写入任何明文密钥、密码或敏感凭证**。

所有此类内容必须引导我写入 `application-local` 或通过环境变量注入。 *需要添加新配置项时，请主动说明该配置项应该放进哪个配置文件。在编写、修改代码或更新配置时，确保不会破坏这三个文件之间的激活/包含关系。  

### 3. git 提交与安全原则

**核心原则**：Git 仓库中只允许上传和追踪通用配置（`application`）。

禁止提交本地私密配置**：`application-local` 严禁被提交到 Git 仓库。 

## 建表语句bucher.sql写入规则

bucher.sql的内容是此项目数据库和表的建表语句

位置在D:\Projects\bucher\bucher-backend\bucher.sql

规则是当需要更改bucher.sql中以存在的某个表的结构时，只能使用alert等语句，不许直接修改建表语句。