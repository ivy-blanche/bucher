# 智学平台后端 API 接口文档

> 基础路径：`http://localhost:8080/api`（通过网关访问）

---

## 通用说明

### 响应格式

所有接口统一返回格式：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1716000000000
}
```

### 认证方式

需要登录的接口，请求头需携带：

```
Authorization: Bearer {token}
```

登录成功后从 `/auth/login` 接口获取 token。

### 角色说明

| 值 | 角色 |
|---|------|
| 1 | 管理员 |
| 2 | 教师 |
| 3 | 学生 |

---

## 一、认证模块

### 1.1 登录

**POST** `/api/user/auth/login`

**无需认证**

**请求体：**
```json
{
  "account": "user@example.com",
  "password": "Password123"
}
```

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| account | string | 是 | 账号（邮箱或学号/工号）|
| password | string | 是 | 密码 |

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1234567890,
    "userNo": "2024001",
    "realName": "张三",
    "role": 3,
    "roleName": "学生",
    "avatarUrl": null,
    "email": "test@example.com",
    "pwdReset": 0
  }
}
```

| 字段 | 类型 | 说明 |
|-----|------|------|
| pwdReset | int | 是否需要强制修改密码：0-否，1-是（管理员重置密码后为1）|

> 注：自主注册用户 `userNo` 为 null，使用邮箱登录；管理员添加的用户使用学号/工号登录。

---

### 1.2 发送验证码

**POST** `/api/user/auth/send-code`

**无需认证**

**请求体：**
```json
{
  "email": "user@example.com",
  "type": "register"
}
```

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| email | string | 是 | 邮箱地址 |
| type | string | 是 | 类型：`register`（注册）或 `reset`（重置密码）|

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

### 1.3 注册（自主注册）

**POST** `/api/user/auth/register`

**无需认证**

**请求体：**
```json
{
  "email": "zhangsan@example.com",
  "emailCode": "123456",
  "realName": "张三",
  "password": "Password123"
}
```

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| email | string | 是 | 邮箱 |
| emailCode | string | 是 | 邮箱验证码 |
| realName | string | 是 | 真实姓名 |
| password | string | 是 | 密码（至少8位，含大小写字母和数字）|

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

> 注：自主注册用户默认角色为学生（role=3），来源标记为"自主注册"（source=2）。

---

### 1.4 重置密码

**POST** `/api/user/auth/reset-password`

**无需认证**

**请求体：**
```json
{
  "email": "user@example.com",
  "emailCode": "123456",
  "newPassword": "NewPassword123"
}
```

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| email | string | 是 | 邮箱 |
| emailCode | string | 是 | 邮箱验证码 |
| newPassword | string | 是 | 新密码（至少8位，含大小写字母和数字）|

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

### 1.5 登出

**POST** `/api/user/auth/logout`

**需要认证**

**请求头：**
```
Authorization: Bearer {token}
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

### 1.6 获取当前用户信息

**GET** `/api/user/auth/me`

**需要认证**

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1234567890,
    "userNo": "2024001",
    "realName": "张三",
    "email": "zhangsan@example.com",
    "phone": "13800138000",
    "avatarUrl": null,
    "role": 3,
    "roleName": "学生",
    "deptId": 1,
    "deptName": "计算机学院",
    "adminClassId": 1,
    "adminClassName": "2024级计算机1班",
    "source": 2,
    "auditStatus": null,
    "status": 1,
    "pwdReset": 0,
    "createTime": "2024-01-01T10:00:00"
  }
}
```

| 字段 | 类型 | 说明 |
|-----|------|------|
| source | int | 用户来源：1-管理员添加，2-自主注册 |
| auditStatus | int | 教师申请审核状态：0-待审核，1-通过，2-拒绝，null-未申请 |
| pwdReset | int | 是否需要强制修改密码：0-否，1-是 |

---

### 1.7 强制修改密码

**POST** `/api/user/auth/change-password`

**需要认证**

> 管理员重置密码后，用户首次登录时 `pwdReset=1`，前端应引导用户调用此接口修改密码。

**请求体：**
```json
{
  "newPassword": "NewPassword123"
}
```

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| newPassword | string | 是 | 新密码（至少8位，含大小写字母和数字）|

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

## 二、管理员模块

> 所有管理员接口需要管理员角色（role=1）才能访问。

---

### 2.1 院系管理

#### 2.1.1 分页查询院系列表

**GET** `/api/user/admin/departments`

**需要认证 + 管理员角色**

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页条数，默认10 |
| name | string | 否 | 院系名称（模糊搜索）|
| type | int | 否 | 类型：1-学校院系，2-企业部门 |

**响应：**
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1234567890,
        "name": "计算机学院",
        "type": 1,
        "description": "计算机相关专业",
        "createTime": "2024-01-01T10:00:00"
      }
    ],
    "total": 1,
    "current": 1,
    "size": 10
  }
}
```

---

#### 2.1.2 获取院系详情

**GET** `/api/user/admin/departments/{id}`

**需要认证 + 管理员角色**

---

#### 2.1.3 新增院系

**POST** `/api/user/admin/departments`

**需要认证 + 管理员角色**

**请求体：**
```json
{
  "name": "计算机学院",
  "type": 1,
  "description": "计算机相关专业"
}
```

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| name | string | 是 | 院系名称 |
| type | int | 是 | 类型：1-学校院系，2-企业部门 |
| description | string | 否 | 描述 |

**响应：**
```json
{
  "code": 200,
  "data": {
    "id": 1234567890,
    "name": "计算机学院",
    "type": 1,
    "description": "计算机相关专业",
    "createTime": "2024-01-01T10:00:00"
  }
}
```

---

#### 2.1.4 修改院系

**PUT** `/api/user/admin/departments/{id}`

**需要认证 + 管理员角色**

**请求体：** 同新增

---

#### 2.1.5 删除院系

**DELETE** `/api/user/admin/departments/{id}`

**需要认证 + 管理员角色**

> 注意：院系下存在行政班级时无法删除。

---

### 2.2 行政班级管理

#### 2.2.1 分页查询行政班级列表

**GET** `/api/user/admin/classes`

**需要认证 + 管理员角色**

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页条数，默认10 |
| deptId | long | 否 | 所属院系ID |
| name | string | 否 | 班级名称（模糊搜索）|

**响应：**
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1234567890,
        "name": "2024级计算机1班",
        "deptId": 1,
        "deptName": "计算机学院",
        "year": 2024,
        "createTime": "2024-01-01T10:00:00"
      }
    ],
    "total": 1,
    "current": 1,
    "size": 10
  }
}
```

---

#### 2.2.2 获取行政班级详情

**GET** `/api/user/admin/classes/{id}`

**需要认证 + 管理员角色**

---

#### 2.2.3 新增行政班级

**POST** `/api/user/admin/classes`

**需要认证 + 管理员角色**

**请求体：**
```json
{
  "name": "2024级计算机1班",
  "deptId": 1234567890,
  "year": 2024
}
```

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| name | string | 是 | 班级名称 |
| deptId | long | 是 | 所属院系ID |
| year | int | 是 | 入学年份 |

**响应：**
```json
{
  "code": 200,
  "data": {
    "id": 1234567890,
    "name": "2024级计算机1班",
    "deptId": 1,
    "deptName": "计算机学院",
    "year": 2024,
    "createTime": "2024-01-01T10:00:00"
  }
}
```

---

#### 2.2.4 修改行政班级

**PUT** `/api/user/admin/classes/{id}`

**需要认证 + 管理员角色**

**请求体：** 同新增

---

#### 2.2.5 删除行政班级

**DELETE** `/api/user/admin/classes/{id}`

**需要认证 + 管理员角色**

> 注意：班级下存在学生时无法删除。

---

### 2.3 批量导入学生

**POST** `/api/user/admin/students/import`

**需要认证 + 管理员角色**

> Content-Type: multipart/form-data

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| adminClassId | long | 是 | 目标行政班级ID |
| file | file | 是 | Excel 文件（.xlsx）|

**Excel 模板列：**

| 列名 | 必填 | 说明 |
|-----|------|------|
| 学号 | 是 | 唯一，不可与已有学号重复 |
| 姓名 | 是 | 真实姓名 |
| 邮箱 | 是 | 唯一，不可与已有邮箱重复 |
| 手机号 | 否 | 手机号 |

**成功响应：**
```json
{
  "code": 200,
  "data": {
    "totalCount": 50,
    "successCount": 50,
    "failCount": 0,
    "errors": []
  }
}
```

**失败响应（有错误行时不会插入任何数据）：**
```json
{
  "code": 200,
  "data": {
    "totalCount": 50,
    "successCount": 0,
    "failCount": 2,
    "errors": [
      { "row": 3, "userNo": "2024001", "reason": "学号已被使用" },
      { "row": 5, "userNo": "2024003", "reason": "邮箱已被使用" }
    ]
  }
}
```

---

### 2.4 批量导入教师

**POST** `/api/user/admin/teachers/import`

**需要认证 + 管理员角色**

> Content-Type: multipart/form-data

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| deptId | long | 是 | 目标院系ID |
| file | file | 是 | Excel 文件（.xlsx）|

**Excel 模板列：**

| 列名 | 必填 | 说明 |
|-----|------|------|
| 工号 | 是 | 唯一，不可与已有工号重复 |
| 姓名 | 是 | 真实姓名 |
| 邮箱 | 是 | 唯一，不可与已有邮箱重复 |
| 手机号 | 否 | 手机号 |

**响应：** 同学生导入

---

### 2.5 重置用户密码

**PUT** `/api/user/admin/users/{id}/reset-password`

**需要认证 + 管理员角色**

> 将用户密码重置为默认密码（`Zx@123456`），该用户下次登录时将强制修改密码。

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

### 2.6 用户列表

**GET** `/api/user/admin/users`

**需要认证 + 管理员角色**

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页条数，默认10 |
| userRole | int | 否 | 角色筛选：1-管理员，2-教师，3-学生 |
| deptId | long | 否 | 所属院系ID |
| adminClassId | long | 否 | 所属班级ID |
| keyword | string | 否 | 关键词（匹配姓名/学号/邮箱）|

**响应：**
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1234567890,
        "userNo": "2024001",
        "realName": "张三",
        "email": "zhangsan@example.com",
        "role": 3,
        "roleName": "学生",
        "deptName": "计算机学院",
        "adminClassName": "2024级计算机1班",
        "status": 1,
        "pwdReset": 0,
        "createTime": "2024-01-01T10:00:00"
      }
    ],
    "total": 1,
    "current": 1,
    "size": 10
  }
}
```

---

## 三、错误码说明

| 错误码 | 说明 |
|-------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权（未登录或token过期）|
| 403 | 禁止访问（权限不足）|
| 500 | 服务器内部错误 |
| 1001 | 用户不存在 |
| 1002 | 密码错误 |
| 1003 | 用户已被禁用 |
| 1004 | 用户已存在 |
| 1005 | 邮箱已被注册 |
| 1006 | 该邮箱未注册 |
| 1007 | 验证码错误或已过期 |
| 1008 | 验证码发送过于频繁 |
| 1010 | 院系不存在 |
| 1011 | 行政班级不存在 |
| 1012 | 行政班级不属于所选院系 |
| 1013 | 行政班级下存在学生，无法删除 |
| 1014 | Excel导入失败 |
| 1015 | 工号/学号已存在 |
| 1016 | 上传文件不能为空 |
| 1017 | 无权限执行此操作 |

---

## 四、前端调用示例

### Axios 配置

```javascript
import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000
})

// 请求拦截器 - 携带 token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器 - 处理错误
api.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      // token 过期，跳转登录
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
```

### 登录示例

```javascript
// 登录
const login = async (userNo, password) => {
  const res = await api.post('/user/auth/login', { userNo, password })
  if (res.code === 200) {
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('userInfo', JSON.stringify(res.data))
  }
  return res
}

// 获取当前用户
const getCurrentUser = async () => {
  const res = await api.get('/user/auth/me')
  return res.data
}

// 登出
const logout = async () => {
  await api.post('/user/auth/logout')
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
}
```

---

## 五、服务端口（直接访问）

如果需要绕过网关直接访问服务：

| 服务 | 端口 | 基础路径 |
|-----|------|---------|
| user-service | 8101 | http://localhost:8101 |
| course-service | 8102 | http://localhost:8102 |
| question-service | 8103 | http://localhost:8103 |
| exam-service | 8104 | http://localhost:8104 |
| homework-service | 8105 | http://localhost:8105 |
| grade-service | 8106 | http://localhost:8106 |
| message-service | 8107 | http://localhost:8107 |
| ai-service | 8108 | http://localhost:8108 |

> 直接访问时无需 `/api/user` 前缀，直接使用 `/auth/login` 或 `/auth/me` 即可。
