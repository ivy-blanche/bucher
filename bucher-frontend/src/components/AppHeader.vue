<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Setting, SwitchButton, ArrowLeft, Message } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { logout } from '@/api/user'
import { getUnreadCount } from '@/api/message'
import { useAIStore } from '@/stores/ai'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const aiStore = useAIStore()

// 用户信息
const userName = computed(() => userStore.userInfo?.realName || '用户')
const userAvatar = computed(() => userStore.userInfo?.avatarUrl || '')
const isStudent = computed(() => userStore.userInfo?.roleName === '学生')
const unreadCount = ref(0)

onMounted(async () => {
  if (!isStudent.value) return
  try {
    unreadCount.value = await getUnreadCount()
  } catch {
    // 静默失败，角标保持 0
  }
})

// 路由变化时刷新未读数（例如从消息页返回）
watch(
  () => route.path,
  async () => {
    if (!isStudent.value) return
    try {
      unreadCount.value = await getUnreadCount()
    } catch {
      // 静默失败
    }
  }
)

// 是否在课程详情页
const isCourseDetail = computed(() => route.name === 'courseDetail')
const courseId = computed(() => route.params.id as string || '')

// 课程信息（从 query 参数获取）
const courseName = computed(() => (route.query.name as string) || '未知课程')
const courseTeacher = computed(() => (route.query.teacher as string) || '未知教师')
const courseSemester = computed(() => (route.query.semester as string) || '未知学期')

// 是否管理员
const isAdmin = computed(() => userStore.userInfo?.roleName === '管理员')

// 下拉菜单选项
const adminMenuItems = [
  { key: 'admin', label: '管理后台', icon: 'admin' },
  { key: 'courses', label: '我的课程', icon: 'courses' },
  { key: 'settings', label: '账号设置', icon: 'settings' },
  { key: 'logout', label: '退出登录', icon: 'logout', divider: true }
]

const userMenuItems = [
  { key: 'courses', label: '我的课程', icon: 'courses' },
  { key: 'settings', label: '账号设置', icon: 'settings' },
  { key: 'logout', label: '退出登录', icon: 'logout', divider: true }
]

const menuItems = computed(() => isAdmin.value ? adminMenuItems : userMenuItems)

// 处理菜单点击
const handleMenuClick = async (key: string) => {
  switch (key) {
    case 'admin':
      router.push('/admin/departments')
      break
    case 'courses':
      router.push('/courses')
      break
    case 'settings':
      router.push('/settings')
      break
    case 'logout':
      try {
        await logout()
        userStore.clearUser()
        ElMessage.success('已退出登录')
        router.push('/login')
      } catch (error) {
        // 即使API失败也清除本地状态
        userStore.clearUser()
        router.push('/login')
      }
      break
  }
}

// 跳转 AI 助手
const goToAI = () => {
  const cid = courseId.value || aiStore.courseId
  if (cid) {
    router.push({
      name: 'courseDetail',
      params: { id: cid },
      query: { tab: 'ai', name: courseName.value || aiStore.courseName },
    })
  } else {
    router.push({ name: 'courseList' })
    ElMessage.info('请先进入课程')
  }
}

// 跳转首页
const goHome = () => {
  router.push('/')
}

// 返回课程列表
const goBackToCourses = () => {
  router.push('/courses')
}
</script>

<template>
  <header class="app-header">
    <!-- 背景装饰 -->
    <div class="header-bg"></div>
    <div class="header-glow"></div>

    <!-- 内容容器 -->
    <div class="header-content">
      <!-- 左侧区域：课程详情页显示课程信息，否则显示 Logo -->
      <div v-if="isCourseDetail" class="course-info-section">
        <div class="back-btn" @click="goBackToCourses">
          <el-icon :size="18"><ArrowLeft /></el-icon>
        </div>
        <div class="course-info-text">
          <span class="course-name-header">{{ courseName }}</span>
          <span class="course-info-divider">|</span>
          <span class="course-meta-header">{{ courseTeacher }}</span>
          <span class="course-info-divider">|</span>
          <span class="course-meta-header">{{ courseSemester }}</span>
        </div>
      </div>

      <div v-else class="logo-section" @click="goHome">
        <div class="logo-container">
          <!-- 外环动画 -->
          <div class="logo-ring"></div>
          <!-- Logo 图标 -->
          <div class="logo-icon">
            <svg viewBox="0 0 40 40" class="logo-svg">
              <defs>
                <linearGradient id="headerLogoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style="stop-color:#00d4ff;stop-opacity:1" />
                  <stop offset="100%" style="stop-color:#0066ff;stop-opacity:1" />
                </linearGradient>
              </defs>
              <!-- 六边形外框 -->
              <polygon
                points="20,3 35,11 35,29 20,37 5,29 5,11"
                fill="none"
                stroke="url(#headerLogoGradient)"
                stroke-width="1.5"
              />
              <!-- 内部书本图标 -->
              <g transform="translate(10, 12)">
                <path
                  d="M0,0 L0,12 Q5,10 10,12 L10,0 Q5,2 0,0 Z"
                  fill="url(#headerLogoGradient)"
                  opacity="0.3"
                />
                <path
                  d="M10,0 L10,12 Q15,10 20,12 L20,0 Q15,2 10,0 Z"
                  fill="url(#headerLogoGradient)"
                  opacity="0.5"
                />
                <path
                  d="M0,0 Q5,2 10,0 M10,0 Q15,2 20,0"
                  fill="none"
                  stroke="url(#headerLogoGradient)"
                  stroke-width="1"
                />
              </g>
              <!-- 顶部小球 -->
              <circle cx="20" cy="8" r="2" fill="url(#headerLogoGradient)" />
            </svg>
          </div>
        </div>

        <!-- 品牌名称 -->
        <div class="brand-name">
          <span class="brand-text">智学平台</span>
        </div>
      </div>

      <!-- 右侧用户区域 -->
      <div class="user-section">
        <!-- AI 助手（仅学生端显示） -->
        <div v-if="isStudent" class="message-btn ai-btn" @click="goToAI" title="AI 助教">
          <el-icon :size="22">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="22" height="22">
              <circle cx="12" cy="12" r="10" />
              <path d="M12 6v6l4 2" stroke-linecap="round" stroke-linejoin="round" />
              <path d="M8 14c0 2 1.5 3.5 4 3.5s4-1.5 4-3.5" stroke-linecap="round" />
            </svg>
          </el-icon>
        </div>
        <!-- 消息图标（仅学生端显示） -->
        <div v-if="isStudent" class="message-btn" @click="router.push('/messages')">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="message-badge">
            <el-icon :size="22"><Message /></el-icon>
          </el-badge>
        </div>
        <el-dropdown
          trigger="click"
          placement="bottom-end"
          :popper-options="{ modifiers: [{ name: 'offset', options: { offset: [0, 8] } }] }"
          @command="handleMenuClick"
        >
          <div class="user-trigger">
            <div class="avatar-wrapper">
              <el-avatar
                :size="36"
                :src="userAvatar"
                class="user-avatar"
              >
                <el-icon :size="20"><User /></el-icon>
              </el-avatar>
              <div class="avatar-ring"></div>
            </div>
            <span class="user-name">{{ userName }}</span>
            <svg class="dropdown-arrow" viewBox="0 0 12 12" width="12" height="12">
              <path d="M3 4.5L6 7.5L9 4.5" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>

          <template #dropdown>
            <div class="dropdown-menu">
              <!-- 管理后台（仅管理员可见） -->
              <div v-if="isAdmin" class="menu-item menu-item-admin" @click="handleMenuClick('admin')">
                <div class="menu-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="3" width="7" height="7" rx="1" />
                    <rect x="14" y="3" width="7" height="7" rx="1" />
                    <rect x="3" y="14" width="7" height="7" rx="1" />
                    <rect x="14" y="14" width="7" height="7" rx="1" />
                  </svg>
                </div>
                <span class="menu-label">管理后台</span>
                <svg class="menu-arrow" viewBox="0 0 12 12" width="14" height="14">
                  <path d="M4.5 2L8.5 6L4.5 10" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <div v-if="isAdmin" class="menu-divider"></div>

              <!-- 我的课程 -->
              <div class="menu-item" @click="handleMenuClick('courses')">
                <div class="menu-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
                    <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
                    <path d="M8 7h8M8 11h6"/>
                  </svg>
                </div>
                <span class="menu-label">我的课程</span>
                <svg class="menu-arrow" viewBox="0 0 12 12" width="14" height="14">
                  <path d="M4.5 2L8.5 6L4.5 10" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>

              <!-- 账号设置 -->
              <div class="menu-item" @click="handleMenuClick('settings')">
                <div class="menu-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="3"/>
                    <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
                  </svg>
                </div>
                <span class="menu-label">账号设置</span>
                <svg class="menu-arrow" viewBox="0 0 12 12" width="14" height="14">
                  <path d="M4.5 2L8.5 6L4.5 10" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>

              <!-- 分割线 -->
              <div class="menu-divider"></div>

              <!-- 退出登录 -->
              <div class="menu-item menu-item-logout" @click="handleMenuClick('logout')">
                <div class="menu-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                    <polyline points="16 17 21 12 16 7"/>
                    <line x1="21" y1="12" x2="9" y2="12"/>
                  </svg>
                </div>
                <span class="menu-label">退出登录</span>
              </div>
            </div>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap');

.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 64px;
  z-index: 1000;
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 背景效果 */
.header-bg {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(0, 102, 255, 0.08);
}

.header-glow {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.03) 0%, rgba(0, 102, 255, 0.02) 100%);
  pointer-events: none;
}

/* 内容容器 */
.header-content {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 32px;
  max-width: 1440px;
  margin: 0 auto;
}

/* Logo 区域 */
.logo-section {
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.logo-section:hover {
  transform: scale(1.02);
}

.logo-container {
  position: relative;
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-ring {
  position: absolute;
  inset: -2px;
  border: 1.5px solid transparent;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.2), rgba(0, 102, 255, 0.2)) padding-box,
              linear-gradient(135deg, #00d4ff, #0066ff) border-box;
  animation: ringPulse 3s ease-in-out infinite;
}

@keyframes ringPulse {
  0%, 100% { opacity: 0.6; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.05); }
}

.logo-icon {
  position: relative;
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-svg {
  width: 100%;
  height: 100%;
}

.brand-name {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.brand-text {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 2px;
}

/* 用户区域 */
.user-section {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 消息按钮 */
.message-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  color: #5a7a9a;
  transition: all 0.3s ease;
  position: relative;
}

.message-btn:hover {
  background: rgba(0, 102, 255, 0.08);
  color: #0066ff;
}

.message-badge :deep(.el-badge__content) {
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  border: 2px solid #fff;
  font-size: 11px;
  height: 18px;
  line-height: 14px;
  padding: 0 5px;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 16px 6px 8px;
  border-radius: 28px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: transparent;
}

.user-trigger:hover {
  background: rgba(0, 102, 255, 0.06);
}

.avatar-wrapper {
  position: relative;
  width: 36px;
  height: 36px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #e0f7ff 0%, #b3e5fc 100%);
  color: #0066ff;
  font-weight: 600;
}

.avatar-ring {
  position: absolute;
  inset: -2px;
  border: 2px solid transparent;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.3), rgba(0, 102, 255, 0.3)) padding-box,
              linear-gradient(135deg, #00d4ff, #0066ff) border-box;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.user-trigger:hover .avatar-ring {
  opacity: 1;
}

.user-name {
  font-size: 15px;
  font-weight: 500;
  color: #1a3a5a;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-arrow {
  color: #7a9aba;
  transition: transform 0.3s ease;
}

.user-trigger:hover .dropdown-arrow {
  color: #0066ff;
}

/* 下拉菜单 */
:deep(.el-dropdown) {
  line-height: 1;
}

.dropdown-menu {
  min-width: 200px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(0, 102, 255, 0.1);
  box-shadow:
    0 20px 40px rgba(0, 102, 255, 0.15),
    0 8px 16px rgba(0, 102, 255, 0.08);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.menu-item:hover {
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.08) 0%, rgba(0, 102, 255, 0.06) 100%);
}

.menu-item:hover .menu-icon {
  color: #0066ff;
}

.menu-item:hover .menu-arrow {
  opacity: 1;
  transform: translateX(2px);
}

.menu-icon {
  width: 20px;
  height: 20px;
  color: #5a7a9a;
  transition: color 0.2s ease;
}

.menu-icon svg {
  width: 100%;
  height: 100%;
}

.menu-label {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: #3a5a7a;
}

.menu-arrow {
  color: #a0b8d0;
  opacity: 0;
  transition: all 0.2s ease;
}

.menu-divider {
  height: 1px;
  margin: 8px 0;
  background: linear-gradient(90deg, transparent, rgba(0, 102, 255, 0.15), transparent);
}

.menu-item-logout:hover {
  background: linear-gradient(135deg, rgba(255, 100, 100, 0.08) 0%, rgba(255, 80, 80, 0.06) 100%);
}

.menu-item-logout:hover .menu-icon {
  color: #ff6b6b;
}

.menu-item-logout:hover .menu-label {
  color: #ff6b6b;
}

/* 管理后台菜单项 */
.menu-item-admin:hover {
  background: linear-gradient(135deg, rgba(0, 102, 255, 0.08) 0%, rgba(0, 212, 255, 0.06) 100%);
}

.menu-item-admin:hover .menu-icon {
  color: #0066ff;
}

/* 课程详情页 - Header 左侧课程信息 */
.course-info-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  cursor: pointer;
  color: #5a7a9a;
  transition: all 0.2s ease;
}

.back-btn:hover {
  background: rgba(0, 102, 255, 0.08);
  color: #0066ff;
}

.course-info-text {
  display: flex;
  align-items: center;
  gap: 10px;
}

.course-name-header {
  font-size: 18px;
  font-weight: 700;
  color: #1a3a5a;
}

.course-info-divider {
  color: rgba(0, 102, 255, 0.15);
  font-size: 16px;
}

.course-meta-header {
  font-size: 14px;
  font-weight: 400;
  color: #7a9aba;
}

/* 响应式 */
@media (max-width: 768px) {
  .header-content {
    padding: 0 16px;
  }

  .brand-text {
    font-size: 18px;
  }

  .user-name {
    display: none;
  }

  .user-trigger {
    padding: 6px;
    border-radius: 50%;
  }
}

@media (max-width: 480px) {
  .logo-container {
    width: 36px;
    height: 36px;
  }

  .logo-icon {
    width: 32px;
    height: 32px;
  }

  .brand-text {
    font-size: 16px;
  }
}
</style>
