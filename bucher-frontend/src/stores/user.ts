import { defineStore } from 'pinia'
import { ref } from 'vue'

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

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)

  // 从 localStorage 恢复用户信息
  const savedUserInfo = localStorage.getItem('userInfo')
  if (savedUserInfo) {
    try {
      userInfo.value = JSON.parse(savedUserInfo)
    } catch {
      localStorage.removeItem('userInfo')
    }
  }

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(info: Partial<UserInfo> & { userId?: number }) {
    // 兼容登录接口返回的字段名 (userId -> id)
    if (info.userId && !info.id) {
      info.id = info.userId
    }
    userInfo.value = info as UserInfo
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  function clearUser() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    token,
    userInfo,
    setToken,
    setUserInfo,
    clearUser
  }
})
