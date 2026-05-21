<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Message, Key, User } from '@element-plus/icons-vue'
import { register, sendCode } from '@/api/user'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const codeLoading = ref(false)
const countdown = ref(0)

const registerForm = reactive({
  email: '',
  emailCode: '',
  realName: '',
  password: '',
  confirmPassword: ''
})

// 密码验证
const validatePassword = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请输入密码'))
  } else if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/.test(value)) {
    callback(new Error('密码至少8位，需包含大小写字母和数字'))
  } else {
    if (registerForm.confirmPassword !== '') {
      formRef.value?.validateField('confirmPassword')
    }
    callback()
  }
}

const validateConfirmPassword = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

// 邮箱验证
const validateEmail = (rule: any, value: any, callback: any) => {
  const emailReg = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
  if (!value) {
    callback(new Error('请输入邮箱'))
  } else if (!emailReg.test(value)) {
    callback(new Error('请输入正确的邮箱地址'))
  } else {
    callback()
  }
}

// 姓名验证
const validateName = (rule: any, value: any, callback: any) => {
  if (!value) {
    callback(new Error('请输入真实姓名'))
  } else if (value.length < 2 || value.length > 20) {
    callback(new Error('姓名长度在 2 到 20 个字符'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  email: [
    { validator: validateEmail, trigger: 'blur' }
  ],
  emailCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ],
  realName: [
    { validator: validateName, trigger: 'blur' }
  ],
  password: [
    { validator: validatePassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 发送验证码
const handleSendCode = async () => {
  if (!registerForm.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }

  const emailReg = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
  if (!emailReg.test(registerForm.email)) {
    ElMessage.warning('请输入正确的邮箱地址')
    return
  }

  codeLoading.value = true
  try {
    await sendCode({ email: registerForm.email, type: 'register' })
    ElMessage.success('验证码已发送')
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error: any) {
    ElMessage.error(error.message || '发送失败')
  } finally {
    codeLoading.value = false
  }
}

// 注册
const handleRegister = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await register({
          email: registerForm.email,
          emailCode: registerForm.emailCode,
          realName: registerForm.realName,
          password: registerForm.password
        })

        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } catch (error: any) {
        ElMessage.error(error.message || '注册失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }
  })
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

const goToLogin = () => {
  router.push('/login')
}

onMounted(() => {
  initParticles()
})

onUnmounted(() => {
  particles.value = []
})
</script>

<template>
  <div class="register-page">
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
    <div class="register-container">
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
            开启智能学习新篇章，让知识触手可及
          </p>

          <!-- 步骤指示器 -->
          <div class="steps-indicator">
            <div class="step-item active">
              <div class="step-circle">1</div>
              <span class="step-text">注册账号</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧注册表单 -->
      <div class="form-section">
        <div class="register-card">
          <div class="card-glow"></div>
          <div class="card-content">
            <!-- 注册表单 -->
            <div class="step-content">
              <div class="card-header">
                <h2 class="card-title">注册账号</h2>
                <p class="card-subtitle">注册成功后默认为学生身份</p>
              </div>

              <el-form
                ref="formRef"
                :model="registerForm"
                :rules="rules"
                class="register-form"
                size="large"
                label-position="top"
                hide-required-asterisk
                @submit.prevent
              >
                <el-form-item label="邮箱" prop="email">
                  <el-input
                    v-model="registerForm.email"
                    placeholder="请输入邮箱"
                    :prefix-icon="Message"
                    clearable
                  />
                </el-form-item>

                <el-form-item label="验证码" prop="emailCode">
                  <div class="code-input-wrapper">
                    <el-input
                      v-model="registerForm.emailCode"
                      placeholder="请输入6位验证码"
                      :prefix-icon="Key"
                      maxlength="6"
                      clearable
                    />
                    <el-button
                      type="primary"
                      :loading="codeLoading"
                      :disabled="countdown > 0"
                      class="code-btn"
                      @click="handleSendCode"
                    >
                      {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
                    </el-button>
                  </div>
                </el-form-item>

                <el-form-item label="真实姓名" prop="realName">
                  <el-input
                    v-model="registerForm.realName"
                    placeholder="请输入真实姓名"
                    :prefix-icon="User"
                    clearable
                  />
                </el-form-item>

                <el-form-item label="设置密码" prop="password">
                  <el-input
                    v-model="registerForm.password"
                    type="password"
                    placeholder="请输入密码（至少8位，含大小写字母和数字）"
                    :prefix-icon="Lock"
                    show-password
                  />
                </el-form-item>

                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input
                    v-model="registerForm.confirmPassword"
                    type="password"
                    placeholder="请再次输入密码"
                    :prefix-icon="Lock"
                    show-password
                  />
                </el-form-item>

                <el-form-item>
                  <el-button
                    type="primary"
                    :loading="loading"
                    class="submit-btn"
                    @click="handleRegister"
                  >
                    <span>立即注册</span>
                  </el-button>
                </el-form-item>
              </el-form>
            </div>

            <div class="divider">
              <span>已有账号？</span>
            </div>

            <div class="login-link">
              <el-button
                type="default"
                class="login-btn"
                @click="goToLogin"
              >
                返回登录
              </el-button>
            </div>

            <div class="terms">
              注册即表示您同意我们的
              <a href="javascript:;">服务条款</a>
              和
              <a href="javascript:;">隐私政策</a>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap');

.register-page {
  position: relative;
  width: 100%;
  min-height: 100vh;
  overflow-x: hidden;
  overflow-y: auto;
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 渐变背景 */
.bg-gradient {
  position: fixed;
  inset: 0;
  background: linear-gradient(135deg, #e0f7ff 0%, #b3e5fc 25%, #e1f5fe 50%, #e3f2fd 75%, #f5f9ff 100%);
  background-size: 400% 400%;
  animation: gradientShift 15s ease infinite;
  z-index: 0;
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

/* 网格背景 */
.bg-grid {
  position: fixed;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 102, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 102, 255, 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: gridMove 20s linear infinite;
  z-index: 1;
}

@keyframes gridMove {
  0% { transform: translate(0, 0); }
  100% { transform: translate(50px, 50px); }
}

/* 粒子容器 */
.particles-container {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 2;
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
  position: fixed;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 3;
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
.register-container {
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

/* 步骤指示器 */
.steps-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 20px;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.step-circle {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.7);
  border: 2px solid rgba(0, 102, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  color: #7a9aba;
  transition: all 0.4s ease;
}

.step-item.active .step-circle {
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  border-color: transparent;
  color: white;
  box-shadow: 0 4px 15px rgba(0, 102, 255, 0.4);
}

.step-item.completed .step-circle {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border-color: transparent;
  color: white;
}

.step-circle svg {
  width: 18px;
  height: 18px;
}

.step-text {
  font-size: 13px;
  color: #7a9aba;
  font-weight: 500;
}

.step-item.active .step-text {
  color: #3a5a7a;
}

.step-line {
  width: 50px;
  height: 3px;
  background: rgba(0, 102, 255, 0.15);
  border-radius: 2px;
  margin-bottom: 20px;
  transition: background 0.4s ease;
}

.step-line.active {
  background: linear-gradient(90deg, #10b981, #0066ff);
}

/* 表单区域 */
.form-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.register-card {
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
  padding: 40px 36px;
}

.step-content {
  animation: fadeIn 0.4s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.card-title {
  font-size: 26px;
  font-weight: 600;
  color: #1a3a5a;
  margin-bottom: 8px;
}

.card-subtitle {
  font-size: 14px;
  color: #7a9aba;
}

.register-form {
  margin-bottom: 8px;
}

.register-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.register-form :deep(.el-form-item__label) {
  font-size: 14px;
  font-weight: 500;
  color: #3a5a7a;
  padding-bottom: 8px;
}

.register-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(0, 102, 255, 0.15);
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 102, 255, 0.05);
  transition: all 0.3s ease;
  padding: 4px 14px;
}

.register-form :deep(.el-input__wrapper:hover) {
  border-color: rgba(0, 102, 255, 0.3);
}

.register-form :deep(.el-input__wrapper.is-focus) {
  border-color: #0066ff;
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.1);
}

.register-form :deep(.el-input__inner) {
  font-size: 14px;
  color: #1a3a5a;
}

.register-form :deep(.el-input__inner::placeholder) {
  color: #a0b8d0;
}

.register-form :deep(.el-input__prefix-inner) {
  color: #0066ff;
}

/* 验证码输入 */
.code-input-wrapper {
  display: flex;
  gap: 12px;
  width: 100%;
}

.code-input-wrapper :deep(.el-input) {
  flex: 1;
}

.code-btn {
  flex-shrink: 0;
  height: 40px;
  padding: 0 16px;
  font-size: 13px;
  font-weight: 500;
  border-radius: 10px;
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  border: none;
  white-space: nowrap;
}

.code-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 102, 255, 0.3);
}

.code-btn:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
}

/* 角色选择器 */
.role-selector {
  display: flex;
  gap: 16px;
  width: 100%;
}

.role-option {
  flex: 1;
  position: relative;
  padding: 16px;
  background: rgba(255, 255, 255, 0.9);
  border: 2px solid rgba(0, 102, 255, 0.15);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: center;
}

.role-option:hover {
  border-color: rgba(0, 102, 255, 0.4);
  background: rgba(255, 255, 255, 1);
}

.role-option.active {
  border-color: #0066ff;
  background: linear-gradient(135deg, rgba(0, 102, 255, 0.05) 0%, rgba(0, 212, 255, 0.05) 100%);
  box-shadow: 0 4px 20px rgba(0, 102, 255, 0.15);
}

.role-icon {
  width: 44px;
  height: 44px;
  margin: 0 auto 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.1) 0%, rgba(0, 102, 255, 0.1) 100%);
  border-radius: 12px;
  color: #0066ff;
  transition: all 0.3s ease;
}

.role-option.active .role-icon {
  background: linear-gradient(135deg, #00d4ff 0%, #0066ff 100%);
  color: white;
}

.role-icon svg {
  width: 24px;
  height: 24px;
}

.role-label {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: #1a3a5a;
}

.role-check {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 20px;
  height: 20px;
  background: linear-gradient(135deg, #00d4ff 0%, #0066ff 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.role-check svg {
  width: 12px;
  height: 12px;
}

/* 按钮组 */
.btn-group {
  display: flex;
  gap: 12px;
  width: 100%;
}

.prev-btn {
  flex: 1;
  height: 50px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.9);
  border: 2px solid rgba(0, 102, 255, 0.2);
  color: #0066ff;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.3s ease;
}

.prev-btn:hover {
  border-color: #0066ff;
  background: rgba(0, 102, 255, 0.05);
}

.btn-arrow-left {
  width: 16px;
  height: 16px;
}

.submit-btn {
  flex: 2;
  height: 50px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  border: none;
  box-shadow: 0 10px 30px rgba(0, 102, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 15px 40px rgba(0, 102, 255, 0.4);
}

.btn-arrow {
  width: 18px;
  height: 18px;
  transition: transform 0.3s ease;
}

.submit-btn:hover .btn-arrow {
  transform: translateX(4px);
}

.divider {
  text-align: center;
  margin: 20px 0;
  position: relative;
}

.divider::before,
.divider::after {
  content: '';
  position: absolute;
  top: 50%;
  width: calc(50% - 60px);
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

.login-btn {
  width: 100%;
  height: 50px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: transparent;
  border: 2px solid rgba(0, 102, 255, 0.3);
  color: #0066ff;
  transition: all 0.3s ease;
}

.login-btn:hover {
  background: rgba(0, 102, 255, 0.05);
  border-color: #0066ff;
  transform: translateY(-2px);
}

.terms {
  text-align: center;
  font-size: 12px;
  color: #7a9aba;
  margin-top: 20px;
}

.terms a {
  color: #0066ff;
  text-decoration: none;
}

.terms a:hover {
  text-decoration: underline;
}

/* 响应式 */
@media (max-width: 1024px) {
  .register-container {
    flex-direction: column;
    padding: 20px;
  }

  .brand-section {
    padding: 20px;
  }

  .title-main {
    font-size: 32px;
  }

  .steps-indicator {
    transform: scale(0.9);
  }

  .form-section {
    padding: 20px;
  }

  .card-content {
    padding: 28px 20px;
  }
}

@media (max-width: 480px) {
  .register-page {
    min-height: auto;
    min-height: 100svh;
  }

  .register-container {
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

  .steps-indicator {
    transform: scale(0.8);
  }

  .register-card {
    border-radius: 20px;
  }

  .card-content {
    padding: 24px 16px;
  }

  .card-title {
    font-size: 22px;
  }

  .code-input-wrapper {
    flex-direction: column;
    gap: 10px;
  }

  .code-btn {
    width: 100%;
  }

  .role-selector {
    flex-direction: column;
    gap: 12px;
  }

  .btn-group {
    flex-direction: column-reverse;
  }

  .prev-btn,
  .submit-btn {
    width: 100%;
    flex: none;
  }

  .login-btn {
    height: 46px;
    font-size: 15px;
  }
}
</style>
