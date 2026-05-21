<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/user'
import { useUserStore } from '@/stores/user'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const rememberMe = ref(false)

const loginForm = reactive({
  account: '',
  password: ''
})

const rules: FormRules = {
  account: [
    { required: true, message: '请输入账号（邮箱/学号/工号）', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

// 粒子动画
const particles = ref<Array<{ id: number; x: number; y: number; size: number; delay: number; duration: number }>>([])

const initParticles = () => {
  const count = 50
  for (let i = 0; i < count; i++) {
    particles.value.push({
      id: i,
      x: Math.random() * 100,
      y: Math.random() * 100,
      size: Math.random() * 4 + 1,
      delay: Math.random() * 5,
      duration: Math.random() * 10 + 10
    })
  }
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await login({
          account: loginForm.account,
          password: loginForm.password
        })

        userStore.setToken(res.token)
        userStore.setUserInfo(res)

        ElMessage.success('登录成功')

        if (rememberMe.value) {
          localStorage.setItem('rememberedAccount', loginForm.account)
        } else {
          localStorage.removeItem('rememberedAccount')
        }

        // 按角色跳转
        if (res.roleName === '管理员') {
          router.push('/admin/departments')
        } else if (res.roleName === '教师') {
          router.push('/teacher/courses')
        } else {
          router.push('/')
        }
      } catch (error: any) {
        ElMessage.error(error.message || '登录失败，请检查账号密码')
      } finally {
        loading.value = false
      }
    }
  })
}

const goToRegister = () => {
  router.push('/register')
}

onMounted(() => {
  initParticles()
  const rememberedAccount = localStorage.getItem('rememberedAccount')
  if (rememberedAccount) {
    loginForm.account = rememberedAccount
    rememberMe.value = true
  }
})

onUnmounted(() => {
  particles.value = []
})
</script>

<template>
  <div class="login-page">
    <!-- 动态背景 -->
    <div class="bg-gradient"></div>
    <div class="bg-grid"></div>

    <!-- 浮动粒子 -->
    <div class="particles-container">
      <div
        v-for="particle in particles"
        :key="particle.id"
        class="particle"
        :style="{
          left: particle.x + '%',
          top: particle.y + '%',
          width: particle.size + 'px',
          height: particle.size + 'px',
          animationDelay: particle.delay + 's',
          animationDuration: particle.duration + 's'
        }"
      ></div>
    </div>

    <!-- 浮动几何装饰 -->
    <div class="floating-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
      <div class="shape shape-4"></div>
    </div>

    <!-- 主内容区 -->
    <div class="login-container">
      <!-- 左侧品牌区 -->
      <div class="brand-section">
        <div class="brand-content">
          <div class="logo-wrapper">
            <div class="logo-ring"></div>
            <div class="logo-ring logo-ring-2"></div>
            <div class="logo-core">
              <svg viewBox="0 0 100 100" class="logo-svg">
                <defs>
                  <linearGradient id="logoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" style="stop-color:#00d4ff;stop-opacity:1" />
                    <stop offset="100%" style="stop-color:#0066ff;stop-opacity:1" />
                  </linearGradient>
                </defs>
                <polygon points="50,10 90,30 90,70 50,90 10,70 10,30" fill="none" stroke="url(#logoGradient)" stroke-width="2"/>
                <polygon points="50,25 75,38 75,62 50,75 25,62 25,38" fill="url(#logoGradient)" opacity="0.3"/>
                <circle cx="50" cy="50" r="8" fill="url(#logoGradient)"/>
              </svg>
            </div>
          </div>
          <h1 class="brand-title">
            <span class="title-main">智学平台</span>
            <span class="title-sub">Smart Learning Platform</span>
          </h1>
          <p class="brand-desc">
            融合人工智能技术，打造沉浸式学习体验
          </p>
          <div class="feature-list">
            <div class="feature-item">
              <div class="feature-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 2L2 7l10 5 10-5-10-5z"/>
                  <path d="M2 17l10 5 10-5"/>
                  <path d="M2 12l10 5 10-5"/>
                </svg>
              </div>
              <span>智能课程推荐</span>
            </div>
            <div class="feature-item">
              <div class="feature-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <path d="M12 6v6l4 2"/>
                </svg>
              </div>
              <span>实时学习追踪</span>
            </div>
            <div class="feature-item">
              <div class="feature-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                </svg>
              </div>
              <span>AI 学习助手</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧登录表单 -->
      <div class="form-section">
        <div class="login-card">
          <div class="card-glow"></div>
          <div class="card-content">
            <div class="card-header">
              <h2 class="card-title">欢迎回来</h2>
              <p class="card-subtitle">登录您的账户继续学习之旅</p>
            </div>

            <el-form
              ref="formRef"
              :model="loginForm"
              :rules="rules"
              class="login-form"
              size="large"
              @submit.prevent
            >
              <el-form-item prop="account">
                <el-input
                  v-model="loginForm.account"
                  placeholder="请输入账号（邮箱/学号/工号）"
                  :prefix-icon="User"
                  clearable
                />
              </el-form-item>

              <el-form-item prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  placeholder="请输入密码"
                  :prefix-icon="Lock"
                  show-password
                  @keyup.enter="handleLogin"
                />
              </el-form-item>

              <div class="form-options">
                <el-checkbox v-model="rememberMe" label="记住账号" />
                <router-link to="/forgot-password" class="forgot-link">忘记密码？</router-link>
              </div>

              <el-form-item>
                <el-button
                  type="primary"
                  :loading="loading"
                  class="login-btn"
                  @click="handleLogin"
                >
                  <span v-if="!loading">登 录</span>
                  <span v-else>登录中...</span>
                </el-button>
              </el-form-item>
            </el-form>

            <div class="divider">
              <span>还没有账号？</span>
            </div>

            <div class="register-link">
              <el-button
                type="default"
                class="register-btn"
                @click="goToRegister"
              >
                立即注册
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap');

.login-page {
  position: relative;
  width: 100%;
  min-height: 100vh;
  overflow: hidden;
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 渐变背景 */
.bg-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #e0f7ff 0%, #b3e5fc 25%, #e1f5fe 50%, #e3f2fd 75%, #f5f9ff 100%);
  background-size: 400% 400%;
  animation: gradientShift 15s ease infinite;
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

/* 网格背景 */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 102, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 102, 255, 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: gridMove 20s linear infinite;
}

@keyframes gridMove {
  0% { transform: translate(0, 0); }
  100% { transform: translate(50px, 50px); }
}

/* 粒子容器 */
.particles-container {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.particle {
  position: absolute;
  background: radial-gradient(circle, rgba(0, 180, 255, 0.8) 0%, transparent 70%);
  border-radius: 50%;
  animation: particleFloat linear infinite;
}

@keyframes particleFloat {
  0%, 100% {
    transform: translateY(0) scale(1);
    opacity: 0.3;
  }
  50% {
    transform: translateY(-100px) scale(1.5);
    opacity: 0.8;
  }
}

/* 浮动几何装饰 */
.floating-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.shape {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.1) 0%, rgba(0, 102, 255, 0.05) 100%);
  backdrop-filter: blur(10px);
}

.shape-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  right: -50px;
  animation: shapeFloat1 20s ease-in-out infinite;
}

.shape-2 {
  width: 200px;
  height: 200px;
  bottom: 10%;
  left: -50px;
  animation: shapeFloat2 25s ease-in-out infinite;
}

.shape-3 {
  width: 150px;
  height: 150px;
  top: 60%;
  right: 10%;
  animation: shapeFloat3 18s ease-in-out infinite;
}

.shape-4 {
  width: 100px;
  height: 100px;
  top: 20%;
  left: 30%;
  animation: shapeFloat4 22s ease-in-out infinite;
}

@keyframes shapeFloat1 {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(-50px, 50px) rotate(180deg); }
}

@keyframes shapeFloat2 {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(30px, -40px) rotate(-180deg); }
}

@keyframes shapeFloat3 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-30px, 30px) scale(1.2); }
}

@keyframes shapeFloat4 {
  0%, 100% { transform: translate(0, 0); opacity: 0.5; }
  50% { transform: translate(20px, -20px); opacity: 0.8; }
}

/* 主容器 */
.login-container {
  position: relative;
  z-index: 10;
  display: flex;
  min-height: 100vh;
  padding: 40px;
}

/* 品牌区域 */
.brand-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.brand-content {
  max-width: 480px;
  text-align: center;
}

.logo-wrapper {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto 30px;
}

.logo-ring {
  position: absolute;
  inset: 0;
  border: 2px solid transparent;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.3), rgba(0, 102, 255, 0.3)) padding-box,
              linear-gradient(135deg, #00d4ff, #0066ff) border-box;
  animation: ringPulse 3s ease-in-out infinite;
}

.logo-ring-2 {
  inset: -10px;
  animation-delay: 0.5s;
  opacity: 0.5;
}

@keyframes ringPulse {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.05); opacity: 1; }
}

.logo-core {
  position: absolute;
  inset: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  box-shadow: 0 10px 40px rgba(0, 102, 255, 0.2);
}

.logo-svg {
  width: 60px;
  height: 60px;
}

.brand-title {
  margin-bottom: 16px;
}

.title-main {
  display: block;
  font-size: 42px;
  font-weight: 700;
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 4px;
}

.title-sub {
  display: block;
  font-size: 14px;
  font-weight: 400;
  color: #0066ff;
  letter-spacing: 2px;
  margin-top: 8px;
  opacity: 0.7;
}

.brand-desc {
  font-size: 16px;
  color: #5a7a9a;
  margin-bottom: 40px;
  line-height: 1.8;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(0, 102, 255, 0.1);
  transition: all 0.3s ease;
}

.feature-item:hover {
  transform: translateX(10px);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 10px 30px rgba(0, 102, 255, 0.1);
}

.feature-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #00d4ff 0%, #0066ff 100%);
  border-radius: 8px;
  color: white;
}

.feature-icon svg {
  width: 20px;
  height: 20px;
}

.feature-item span {
  font-size: 15px;
  color: #3a5a7a;
  font-weight: 500;
}

/* 表单区域 */
.form-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-card {
  position: relative;
  width: 100%;
  max-width: 420px;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 24px;
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow:
    0 25px 50px rgba(0, 102, 255, 0.1),
    0 10px 20px rgba(0, 102, 255, 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  overflow: hidden;
}

.card-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at 50% 0%, rgba(0, 212, 255, 0.15) 0%, transparent 50%);
  pointer-events: none;
}

.card-content {
  position: relative;
  padding: 48px 40px;
}

.card-header {
  text-align: center;
  margin-bottom: 36px;
}

.card-title {
  font-size: 28px;
  font-weight: 600;
  color: #1a3a5a;
  margin-bottom: 8px;
}

.card-subtitle {
  font-size: 14px;
  color: #7a9aba;
}

.login-form {
  margin-bottom: 8px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.login-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(0, 102, 255, 0.15);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 102, 255, 0.05);
  transition: all 0.3s ease;
  padding: 4px 16px;
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: rgba(0, 102, 255, 0.3);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: #0066ff;
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.1);
}

.login-form :deep(.el-input__inner) {
  font-size: 15px;
  color: #1a3a5a;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: #a0b8d0;
}

.login-form :deep(.el-input__prefix-inner) {
  color: #0066ff;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.form-options :deep(.el-checkbox__label) {
  color: #5a7a9a;
  font-size: 14px;
}

.form-options :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #0066ff;
  border-color: #0066ff;
}

.forgot-link {
  font-size: 14px;
  color: #0066ff;
  text-decoration: none;
  transition: color 0.3s ease;
}

.forgot-link:hover {
  color: #00d4ff;
}

.login-btn {
  width: 100%;
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  border: none;
  box-shadow: 0 10px 30px rgba(0, 102, 255, 0.3);
  transition: all 0.3s ease;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 15px 40px rgba(0, 102, 255, 0.4);
}

.login-btn:active {
  transform: translateY(0);
}

.divider {
  text-align: center;
  margin: 24px 0;
  position: relative;
}

.divider::before,
.divider::after {
  content: '';
  position: absolute;
  top: 50%;
  width: calc(50% - 70px);
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 102, 255, 0.2));
}

.divider::before {
  left: 0;
}

.divider::after {
  right: 0;
  background: linear-gradient(90deg, rgba(0, 102, 255, 0.2), transparent);
}

.divider span {
  font-size: 14px;
  color: #7a9aba;
  padding: 0 16px;
  background: rgba(255, 255, 255, 0.85);
}

.register-btn {
  width: 100%;
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: transparent;
  border: 2px solid rgba(0, 102, 255, 0.3);
  color: #0066ff;
  transition: all 0.3s ease;
}

.register-btn:hover {
  background: rgba(0, 102, 255, 0.05);
  border-color: #0066ff;
  transform: translateY(-2px);
}

/* 响应式 */
@media (max-width: 1024px) {
  .login-container {
    flex-direction: column;
    padding: 20px;
  }

  .brand-section {
    padding: 20px;
  }

  .title-main {
    font-size: 32px;
  }

  .feature-list {
    display: none;
  }

  .form-section {
    padding: 20px;
  }

  .card-content {
    padding: 32px 24px;
  }
}

@media (max-width: 480px) {
  .login-page {
    min-height: auto;
    min-height: 100svh;
  }

  .login-container {
    padding: 16px;
  }

  .brand-section {
    padding: 16px;
  }

  .logo-wrapper {
    width: 80px;
    height: 80px;
    margin-bottom: 20px;
  }

  .logo-core {
    inset: 10px;
  }

  .logo-svg {
    width: 40px;
    height: 40px;
  }

  .title-main {
    font-size: 28px;
  }

  .title-sub {
    font-size: 12px;
  }

  .brand-desc {
    font-size: 14px;
    margin-bottom: 24px;
  }

  .login-card {
    border-radius: 20px;
  }

  .card-content {
    padding: 24px 20px;
  }

  .card-title {
    font-size: 24px;
  }

  .login-btn,
  .register-btn {
    height: 48px;
    font-size: 15px;
  }
}
</style>
