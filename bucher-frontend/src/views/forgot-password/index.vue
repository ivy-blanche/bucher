<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Message, Key, Check } from '@element-plus/icons-vue'
import { sendCode, resetPassword } from '@/api/user'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const codeLoading = ref(false)
const countdown = ref(0)
const currentStep = ref(1) // 1: 验证邮箱, 2: 重置密码, 3: 完成

const forgotForm = reactive({
  email: '',
  emailCode: '',
  newPassword: '',
  confirmPassword: ''
})

// 密码验证
const validatePassword = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请输入新密码'))
  } else if (value.length < 8) {
    callback(new Error('密码至少8位，需包含大小写字母和数字'))
  } else {
    if (forgotForm.confirmPassword !== '') {
      formRef.value?.validateField('confirmPassword')
    }
    callback()
  }
}

const validateConfirmPassword = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== forgotForm.newPassword) {
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

const rules: FormRules = {
  email: [
    { validator: validateEmail, trigger: 'blur' }
  ],
  emailCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ],
  newPassword: [
    { validator: validatePassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 发送验证码
const handleSendCode = async () => {
  if (!forgotForm.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }

  const emailReg = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
  if (!emailReg.test(forgotForm.email)) {
    ElMessage.warning('请输入正确的邮箱地址')
    return
  }

  codeLoading.value = true
  try {
    await sendCode({ email: forgotForm.email, type: 'reset' })
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

// 下一步
const handleNextStep = async () => {
  if (!formRef.value) return

  await formRef.value.validateField(['email', 'emailCode'], async (valid) => {
    if (valid) {
      currentStep.value = 2
    }
  })
}

// 重置密码
const handleResetPassword = async () => {
  if (!formRef.value) return

  await formRef.value.validateField(['newPassword', 'confirmPassword'], async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await resetPassword({
          email: forgotForm.email,
          emailCode: forgotForm.emailCode,
          newPassword: forgotForm.newPassword
        })
        currentStep.value = 3
      } catch (error: any) {
        ElMessage.error(error.message || '重置失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const goToLogin = () => {
  router.push('/login')
}

// 粒子动画
const particles = ref<Array<{ id: number; x: number; y: number; size: number; delay: number; duration: number }>>([])

const initParticles = () => {
  const count = 40
  for (let i = 0; i < count; i++) {
    particles.value.push({
      id: i,
      x: Math.random() * 100,
      y: Math.random() * 100,
      size: Math.random() * 3 + 1,
      delay: Math.random() * 5,
      duration: Math.random() * 8 + 8
    })
  }
}

onMounted(() => {
  initParticles()
})

onUnmounted(() => {
  particles.value = []
})
</script>

<template>
  <div class="forgot-page">
    <!-- 动态背景 -->
    <div class="bg-mesh"></div>
    <div class="bg-noise"></div>

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

    <!-- 装饰线条 -->
    <div class="decorative-lines">
      <div class="line line-1"></div>
      <div class="line line-2"></div>
      <div class="line line-3"></div>
    </div>

    <!-- 主内容 -->
    <div class="forgot-container">
      <!-- 左侧信息区 -->
      <div class="info-section">
        <div class="info-content">
          <div class="brand-icon">
            <svg viewBox="0 0 100 100" class="icon-svg">
              <defs>
                <linearGradient id="iconGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style="stop-color:#00d4ff;stop-opacity:1" />
                  <stop offset="100%" style="stop-color:#7c3aed;stop-opacity:1" />
                </linearGradient>
              </defs>
              <circle cx="50" cy="50" r="45" fill="none" stroke="url(#iconGradient)" stroke-width="2" stroke-dasharray="8 4"/>
              <circle cx="50" cy="35" r="12" fill="url(#iconGradient)" opacity="0.8"/>
              <path d="M30 75 Q50 55 70 75" fill="none" stroke="url(#iconGradient)" stroke-width="3" stroke-linecap="round"/>
              <circle cx="50" cy="50" r="25" fill="none" stroke="url(#iconGradient)" stroke-width="1.5" opacity="0.5"/>
            </svg>
          </div>
          <h1 class="info-title">找回密码</h1>
          <p class="info-desc">
            别担心，我们会帮您安全地重置密码
          </p>

          <div class="steps-indicator">
            <div class="step" :class="{ active: currentStep >= 1, completed: currentStep > 1 }">
              <div class="step-number">1</div>
              <span>验证邮箱</span>
            </div>
            <div class="step-line" :class="{ active: currentStep > 1 }"></div>
            <div class="step" :class="{ active: currentStep >= 2, completed: currentStep > 2 }">
              <div class="step-number">2</div>
              <span>重置密码</span>
            </div>
            <div class="step-line" :class="{ active: currentStep > 2 }"></div>
            <div class="step" :class="{ active: currentStep >= 3 }">
              <div class="step-number">3</div>
              <span>完成</span>
            </div>
          </div>

          <div class="tips-card">
            <div class="tip-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <path d="M12 16v-4"/>
                <path d="M12 8h.01"/>
              </svg>
            </div>
            <div class="tip-content">
              <h4>安全提示</h4>
              <p>验证码有效期为5分钟，请尽快完成验证</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-section">
        <div class="form-card">
          <!-- 步骤1: 验证邮箱 -->
          <div v-if="currentStep === 1" class="step-content">
            <div class="step-header">
              <div class="step-badge">STEP 01</div>
              <h2 class="step-title">验证邮箱</h2>
              <p class="step-desc">请输入您的邮箱，我们将向您发送验证码</p>
            </div>

            <el-form
              ref="formRef"
              :model="forgotForm"
              :rules="rules"
              class="forgot-form"
              size="large"
              hide-required-asterisk
              @submit.prevent
            >
              <el-form-item prop="email">
                <div class="input-label">邮箱地址</div>
                <el-input
                  v-model="forgotForm.email"
                  placeholder="请输入邮箱"
                  :prefix-icon="Message"
                  clearable
                />
              </el-form-item>

              <el-form-item prop="emailCode">
                <div class="input-label">验证码</div>
                <div class="code-input-wrapper">
                  <el-input
                    v-model="forgotForm.emailCode"
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

              <el-form-item>
                <el-button
                  type="primary"
                  :loading="loading"
                  class="submit-btn"
                  @click="handleNextStep"
                >
                  <span>验证邮箱</span>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="btn-arrow">
                    <path d="M5 12h14M12 5l7 7-7 7"/>
                  </svg>
                </el-button>
              </el-form-item>
            </el-form>

            <div class="back-link">
              <span>想起密码了？</span>
              <a @click="goToLogin">返回登录</a>
            </div>
          </div>

          <!-- 步骤2: 重置密码 -->
          <div v-else-if="currentStep === 2" class="step-content">
            <div class="step-header">
              <div class="step-badge">STEP 02</div>
              <h2 class="step-title">重置密码</h2>
              <p class="step-desc">请设置您的新密码</p>
            </div>

            <el-form
              ref="formRef"
              :model="forgotForm"
              :rules="rules"
              class="forgot-form"
              size="large"
              hide-required-asterisk
              @submit.prevent
            >
              <el-form-item prop="newPassword">
                <div class="input-label">新密码</div>
                <el-input
                  v-model="forgotForm.newPassword"
                  type="password"
                  placeholder="请输入新密码（至少8位，含大小写字母和数字）"
                  :prefix-icon="Lock"
                  show-password
                />
              </el-form-item>

              <el-form-item prop="confirmPassword">
                <div class="input-label">确认密码</div>
                <el-input
                  v-model="forgotForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入新密码"
                  :prefix-icon="Lock"
                  show-password
                />
              </el-form-item>

              <el-form-item>
                <el-button
                  type="primary"
                  :loading="loading"
                  class="submit-btn"
                  @click="handleResetPassword"
                >
                  <span>确认重置</span>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="btn-arrow">
                    <path d="M5 12h14M12 5l7 7-7 7"/>
                  </svg>
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 步骤3: 完成 -->
          <div v-else class="step-content success-content">
            <div class="success-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <path d="M8 12l2.5 2.5L16 9"/>
              </svg>
            </div>
            <h2 class="success-title">密码重置成功</h2>
            <p class="success-desc">您的密码已成功重置，请使用新密码登录</p>
            <el-button
              type="primary"
              class="success-btn"
              @click="goToLogin"
            >
              立即登录
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap');

.forgot-page {
  position: relative;
  width: 100%;
  min-height: 100vh;
  overflow-x: hidden;
  overflow-y: auto;
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 渐变背景 */
.bg-mesh {
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

/* 噪点纹理 */
.bg-noise {
  position: fixed;
  inset: 0;
  opacity: 0.03;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E");
  z-index: 1;
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
  animation: particleDrift linear infinite;
}

@keyframes particleDrift {
  0% {
    transform: translateY(0) translateX(0);
    opacity: 0.2;
  }
  25% {
    opacity: 0.6;
  }
  50% {
    transform: translateY(-80px) translateX(30px);
    opacity: 0.3;
  }
  75% {
    opacity: 0.5;
  }
  100% {
    transform: translateY(-150px) translateX(-20px);
    opacity: 0.1;
  }
}

/* 装饰线条 */
.decorative-lines {
  position: fixed;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 3;
}

.line {
  position: absolute;
  background: linear-gradient(90deg, transparent, rgba(0, 102, 255, 0.2), transparent);
  height: 1px;
  width: 100%;
  transform-origin: left center;
}

.line-1 {
  top: 20%;
  animation: lineMove 20s linear infinite;
}

.line-2 {
  top: 50%;
  animation: lineMove 25s linear infinite reverse;
  background: linear-gradient(90deg, transparent, rgba(0, 102, 255, 0.15), transparent);
}

.line-3 {
  top: 80%;
  animation: lineMove 18s linear infinite;
}

@keyframes lineMove {
  0% { transform: translateX(-100%) scaleX(0.5); opacity: 0; }
  50% { opacity: 1; }
  100% { transform: translateX(100%) scaleX(0.5); opacity: 0; }
}

/* 主容器 */
.forgot-container {
  position: relative;
  z-index: 10;
  display: flex;
  min-height: 100vh;
  padding: 40px;
}

/* 左侧信息区 */
.info-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.info-content {
  max-width: 420px;
  text-align: center;
}

.brand-icon {
  width: 100px;
  height: 100px;
  margin: 0 auto 32px;
  animation: iconFloat 6s ease-in-out infinite;
}

@keyframes iconFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.icon-svg {
  width: 100%;
  height: 100%;
}

.info-title {
  font-size: 36px;
  font-weight: 700;
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 12px;
}

.info-desc {
  font-size: 16px;
  color: #5a7a9a;
  margin-bottom: 48px;
  line-height: 1.6;
}

/* 步骤指示器 */
.steps-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 40px;
  gap: 8px;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.step-number {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(0, 102, 255, 0.1);
  border: 2px solid rgba(0, 102, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: #7a9aba;
  transition: all 0.4s ease;
}

.step span {
  font-size: 12px;
  color: #7a9aba;
  transition: color 0.4s ease;
}

.step.active .step-number {
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  border-color: transparent;
  color: white;
  box-shadow: 0 4px 20px rgba(0, 102, 255, 0.3);
}

.step.active span {
  color: #3a5a7a;
}

.step.completed .step-number {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border-color: transparent;
  color: white;
}

.step-line {
  width: 40px;
  height: 2px;
  background: rgba(0, 102, 255, 0.15);
  margin-bottom: 20px;
  transition: background 0.4s ease;
}

.step-line.active {
  background: linear-gradient(90deg, #0066ff, #00d4ff);
}

/* 提示卡片 */
.tips-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(0, 102, 255, 0.1);
  border-radius: 16px;
  text-align: left;
  backdrop-filter: blur(10px);
}

.tip-icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.15) 0%, rgba(0, 102, 255, 0.15) 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0066ff;
}

.tip-icon svg {
  width: 20px;
  height: 20px;
}

.tip-content h4 {
  font-size: 14px;
  font-weight: 600;
  color: #3a5a7a;
  margin-bottom: 4px;
}

.tip-content p {
  font-size: 13px;
  color: #7a9aba;
  line-height: 1.5;
}

/* 右侧表单区 */
.form-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.form-card {
  position: relative;
  width: 100%;
  max-width: 400px;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(20px);
  padding: 40px;
  box-shadow:
    0 25px 50px rgba(0, 102, 255, 0.1),
    0 10px 20px rgba(0, 102, 255, 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.step-content {
  animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.step-header {
  margin-bottom: 32px;
}

.step-badge {
  display: inline-block;
  padding: 4px 12px;
  background: linear-gradient(135deg, rgba(0, 102, 255, 0.1) 0%, rgba(0, 212, 255, 0.1) 100%);
  border: 1px solid rgba(0, 102, 255, 0.2);
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  color: #0066ff;
  letter-spacing: 1px;
  margin-bottom: 16px;
}

.step-title {
  font-size: 28px;
  font-weight: 600;
  color: #1a3a5a;
  margin-bottom: 8px;
}

.step-desc {
  font-size: 14px;
  color: #7a9aba;
  line-height: 1.6;
}

.forgot-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.input-label {
  font-size: 13px;
  font-weight: 500;
  color: #3a5a7a;
  margin-bottom: 8px;
}

.forgot-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(0, 102, 255, 0.15);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 102, 255, 0.05);
  transition: all 0.3s ease;
  padding: 4px 16px;
}

.forgot-form :deep(.el-input__wrapper:hover) {
  border-color: rgba(0, 102, 255, 0.3);
}

.forgot-form :deep(.el-input__wrapper.is-focus) {
  border-color: #0066ff;
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.1);
}

.forgot-form :deep(.el-input__inner) {
  font-size: 14px;
  color: #1a3a5a;
}

.forgot-form :deep(.el-input__inner::placeholder) {
  color: #a0b8d0;
}

.forgot-form :deep(.el-input__prefix-inner) {
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
  height: 44px;
  padding: 0 16px;
  font-size: 13px;
  font-weight: 500;
  border-radius: 12px;
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  border: none;
  white-space: nowrap;
}

.code-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 15px rgba(0, 102, 255, 0.3);
}

.code-btn:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
  opacity: 0.6;
}

.submit-btn {
  width: 100%;
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 8px;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 102, 255, 0.35);
}

.btn-arrow {
  width: 18px;
  height: 18px;
  transition: transform 0.3s ease;
}

.submit-btn:hover .btn-arrow {
  transform: translateX(4px);
}

.back-link {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #7a9aba;
}

.back-link a {
  color: #0066ff;
  cursor: pointer;
  margin-left: 4px;
  transition: color 0.3s ease;
}

.back-link a:hover {
  color: #00d4ff;
}

/* 成功页面 */
.success-content {
  text-align: center;
  padding: 20px 0;
}

.success-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.15) 0%, rgba(16, 185, 129, 0.05) 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #10b981;
  animation: successPulse 2s ease-in-out infinite;
}

@keyframes successPulse {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 0 0 20px rgba(16, 185, 129, 0);
  }
}

.success-icon svg {
  width: 40px;
  height: 40px;
}

.success-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a3a5a;
  margin-bottom: 8px;
}

.success-desc {
  font-size: 14px;
  color: #7a9aba;
  margin-bottom: 32px;
  line-height: 1.6;
}

.success-btn {
  width: 100%;
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border: none;
}

.success-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(16, 185, 129, 0.35);
}

/* 响应式 */
@media (max-width: 1024px) {
  .forgot-container {
    flex-direction: column;
    padding: 20px;
  }

  .info-section {
    padding: 20px;
  }

  .info-title {
    font-size: 28px;
  }

  .tips-card {
    display: none;
  }

  .form-section {
    padding: 20px;
  }

  .form-card {
    padding: 32px 24px;
  }
}

@media (max-width: 480px) {
  .forgot-page {
    min-height: auto;
    min-height: 100svh;
  }

  .forgot-container {
    padding: 16px;
  }

  .brand-icon {
    width: 70px;
    height: 70px;
  }

  .info-title {
    font-size: 24px;
  }

  .info-desc {
    font-size: 14px;
    margin-bottom: 32px;
  }

  .steps-indicator {
    transform: scale(0.85);
    gap: 4px;
  }

  .step-line {
    width: 24px;
  }

  .form-card {
    padding: 24px 16px;
    border-radius: 20px;
  }

  .step-title {
    font-size: 24px;
  }

  .code-input-wrapper {
    flex-direction: column;
    gap: 10px;
  }

  .code-btn {
    width: 100%;
  }

  .submit-btn,
  .success-btn {
    height: 48px;
    font-size: 15px;
  }
}
</style>
