<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import TeacherLayout from '@/layouts/TeacherLayout.vue'
import { createCourse, getCourseList, type CourseInfo } from '@/api/course'

const router = useRouter()

interface CourseItem {
  id: number
  courseCode: string
  name: string
  semester: string
}

const courses = ref<CourseItem[]>([])
const loading = ref(true)

// 新建课程弹窗
const showDialog = ref(false)
const submitting = ref(false)
const form = ref({
  name: '',
  semester: '',
  description: '',
})

function openCreateDialog() {
  form.value = { name: '', semester: '', description: '' }
  showDialog.value = true
}

function closeCreateDialog() {
  showDialog.value = false
  form.value = { name: '', semester: '', description: '' }
}

async function fetchCourses() {
  loading.value = true
  try {
    courses.value = await getCourseList()
  } catch {
    // 错误已由拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入课程名称')
    return
  }
  if (!form.value.semester.trim()) {
    ElMessage.warning('请输入开设学期')
    return
  }

  submitting.value = true
  try {
    const data: CourseInfo = await createCourse({
      name: form.value.name.trim(),
      semester: form.value.semester.trim(),
      description: form.value.description.trim() || undefined,
    })
    // 将新课程插入列表头部
    courses.value.unshift({
      id: data.id,
      courseCode: data.courseCode,
      name: data.name,
      semester: data.semester,
    })
    ElMessage.success('课程创建成功')
    closeCreateDialog()
  } catch {
    // 错误已由拦截器统一处理
  } finally {
    submitting.value = false
  }
}

function goToCourse(course: CourseItem) {
  router.push({
    name: 'teacherCourseDetail',
    params: { id: course.id },
    query: {
      name: course.name,
      courseCode: course.courseCode,
      semester: course.semester,
    },
  })
}

onMounted(() => {
  fetchCourses()
})
</script>

<template>
  <TeacherLayout>
    <div class="page-container">
      <!-- 工具栏 -->
      <div class="toolbar">
        <h1 class="page-title">课程管理</h1>
        <button class="create-btn" @click="openCreateDialog">
          <svg viewBox="0 0 20 20" width="16" height="16" class="btn-icon">
            <line x1="10" y1="3" x2="10" y2="17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            <line x1="3" y1="10" x2="17" y2="10" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <span>新建课程</span>
        </button>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="empty-state">
        <p class="empty-text">加载中...</p>
      </div>

      <!-- 课程卡片网格 -->
      <div v-else-if="courses.length > 0" class="course-grid">
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
                  <rect x="2" y="3" width="12" height="11" rx="1" stroke="currentColor" stroke-width="1.2" fill="none"/>
                  <line x1="2" y1="7" x2="14" y2="7" stroke="currentColor" stroke-width="1.2"/>
                  <line x1="7" y1="7" x2="7" y2="14" stroke="currentColor" stroke-width="1.2"/>
                </svg>
                {{ course.semester }}
              </span>
              <span class="meta-item">
                <svg viewBox="0 0 16 16" width="14" height="14" class="meta-icon">
                  <rect x="1" y="4" width="14" height="9" rx="1.5" stroke="currentColor" stroke-width="1.2" fill="none"/>
                  <line x1="1" y1="8" x2="15" y2="8" stroke="currentColor" stroke-width="1.2"/>
                  <text x="8" y="7" text-anchor="middle" font-size="4" fill="currentColor" font-weight="bold">★</text>
                </svg>
                {{ course.courseCode }}
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

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <svg viewBox="0 0 80 80" width="80" height="80" class="empty-icon">
          <rect x="10" y="15" width="60" height="50" rx="4" fill="none" stroke="#b0c8e0" stroke-width="2"/>
          <line x1="10" y1="28" x2="70" y2="28" stroke="#b0c8e0" stroke-width="1.5"/>
          <line x1="26" y1="38" x2="60" y2="38" stroke="#b0c8e0" stroke-width="1.5" opacity="0.5"/>
          <line x1="26" y1="48" x2="50" y2="48" stroke="#b0c8e0" stroke-width="1.5" opacity="0.5"/>
        </svg>
        <p class="empty-text">还没有课程，点击上方按钮创建第一门课程</p>
      </div>
    </div>

    <!-- 新建课程弹窗 -->
    <teleport to="body">
      <transition name="dialog">
        <div v-if="showDialog" class="dialog-overlay" @click.self="closeCreateDialog">
          <div class="dialog-card">
            <h3 class="dialog-title">新建课程</h3>

            <div class="form-group">
              <label class="form-label">
                课程名称 <span class="required">*</span>
              </label>
              <input
                v-model="form.name"
                type="text"
                class="form-input"
                placeholder="例：Java程序设计"
                maxlength="100"
              />
            </div>

            <div class="form-group">
              <label class="form-label">
                开设学期 <span class="required">*</span>
              </label>
              <input
                v-model="form.semester"
                type="text"
                class="form-input"
                placeholder="例：2025-2026-2"
              />
              <p class="form-tip">格式示例：2025-2026-1（第一学期）/ 2025-2026-2（第二学期）</p>
            </div>

            <div class="form-group">
              <label class="form-label">课程描述</label>
              <textarea
                v-model="form.description"
                class="form-textarea"
                placeholder="选填，简要描述课程内容和目标"
                rows="3"
                maxlength="500"
              ></textarea>
            </div>

            <div class="dialog-actions">
              <button class="dialog-btn dialog-btn-cancel" @click="closeCreateDialog">取消</button>
              <button
                class="dialog-btn dialog-btn-confirm"
                :disabled="submitting"
                @click="handleCreate"
              >
                {{ submitting ? '创建中...' : '确认创建' }}
              </button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </TeacherLayout>
</template>

<style scoped>
.page-container {
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

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a3a5a;
  margin: 0;
}

.create-btn {
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
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.create-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(0, 102, 255, 0.25);
}

.btn-icon {
  flex-shrink: 0;
}

/* 课程卡片网格 — 三列 */
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
  gap: 12px;
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

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-icon {
  margin-bottom: 20px;
}

.empty-text {
  font-size: 15px;
  color: #7a9aba;
  margin: 0;
}

/* ===== 新建课程弹窗 ===== */
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
  width: 480px;
  max-width: 90vw;
  max-height: 90vh;
  overflow-y: auto;
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
  margin: 0 0 28px;
  text-align: center;
}

/* 表单 */
.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #3a5a7a;
  margin-bottom: 8px;
}

.required {
  color: #ff4d4f;
}

.form-input {
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

.form-input::placeholder {
  color: #b0c8e0;
}

.form-input:focus {
  border-color: #0066ff;
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.08);
  background: rgba(255, 255, 255, 0.9);
}

.form-textarea {
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
  resize: vertical;
  min-height: 80px;
  transition: all 0.3s ease;
}

.form-textarea::placeholder {
  color: #b0c8e0;
}

.form-textarea:focus {
  border-color: #0066ff;
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.08);
  background: rgba(255, 255, 255, 0.9);
}

.form-tip {
  font-size: 12px;
  color: #b0c8e0;
  margin: 6px 0 0;
}

.dialog-actions {
  display: flex;
  gap: 12px;
  margin-top: 28px;
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

.dialog-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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

.dialog-btn-confirm:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(0, 102, 255, 0.25);
}

/* 弹窗动画 */
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

/* 响应式 */
@media (max-width: 1000px) {
  .course-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
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

  .dialog-card {
    padding: 28px 20px 24px;
  }
}
</style>
