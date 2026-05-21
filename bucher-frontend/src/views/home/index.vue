<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import MainLayout from '@/layouts/MainLayout.vue'

const router = useRouter()
const userStore = useUserStore()
const userName = computed(() => userStore.userInfo?.realName || '用户')
const roleName = computed(() => userStore.userInfo?.roleName || '')

// 管理员自动跳转到管理后台
onMounted(() => {
  if (userStore.userInfo?.roleName === '管理员') {
    router.replace('/admin/departments')
  }
})

function goToCourses() {
  router.push('/courses')
}
</script>

<template>
  <MainLayout>
    <div class="home-page">
      <!-- 背景装饰 -->
      <div class="bg-grid"></div>
      <div class="floating-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
      </div>

      <!-- 欢迎区域 -->
      <div class="welcome-section">
        <div class="welcome-card">
          <div class="card-glow"></div>
          <div class="welcome-content">
            <!-- 欢迎图标 -->
            <div class="welcome-icon">
              <svg viewBox="0 0 80 80" class="icon-svg">
                <defs>
                  <linearGradient id="welcomeGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" style="stop-color:#00d4ff;stop-opacity:1" />
                    <stop offset="100%" style="stop-color:#0066ff;stop-opacity:1" />
                  </linearGradient>
                </defs>
                <circle cx="40" cy="40" r="35" fill="none" stroke="url(#welcomeGradient)" stroke-width="2" opacity="0.3"/>
                <circle cx="40" cy="40" r="25" fill="url(#welcomeGradient)" opacity="0.1"/>
                <g transform="translate(22, 25)">
                  <!-- 书本 -->
                  <path d="M0,0 L0,25 Q9,22 18,25 L18,0 Q9,3 0,0 Z" fill="url(#welcomeGradient)" opacity="0.6"/>
                  <path d="M18,0 L18,25 Q27,22 36,25 L36,0 Q27,3 18,0 Z" fill="url(#welcomeGradient)" opacity="0.4"/>
                  <line x1="18" y1="0" x2="18" y2="25" stroke="url(#welcomeGradient)" stroke-width="1"/>
                </g>
                <!-- 星星装饰 -->
                <circle cx="60" cy="20" r="3" fill="url(#welcomeGradient)"/>
                <circle cx="20" cy="60" r="2" fill="url(#welcomeGradient)" opacity="0.6"/>
              </svg>
            </div>

            <!-- 欢迎文字 -->
            <div class="welcome-text">
              <h1 class="welcome-title">
                欢迎回来，<span class="user-name-highlight">{{ userName }}</span>
              </h1>
              <p class="welcome-subtitle">
                <span v-if="roleName" class="role-badge">{{ roleName }}</span>
                开始您的学习之旅吧
              </p>
            </div>

            <!-- 快捷入口 -->
            <div class="quick-actions">
              <div class="action-card" @click="goToCourses">
                <div class="action-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
                    <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
                  </svg>
                </div>
                <span class="action-label">我的课程</span>
                <svg class="action-arrow" viewBox="0 0 12 12" width="14" height="14">
                  <path d="M4.5 2L8.5 6L4.5 10" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>

              <div class="action-card">
                <div class="action-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/>
                    <polyline points="14 2 14 8 20 8"/>
                    <line x1="16" y1="13" x2="8" y2="13"/>
                    <line x1="16" y1="17" x2="8" y2="17"/>
                    <line x1="10" y1="9" x2="8" y2="9"/>
                  </svg>
                </div>
                <span class="action-label">我的作业</span>
                <svg class="action-arrow" viewBox="0 0 12 12" width="14" height="14">
                  <path d="M4.5 2L8.5 6L4.5 10" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>

              <div class="action-card">
                <div class="action-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M9 11a3 3 0 1 0 6 0a3 3 0 0 0 -6 0"/>
                    <path d="M17.657 16.657l-4.243 4.243a2 2 0 0 1-2.827 0l-4.244-4.243a8 8 0 1 1 11.314 0z"/>
                  </svg>
                </div>
                <span class="action-label">我的考试</span>
                <svg class="action-arrow" viewBox="0 0 12 12" width="14" height="14">
                  <path d="M4.5 2L8.5 6L4.5 10" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>

              <div class="action-card">
                <div class="action-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 2L2 7l10 5 10-5-10-5z"/>
                    <path d="M2 17l10 5 10-5"/>
                    <path d="M2 12l10 5 10-5"/>
                  </svg>
                </div>
                <span class="action-label">AI 助手</span>
                <svg class="action-arrow" viewBox="0 0 12 12" width="14" height="14">
                  <path d="M4.5 2L8.5 6L4.5 10" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap');

.home-page {
  position: relative;
  min-height: calc(100vh - 64px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  overflow: hidden;
}

/* 网格背景 */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 102, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 102, 255, 0.02) 1px, transparent 1px);
  background-size: 60px 60px;
  pointer-events: none;
}

/* 浮动装饰 */
.floating-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.shape {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.08) 0%, rgba(0, 102, 255, 0.04) 100%);
  backdrop-filter: blur(10px);
}

.shape-1 {
  width: 400px;
  height: 400px;
  top: -150px;
  right: -100px;
  animation: shapeFloat 20s ease-in-out infinite;
}

.shape-2 {
  width: 300px;
  height: 300px;
  bottom: -100px;
  left: -80px;
  animation: shapeFloat 25s ease-in-out infinite reverse;
}

.shape-3 {
  width: 200px;
  height: 200px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: shapeFloat 18s ease-in-out infinite;
}

@keyframes shapeFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.05); }
  66% { transform: translate(-20px, 20px) scale(0.95); }
}

/* 欢迎区域 */
.welcome-section {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 800px;
}

.welcome-card {
  position: relative;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 32px;
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow:
    0 25px 60px rgba(0, 102, 255, 0.1),
    0 10px 30px rgba(0, 102, 255, 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  overflow: hidden;
}

.card-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at 50% 0%, rgba(0, 212, 255, 0.1) 0%, transparent 50%);
  pointer-events: none;
}

.welcome-content {
  position: relative;
  padding: 48px;
  text-align: center;
}

/* 欢迎图标 */
.welcome-icon {
  width: 100px;
  height: 100px;
  margin: 0 auto 28px;
}

.icon-svg {
  width: 100%;
  height: 100%;
  animation: iconFloat 4s ease-in-out infinite;
}

@keyframes iconFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

/* 欢迎文字 */
.welcome-text {
  margin-bottom: 40px;
}

.welcome-title {
  font-size: 32px;
  font-weight: 600;
  color: #1a3a5a;
  margin-bottom: 12px;
  line-height: 1.3;
}

.user-name-highlight {
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.welcome-subtitle {
  font-size: 16px;
  color: #7a9aba;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.role-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.15) 0%, rgba(0, 102, 255, 0.1) 100%);
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  color: #0066ff;
}

/* 快捷入口 */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px 16px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  border: 1px solid rgba(0, 102, 255, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.action-card:hover {
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(0, 102, 255, 0.2);
  transform: translateY(-4px);
  box-shadow: 0 15px 35px rgba(0, 102, 255, 0.1);
}

.action-card:hover .action-icon {
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  color: white;
}

.action-card:hover .action-arrow {
  opacity: 1;
  transform: translateX(0);
}

.action-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.1) 0%, rgba(0, 102, 255, 0.08) 100%);
  border-radius: 14px;
  color: #0066ff;
  transition: all 0.3s ease;
}

.action-icon svg {
  width: 24px;
  height: 24px;
}

.action-label {
  font-size: 14px;
  font-weight: 500;
  color: #3a5a7a;
}

.action-arrow {
  position: absolute;
  top: 12px;
  right: 12px;
  color: #0066ff;
  opacity: 0;
  transform: translateX(-4px);
  transition: all 0.3s ease;
}

/* 响应式 */
@media (max-width: 768px) {
  .welcome-content {
    padding: 32px 24px;
  }

  .welcome-icon {
    width: 80px;
    height: 80px;
  }

  .welcome-title {
    font-size: 26px;
  }

  .quick-actions {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .home-page {
    padding: 24px 16px;
  }

  .welcome-card {
    border-radius: 24px;
  }

  .welcome-content {
    padding: 28px 20px;
  }

  .welcome-icon {
    width: 64px;
    height: 64px;
    margin-bottom: 20px;
  }

  .welcome-title {
    font-size: 22px;
  }

  .welcome-subtitle {
    font-size: 14px;
    flex-direction: column;
    gap: 6px;
  }

  .action-card {
    padding: 20px 12px;
  }

  .action-icon {
    width: 40px;
    height: 40px;
  }

  .action-icon svg {
    width: 20px;
    height: 20px;
  }

  .action-label {
    font-size: 13px;
  }
}
</style>
