import request from './index'

// 登录参数
export interface LoginParams {
  account: string
  password: string
}

// 登录响应
export interface LoginResult {
  token: string
  userId: number
  userNo: string | null
  realName: string
  role: number
  roleName: string
  avatarUrl: string | null
  email: string
  pwdReset: number
}

// 注册参数
export interface RegisterParams {
  email: string
  emailCode: string
  realName: string
  password: string
}

// 发送验证码参数
export interface SendCodeParams {
  email: string
  type: 'register' | 'reset'
}

// 重置密码参数
export interface ResetPasswordParams {
  email: string
  emailCode: string
  newPassword: string
}

// 用户信息
export interface UserInfo {
  id: number
  userNo: string | null
  realName: string
  email: string
  phone: string | null
  avatarUrl: string | null
  role: number
  roleName: string
  deptId: number | null
  deptName: string | null
  adminClassId: number | null
  adminClassName: string | null
  source: number
  auditStatus: number | null
  status: number
  pwdReset: number
  createTime: string
}

// 用户登录
export function login(params: LoginParams): Promise<LoginResult> {
  return request.post('/user/auth/login', params)
}

// 发送验证码
export function sendCode(params: SendCodeParams): Promise<void> {
  return request.post('/user/auth/send-code', params)
}

// 用户注册
export function register(params: RegisterParams): Promise<void> {
  return request.post('/user/auth/register', params)
}

// 重置密码
export function resetPassword(params: ResetPasswordParams): Promise<void> {
  return request.post('/user/auth/reset-password', params)
}

// 获取当前用户信息
export function getUserInfo(): Promise<UserInfo> {
  return request.get('/user/auth/me')
}

// 用户登出
export function logout(): Promise<void> {
  return request.post('/user/auth/logout')
}

// 强制修改密码
export function changePassword(newPassword: string): Promise<void> {
  return request.post('/user/auth/change-password', { newPassword })
}
