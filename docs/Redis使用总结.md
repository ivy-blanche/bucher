# 智学平台 Redis 使用总结（面试口述版）

## 一、项目里哪些地方用到了 Redis？

我们项目里 Redis 主要用在三个业务场景：

### 1. 用户认证（user-service）—— Token 存储与踢出登录

用户登录成功后，服务端会生成 JWT Token，同时把这个 Token 存到 Redis 里，Key 是 `user:token:{userId}`，TTL 设 24 小时。

**为什么这么做？** 因为 JWT 本身是无状态的，服务端没法主动让它失效。但我们业务上有"修改密码后强制重新登录"的需求，所以把 Token 存在 Redis，重置密码时把 Redis 里的 Token 删掉。网关校验 Token 的时候，除了验签名，还会比对 Redis 里的 Token 是否一致，不一致就拒绝，以此实现"踢出登录"。

```java
// AuthServiceImpl.java
private static final String TOKEN_KEY_PREFIX = "user:token:";

// 登录时写入 Redis
String tokenKey = TOKEN_KEY_PREFIX + user.getId();
RBucket<String> tokenBucket = redissonClient.getBucket(tokenKey);
tokenBucket.set(token, 24, TimeUnit.HOURS);

// 重置密码时删除 Token，强制重新登录
String tokenKey = TOKEN_KEY_PREFIX + user.getId();
redissonClient.getBucket(tokenKey).delete();

// 登出时删除 Token
public void logout(Long userId) {
    String tokenKey = TOKEN_KEY_PREFIX + userId;
    redissonClient.getBucket(tokenKey).delete();
}
```

### 2. 邮件验证码（user-service）—— 验证码存储 + 发送频率限制

注册和密码重置都需要邮箱验证码。这里用了两个 Key：
- `user:email:code:{email}`：存验证码本身，TTL 5 分钟（300 秒）。
- `user:email:limit:{email}`：限制同一个邮箱 60 秒内只能发一次，TTL 60 秒。

**频率限制**是为了防止恶意刷接口。发验证码前先看 limit Key 是否存在，存在就直接拒绝。

```java
// EmailServiceImpl.java
private static final String CODE_KEY_PREFIX = "user:email:code:";
private static final String LIMIT_KEY_PREFIX = "user:email:limit:";

public void sendVerificationCode(String email, String type) {
    // 1. 检查发送频率
    String limitKey = LIMIT_KEY_PREFIX + email;
    RBucket<Integer> limitBucket = redissonClient.getBucket(limitKey);
    if (limitBucket.isExists()) {
        throw new BusinessException("验证码发送过于频繁，请稍后再试");
    }

    // 2. 生成并存储验证码
    String code = RandomUtil.randomNumbers(codeLength);
    String codeKey = CODE_KEY_PREFIX + email;
    RBucket<String> codeBucket = redissonClient.getBucket(codeKey);
    codeBucket.set(code, codeExpire, TimeUnit.SECONDS);   // TTL 300s

    // 3. 设置频率限制
    limitBucket.set(1, codeInterval, TimeUnit.SECONDS);   // TTL 60s

    // 4. 发送邮件...
}

public boolean verifyCode(String email, String code) {
    String codeKey = CODE_KEY_PREFIX + email;
    RBucket<String> codeBucket = redissonClient.getBucket(codeKey);
    if (!codeBucket.isExists()) return false;
    return codeBucket.get().equals(code);
}
```

### 3. 作业答案暂存（homework-service）—— 学生答题中途保存

学生做作业时，可以"暂存答案"，数据写到 Redis Hash 里，Key 是 `homework:answers:{homeworkId}:{studentId}`，TTL 7 天。

**为什么用 Redis？** 因为学生做题过程中可能多次保存，如果每次都写数据库，会产生大量无用记录。先放 Redis，等最终提交时一次性落库，提交成功后再把 Redis 清空。

```java
// HomeworkServiceImpl.java
private static final String REDIS_KEY_PREFIX = "homework:answers:";
private static final long REDIS_TTL_SECONDS = 7 * 24 * 60 * 60; // 7 天

// 暂存答案
public void saveAnswer(Long studentId, AnswerSaveDTO dto) {
    String redisKey = REDIS_KEY_PREFIX + dto.getHomeworkId() + ":" + studentId;
    RMap<String, String> map = redissonClient.getMap(redisKey);
    map.put(String.valueOf(dto.getQuestionId()), dto.getAnswer() != null ? dto.getAnswer() : "");
    map.expire(REDIS_TTL_SECONDS, TimeUnit.SECONDS);
}

// 获取作业详情时，优先读 Redis，没有再从 MySQL 读
private Map<String, String> getRedisAnswers(Long homeworkId, Long studentId) {
    String redisKey = REDIS_KEY_PREFIX + homeworkId + ":" + studentId;
    RMap<String, String> map = redissonClient.getMap(redisKey);
    return new HashMap<>(map.readAllMap());
}

// 提交作业后清理 Redis
redisMap.delete();
```

### 4. 课程班级邀请码缓存（course-service）—— 邀请码反查班级

教师创建课程班级时会生成 8 位邀请码，学生凭码加入。为了加速验证，把"邀请码 -> 班级 ID"映射缓存到 Redis，Key 是 `course:invite:{inviteCode}`，TTL 24 小时。

这里还做了**回源兜底**：如果 Redis 里没有，会去数据库查，查到后再回填 Redis。

```java
// CourseClassServiceImpl.java
private static final String INVITE_CODE_KEY_PREFIX = "course:invite:";

private void cacheInviteCode(String code, Long classId) {
    RBucket<Long> bucket = redissonClient.getBucket(INVITE_CODE_KEY_PREFIX + code);
    bucket.set(classId, 24, TimeUnit.HOURS);
}

private Long getClassIdByInviteCode(String code) {
    // 先读 Redis
    RBucket<Long> bucket = redissonClient.getBucket(INVITE_CODE_KEY_PREFIX + code);
    Long classId = bucket.get();
    if (classId != null) return classId;

    // 回源数据库
    CourseClass courseClass = courseClassMapper.selectOne(...);
    if (courseClass != null) {
        cacheInviteCode(code, courseClass.getId());  // 回填缓存
        return courseClass.getId();
    }
    return null;
}

// 删除班级或刷新邀请码时清理缓存
private void clearInviteCodeCache(String code) {
    if (code != null) {
        redissonClient.getBucket(INVITE_CODE_KEY_PREFIX + code).delete();
    }
}
```

---

## 二、Key 和 TTL 是怎么设计的？

### Key 设计原则

我们统一遵循 `业务模块:实体:唯一标识` 的格式：

| 业务场景 | Key 格式 | 示例 |
|---------|---------|------|
| 用户 Token | `user:token:{userId}` | `user:token:1001` |
| 邮箱验证码 | `user:email:code:{email}` | `user:email:code:xxx@qq.com` |
| 发送频率限制 | `user:email:limit:{email}` | `user:email:limit:xxx@qq.com` |
| 作业答案暂存 | `homework:answers:{homeworkId}:{studentId}` | `homework:answers:2001:1001` |
| 邀请码映射 | `course:invite:{inviteCode}` | `course:invite:ABCD1234` |

**这样设计的好处**：
1. **可读性好**：一眼能看出这个 Key 属于哪个模块、什么业务。
2. **避免冲突**：不同模块用不同前缀，不会互相覆盖。
3. **便于批量清理**：比如用户注销时，可以按 `user:token:{userId}*` 模式清理。

### TTL 设计

| 场景 | TTL | 理由 |
|-----|-----|------|
| Token | 24 小时 | 与 JWT 过期时间对齐，避免长期无效 Token 残留 |
| 验证码 | 5 分钟 | 安全考虑，验证码不能长期有效 |
| 发送频率限制 | 60 秒 | 业务定义的最小发送间隔 |
| 作业答案 | 7 天 | 作业周期通常在一周内，过期自动清理避免堆积 |
| 邀请码 | 24 小时 | 邀请码本身不会频繁变化，一天内有效足够 |

**核心原则**：所有业务缓存都设置了过期时间，防止 Redis 成为"垃圾堆"。

---

## 三、为什么选用 Redisson，而不是 Spring 自带的 RedisTemplate？

我们项目里引入了 `redisson-spring-boot-starter`，主要基于这几点考虑：

### 1. 数据结构更丰富、API 更直观

Redisson 把 Redis 的数据结构封装成了 Java 熟悉的对象：
- `RBucket<V>` 对应 String
- `RMap<K, V>` 对应 Hash
- `RList<V>` 对应 List
- `RSet<V>` 对应 Set

比如作业答案暂存用的是 Hash，Redisson 的 `RMap` 可以直接 `put`、`readAllMap`、`expire`，比 `RedisTemplate` 的 `opsForHash()` 链式调用更简洁。

```java
// Redisson 写法
RMap<String, String> map = redissonClient.getMap(redisKey);
map.put(questionId, answer);
map.expire(7, TimeUnit.DAYS);

// RedisTemplate 写法（对比）
redisTemplate.opsForHash().put(redisKey, questionId, answer);
redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);
```

### 2. 原生支持分布式锁

Redisson 内置了 `RLock`，基于 Redis 的 RedLock 算法实现，后面如果业务需要分布式锁（比如防止重复提交、库存扣减），可以直接用，不用再自己写 Lua 脚本。

```java
RLock lock = redissonClient.getLock("order:lock:" + orderId);
lock.lock();
try {
    // 业务逻辑
} finally {
    lock.unlock();
}
```

### 3. 与 Spring Boot 集成简单

加依赖、配地址就能用，不需要额外写配置类。而且 Redisson 会自动复用 Spring Boot 的 `spring.redis` 配置，也可以独立配 `redisson.yml`。

```yaml
# user-service 的 application-local.yml
spring:
  redis:
    host: localhost
    port: 6379
    redisson:
      file: classpath:redisson-local.yml
```

### 4. 更完善的连接管理

Redisson 内置了连接池、哨兵模式、集群模式的完整支持，还有看门狗机制（Watch Dog）自动续期锁，避免业务执行超时导致锁提前释放。

---

## 四、补充：当前已引入但未实际使用 Redis 的服务

以下服务在 `pom.xml` 中引入了 `redisson-spring-boot-starter`，但当前代码中暂无 Redis 使用场景，属于**提前预留**：

| 服务 | 状态 | 可能用途 |
|-----|------|---------|
| exam-service | 已引入，未使用 | 考试倒计时 `exam:countdown:{examId}:{userId}`（已在 CLAUDE.md 规范中定义） |
| question-service | 已引入，未使用 | 题目缓存、题库分组缓存 |
| message-service | 已引入，未使用 | 消息队列消费状态、未读消息计数 |
| ai-service | 已引入，未使用 | 对话上下文缓存、RAG 检索结果缓存 |

---

## 五、一句话总结

> 我们用 Redisson 做了四件事：**Token 管理实现踢出登录**、**验证码存储+频率限制**、**作业答案 Redis 暂存落库**、**邀请码缓存加速查询**。Key 统一按 `模块:实体:标识` 设计，所有缓存都设了 TTL，选型 Redisson 是因为它的 API 更直观、自带分布式锁、和 Spring Boot 集成方便。
