<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { joinClass, getStudentCourseList } from '@/api/course'
import type { StudentCourseItem } from '@/api/course'
import MainLayout from '@/layouts/MainLayout.vue'

const router = useRouter()

const courses = ref<StudentCourseItem[]>([])

onMounted(async () => {
  try {
    courses.value = await getStudentCourseList()
  } catch {
    // 错误由 request 拦截器统一处理
  }
})

const showJoinInput = ref(false)
const joinCode = ref('')

function goToCourse(course: StudentCourseItem) {
  router.push({
    name: 'courseDetail',
    params: { id: course.id },
    query: {
      name: course.name,
      teacher: course.teacherName,
      semester: course.semester,
    },
  })
}

function openJoinDialog() {
  showJoinInput.value = true
  joinCode.value = ''
}

function closeJoinDialog() {
  showJoinInput.value = false
  joinCode.value = ''
}

async function handleJoinCourse() {
  if (!joinCode.value.trim()) {
    ElMessage.warning('请输入班级码')
    return
  }
  try {
    await joinClass({ inviteCode: joinCode.value.trim() })
    ElMessage.success('加入成功')
    closeJoinDialog()
  } catch {
    closeJoinDialog()
  }
}
</script>

<template>
  <MainLayout>
    <div class="course-list-page">
      <!-- 背景装饰 -->
      <div class="bg-grid"></div>
      <div class="floating-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
      </div>

      <div class="content-container">
        <!-- 工具栏 -->
        <div class="toolbar">
          <div></div>
          <button class="join-btn" @click="openJoinDialog">
            <svg viewBox="0 0 20 20" width="16" height="16" class="join-btn-icon">
              <line x1="10" y1="3" x2="10" y2="17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              <line x1="3" y1="10" x2="17" y2="10" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
            <span>加入课程</span>
          </button>
        </div>

        <!-- 课程卡片网格 -->
        <div class="course-grid">
          <div
            v-for="course in courses"
            :key="course.id"
            class="course-card"
            @click="goToCourse(course)"
          >
            <div class="card-bg"></div>

            <!-- 课程图标 -->
            <div class="course-icon">
              <svg viewBox="0 0 48 48" class="icon-svg">
                <defs>
                  <linearGradient :id="'cardGrad-' + course.id" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" style="stop-color:#00d4ff;stop-opacity:1" />
                    <stop offset="100%" style="stop-color:#0066ff;stop-opacity:1" />
                  </linearGradient>
                </defs>
                <rect x="6" y="10" width="36" height="28" rx="3" fill="none" :stroke="'url(#cardGrad-' + course.id + ')'" stroke-width="1.5" />
                <line x1="6" y1="18" x2="42" y2="18" :stroke="'url(#cardGrad-' + course.id + ')'" stroke-width="1" opacity="0.4" />
                <line x1="16" y1="24" x2="36" y2="24" :stroke="'url(#cardGrad-' + course.id + ')'" stroke-width="1" opacity="0.3" />
                <line x1="16" y1="30" x2="30" y2="30" :stroke="'url(#cardGrad-' + course.id + ')'" stroke-width="1" opacity="0.3" />
              </svg>
            </div>

            <!-- 课程信息 -->
            <div class="course-info">
              <h3 class="course-name">{{ course.name }}</h3>
              <div class="course-meta">
                <span class="meta-item">
                  <svg viewBox="0 0 16 16" width="14" height="14" class="meta-icon">
                    <circle cx="8" cy="6" r="2.5" stroke="currentColor" stroke-width="1.2" fill="none"/>
                    <path d="M3 14c0-2.76 2.24-5 5-5s5 2.24 5 5" stroke="currentColor" stroke-width="1.2" fill="none"/>
                  </svg>
                  {{ course.teacherName }}
                </span>
                <span class="meta-item">
                  <svg viewBox="0 0 16 16" width="14" height="14" class="meta-icon">
                    <rect x="2" y="3" width="12" height="11" rx="1" stroke="currentColor" stroke-width="1.2" fill="none"/>
                    <line x1="2" y1="7" x2="14" y2="7" stroke="currentColor" stroke-width="1.2"/>
                    <line x1="7" y1="7" x2="7" y2="14" stroke="currentColor" stroke-width="1.2"/>
                  </svg>
                  {{ course.semester }}
                </span>
              </div>
            </div>

            <!-- 进入箭头 -->
            <div class="card-arrow">
              <svg viewBox="0 0 12 12" width="16" height="16">
                <path d="M4.5 2L8.5 6L4.5 10" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
          </div>
        </div>
      </div>

      <!-- 加入课程弹窗 -->
      <teleport to="body">
        <transition name="dialog">
          <div v-if="showJoinInput" class="dialog-overlay" @click.self="closeJoinDialog">
            <div class="dialog-card">
              <h3 class="dialog-title">加入课程</h3>
              <input
                ref="joinInputRef"
                v-model="joinCode"
                type="text"
                class="dialog-input"
                placeholder="请输入班级码加入课程"
                @keyup.enter="handleJoinCourse"
              />
              <div class="dialog-actions">
                <button class="dialog-btn dialog-btn-cancel" @click="closeJoinDialog">取消</button>
                <button class="dialog-btn dialog-btn-confirm" @click="handleJoinCourse">加入</button>
              </div>
            </div>
          </div>
        </transition>
      </teleport>
    </div>
  </MainLayout>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap');

.course-list-page {
  position: relative;
  min-height: calc(100vh - 64px);
  padding: 40px 20px;
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  overflow: hidden;
}

/* 背景装饰 */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 102, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 102, 255, 0.02) 1px, transparent 1px);
  background-size: 60px 60px;
  pointer-events: none;
}

.floating-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.shape {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.06) 0%, rgba(0, 102, 255, 0.03) 100%);
  backdrop-filter: blur(10px);
}

.shape-1 {
  width: 350px;
  height: 350px;
  top: -120px;
  right: -80px;
  animation: shapeFloat 20s ease-in-out infinite;
}

.shape-2 {
  width: 250px;
  height: 250px;
  bottom: -80px;
  left: -60px;
  animation: shapeFloat 25s ease-in-out infinite reverse;
}

@keyframes shapeFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(25px, -25px) scale(1.05); }
  66% { transform: translate(-15px, 15px) scale(0.95); }
}

/* 内容容器 */
.content-container {
  position: relative;
  z-index: 10;
  max-width: 1200px;
  margin: 0 auto;
}

/* 工具栏 */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.join-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  color: #fff;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.join-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(0, 102, 255, 0.25);
}

.join-btn-icon {
  flex-shrink: 0;
}

/* 加入课程弹窗 */
.dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 30, 60, 0.4);
  backdrop-filter: blur(6px);
}

.dialog-card {
  width: 420px;
  max-width: 90vw;
  padding: 36px 32px 28px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow:
    0 25px 60px rgba(0, 102, 255, 0.15),
    0 10px 30px rgba(0, 102, 255, 0.08);
}

.dialog-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a3a5a;
  margin: 0 0 24px;
  text-align: center;
}

.dialog-input {
  width: 100%;
  padding: 12px 16px;
  background: rgba(0, 102, 255, 0.03);
  border: 1px solid rgba(0, 102, 255, 0.15);
  border-radius: 12px;
  font-size: 15px;
  font-family: inherit;
  color: #1a3a5a;
  outline: none;
  box-sizing: border-box;
  transition: all 0.3s ease;
}

.dialog-input::placeholder {
  color: #b0c8e0;
}

.dialog-input:focus {
  border-color: #0066ff;
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.08);
  background: rgba(255, 255, 255, 0.9);
}

.dialog-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.dialog-btn {
  flex: 1;
  padding: 12px 0;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.3s ease;
}

.dialog-btn-cancel {
  background: rgba(0, 102, 255, 0.06);
  color: #5a7a9a;
}

.dialog-btn-cancel:hover {
  background: rgba(0, 102, 255, 0.1);
}

.dialog-btn-confirm {
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  color: #fff;
}

.dialog-btn-confirm:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(0, 102, 255, 0.25);
}

.dialog-enter-active,
.dialog-leave-active {
  transition: all 0.3s ease;
}

.dialog-enter-from,
.dialog-leave-to {
  opacity: 0;
}

.dialog-enter-from .dialog-card,
.dialog-leave-to .dialog-card {
  transform: translateY(-20px) scale(0.95);
}

/* 课程卡片网格 */
.course-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.course-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 28px 24px;
  background: rgba(255, 255, 255, 0.75);
  border-radius: 20px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow:
    0 8px 24px rgba(0, 102, 255, 0.06),
    0 2px 8px rgba(0, 102, 255, 0.03);
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s ease;
}

.course-card:hover {
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(0, 102, 255, 0.15);
  transform: translateY(-4px);
  box-shadow:
    0 16px 40px rgba(0, 102, 255, 0.12),
    0 4px 16px rgba(0, 102, 255, 0.06);
}

.course-card:hover .card-arrow {
  opacity: 1;
  transform: translateX(0);
}

.course-card:hover .course-icon {
  background: linear-gradient(135deg, rgba(0, 102, 255, 0.15) 0%, rgba(0, 212, 255, 0.12) 100%);
  border-color: rgba(0, 102, 255, 0.2);
}

.card-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.03) 0%, rgba(0, 102, 255, 0.02) 100%);
  pointer-events: none;
}

/* 课程图标 */
.course-icon {
  flex-shrink: 0;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.08) 0%, rgba(0, 102, 255, 0.06) 100%);
  border-radius: 14px;
  border: 1px solid rgba(0, 102, 255, 0.08);
  transition: all 0.3s ease;
}

.icon-svg {
  width: 36px;
  height: 36px;
}

/* 课程信息 */
.course-info {
  flex: 1;
  min-width: 0;
}

.course-name {
  font-size: 17px;
  font-weight: 600;
  color: #1a3a5a;
  margin: 0 0 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #7a9aba;
}

.meta-icon {
  flex-shrink: 0;
}

/* 进入箭头 */
.card-arrow {
  flex-shrink: 0;
  color: #0066ff;
  opacity: 0;
  transform: translateX(-4px);
  transition: all 0.3s ease;
}

/* 响应式 */
@media (max-width: 1000px) {
  .course-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .course-list-page {
    padding: 24px 16px;
  }

  .course-grid {
    grid-template-columns: 1fr;
  }

  .course-card {
    padding: 22px 18px;
  }

  .course-icon {
    width: 48px;
    height: 48px;
  }

  .course-name {
    font-size: 15px;
  }
}
</style>
