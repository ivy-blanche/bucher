<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import MainLayout from '@/layouts/MainLayout.vue'
import { getExamDoData, saveExamAnswer, submitExam } from '@/api/exam'
import type { ExamQuestionItem } from '@/api/exam'
import { useAIStore } from '@/stores/ai'
import AIChat from '@/components/AIChat.vue'

const route = useRoute()
const router = useRouter()
const examId = route.params.id as string
const aiStore = useAIStore()

const aiSidebarVisible = ref(false)
const courseIdFromQuery = computed(() => route.query.courseId as string || '')
const courseNameFromQuery = computed(() => route.query.courseName as string || '')

function toggleAISidebar() {
  aiSidebarVisible.value = !aiSidebarVisible.value
  if (aiSidebarVisible.value && courseIdFromQuery.value) {
    aiStore.initCourse(courseIdFromQuery.value, courseNameFromQuery.value)
    aiStore.setFullMode()
  }
}

// ==================== 页面数据 ====================
interface ExamPageData {
  examId: number
  title: string
  description: string
  courseName: string
  totalScore: number
  duration: number
  earlySubmitMinutes: number
  startTime: string
  endTime: string
  status: number
  submitted: boolean
  remainingSeconds: number
  questions: ExamQuestionItem[]
}

const loading = ref(true)
const examData = reactive<ExamPageData>({
  examId: 0,
  title: '',
  description: '',
  courseName: '',
  totalScore: 0,
  duration: 0,
  earlySubmitMinutes: 0,
  startTime: '',
  endTime: '',
  status: 0,
  submitted: false,
  remainingSeconds: 0,
  questions: [],
})

const currentIndex = ref(0)
const current = computed(() => examData.questions[currentIndex.value] || null)

const sidebarCollapsed = ref(false)
const lastSavedAnswer = ref<string | null>(null)
const answerDirty = ref(false)

/** 倒计时 */
const remainingSeconds = ref(0)
let timerInterval: number | null = null
const timeUp = ref(false)

/** 是否只读 */
const isReadonly = computed(() => {
  return examData.submitted || examData.status === 3 || timeUp.value || examData.status === 0
})

/** 考试尚未开始 */
const notStarted = computed(() => examData.status === 0 && !examData.submitted)

// ==================== 题型 ====================
const typeNames: Record<string, string> = {
  single: '单选题',
  multiple: '多选题',
  judge: '判断题',
  fill: '填空题',
  'short-answer': '简答题',
  programming: '编程题',
}

const typeColors: Record<string, string> = {
  single: '#0066ff',
  multiple: '#9c27b0',
  judge: '#ff9800',
  fill: '#00c853',
  'short-answer': '#e53935',
  programming: '#16a34a',
}

// ==================== 倒计时 ====================
function formatRemaining(seconds: number): string {
  if (seconds <= 0) return '00:00:00'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const remainingDisplay = computed(() => formatRemaining(remainingSeconds.value))

const countdownUrgent = computed(() => {
  return remainingSeconds.value > 0 && remainingSeconds.value < 300
})

const canEarlySubmit = computed(() => {
  if (examData.earlySubmitMinutes <= 0) return true
  const elapsed = examData.duration * 60 - remainingSeconds.value
  return elapsed >= examData.earlySubmitMinutes * 60
})

// ==================== 作答 ====================
function isAnswered(q: ExamQuestionItem): boolean {
  return q.answer !== null && q.answer !== ''
}

function isMultiSelected(q: ExamQuestionItem, label: string): boolean {
  return (q.answer || '').includes(label)
}

const answeredCount = computed(() => examData.questions.filter(isAnswered).length)

function selectSingle(q: ExamQuestionItem, label: string) {
  if (isReadonly.value) return
  q.answer = label
  answerDirty.value = true
}

function toggleMulti(q: ExamQuestionItem, label: string) {
  if (isReadonly.value) return
  const set = new Set((q.answer || '').split(''))
  if (set.has(label)) set.delete(label)
  else set.add(label)
  q.answer = [...set].sort().join('')
  answerDirty.value = true
}

function markDirty() {
  answerDirty.value = true
}

// ==================== 保存 ====================
async function saveCurrentAnswer(): Promise<void> {
  const q = current.value
  if (!q || isReadonly.value) return
  if (!answerDirty.value) return
  try {
    await saveExamAnswer({
      examId: examData.examId,
      questionId: q.questionId,
      answer: q.answer ?? '',
    })
    syncLastSaved()
  } catch {
    throw new Error('答案暂存失败')
  }
}

function syncLastSaved() {
  const q = examData.questions[currentIndex.value]
  lastSavedAnswer.value = q ? q.answer : null
  answerDirty.value = false
}

// ==================== 导航 ====================
async function goToQuestion(index: number) {
  if (index === currentIndex.value) return
  try { await saveCurrentAnswer() } catch { ElMessage.warning('答案暂存失败，请重试') }
  currentIndex.value = index
  syncLastSaved()
}

async function prevQuestion() {
  if (currentIndex.value <= 0) return
  try { await saveCurrentAnswer() } catch { ElMessage.warning('答案暂存失败，请重试') }
  currentIndex.value--
  syncLastSaved()
}

async function nextQuestion() {
  if (currentIndex.value >= examData.questions.length - 1) return
  try { await saveCurrentAnswer() } catch { ElMessage.warning('答案暂存失败，请重试') }
  currentIndex.value++
  syncLastSaved()
}

// ==================== 提交 ====================
const submitting = ref(false)

async function handleSubmit() {
  if (!canEarlySubmit.value) {
    ElMessage.warning(`开考 ${examData.earlySubmitMinutes} 分钟后才能交卷`)
    return
  }

  try { await saveCurrentAnswer() } catch { /* continue */ }

  const unanswered = examData.questions.filter(q => !isAnswered(q))
  try {
    await ElMessageBox.confirm(
      unanswered.length > 0
        ? `还有 ${unanswered.length} 道题未作答，确定要提交吗？`
        : '所有题目已作答，确认提交？',
      '确认提交',
      {
        confirmButtonText: '确定提交',
        cancelButtonText: unanswered.length > 0 ? '继续作答' : '再看看',
        type: unanswered.length > 0 ? 'warning' : 'info',
      },
    )
  } catch {
    return
  }

  submitting.value = true
  try {
    await submitExam({ examId: examData.examId })
    ElMessage.success('考试提交成功！')
    if (timerInterval) clearInterval(timerInterval)
    router.back()
  } catch {
    submitting.value = false
  }
}

// ==================== 时间到 ====================
async function autoSubmitWhenTimeUp() {
  if (examData.submitted || timeUp.value) return
  timeUp.value = true
  if (timerInterval) clearInterval(timerInterval)
  try {
    await submitExam({ examId: examData.examId })
    examData.submitted = true
    ElMessage.info('考试时间到，已自动交卷')
  } catch {
    ElMessage.info('考试时间到')
  }
}

function goBack() {
  router.back()
}

// ==================== 生命周期 ====================
onMounted(async () => {
  loading.value = true
  try {
    const res = await getExamDoData(examId)
    Object.assign(examData, res)
    examData.questions.sort((a, b) => a.sortOrder - b.sortOrder)
    remainingSeconds.value = res.remainingSeconds
    syncLastSaved()

    // 进行中 → 启动倒计时
    if (res.status === 1 && res.remainingSeconds > 0 && !res.submitted) {
      timerInterval = window.setInterval(() => {
        if (remainingSeconds.value > 0) {
          remainingSeconds.value--
        } else {
          autoSubmitWhenTimeUp()
        }
      }, 1000)
    }
  } catch {
    // 错误由拦截器统一处理
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval)
})
</script>

<template>
  <MainLayout>
    <!-- 加载中 -->
    <div v-if="loading" class="loading-full">
      <div class="loading-spinner" />
      <p>加载中...</p>
    </div>

    <template v-else>
      <div class="exam-container">
        <!-- 左侧题目导航 -->
        <aside class="exam-sidebar" :class="{ collapsed: sidebarCollapsed }">
          <template v-if="!sidebarCollapsed">
            <div class="sidebar-header">
              <button class="back-btn" @click="goBack">
                <svg viewBox="0 0 16 16" width="16" height="16">
                  <polyline points="10,3 5,8 10,13" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span>返回</span>
              </button>
              <h3 class="sidebar-title">{{ examData.title }}</h3>
              <p class="sidebar-course">{{ examData.courseName }}</p>
              <p v-if="examData.description" class="sidebar-desc">{{ examData.description }}</p>
              <p class="sidebar-progress">
                已答 <strong>{{ answeredCount }}</strong> / {{ examData.questions.length }} 题
              </p>
              <p class="sidebar-score">总分: {{ examData.totalScore }} 分</p>
            </div>

            <div class="question-grid">
              <button
                v-for="(q, idx) in examData.questions"
                :key="q.questionId"
                class="q-num-btn"
                :class="{ active: idx === currentIndex, answered: isAnswered(q) }"
                @click="goToQuestion(idx)"
              >
                {{ idx + 1 }}
              </button>
            </div>

            <div class="sidebar-footer">
              <div v-if="examData.submitted" class="sidebar-status submitted-status">已提交</div>
              <div v-else-if="timeUp" class="sidebar-status timeup-status">时间到</div>
              <div v-else-if="notStarted" class="sidebar-status notstarted-status">未开始</div>
              <button
                v-if="courseIdFromQuery"
                class="ai-sidebar-btn"
                :class="{ active: aiSidebarVisible }"
                @click="toggleAISidebar"
              >
                <svg viewBox="0 0 20 20" width="16" height="16">
                  <circle cx="10" cy="10" r="8"/>
                  <path d="M10 5v5l3 1.5" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M6 12.5c0 1.2 1.5 2.5 4 2.5s4-1.3 4-2.5" stroke-linecap="round"/>
                </svg>
                <span>{{ aiSidebarVisible ? '关闭 AI' : 'AI 助教' }}</span>
              </button>
            </div>
          </template>
        </aside>

        <button class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed" :title="sidebarCollapsed ? '展开题目列表' : '收起题目列表'">
          <svg v-if="!sidebarCollapsed" viewBox="0 0 16 16" width="14" height="14">
            <line x1="4" y1="3" x2="4" y2="13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            <line x1="12" y1="3" x2="12" y2="13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          <svg v-else viewBox="0 0 16 16" width="14" height="14">
            <line x1="3" y1="4" x2="13" y2="4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            <line x1="3" y1="8" x2="13" y2="8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            <line x1="3" y1="12" x2="13" y2="12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </button>

        <!-- 右侧答题区域 -->
        <main class="exam-main">
          <!-- 状态条 -->
          <div v-if="timeUp" class="status-banner timeup-banner">
            <svg viewBox="0 0 16 16" width="16" height="16">
              <circle cx="8" cy="8" r="7" fill="none" stroke="currentColor" stroke-width="1.2"/>
              <line x1="5" y1="5" x2="11" y2="11" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
              <line x1="11" y1="5" x2="5" y2="11" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
            </svg>
            考试时间到{{ examData.submitted ? '，已自动交卷' : '' }}
          </div>
          <div v-else-if="examData.submitted" class="status-banner submitted-banner">
            <svg viewBox="0 0 16 16" width="16" height="16">
              <circle cx="8" cy="8" r="7" fill="none" stroke="currentColor" stroke-width="1.2"/>
              <polyline points="5,8 7,10 11,5" stroke="currentColor" stroke-width="1.4" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            已提交，仅可查看
          </div>
          <div v-else-if="notStarted" class="status-banner notstarted-banner">
            <svg viewBox="0 0 16 16" width="16" height="16">
              <circle cx="8" cy="8" r="7" fill="none" stroke="currentColor" stroke-width="1.2"/>
              <polyline points="8,4.5 8,8 11,9.5" stroke="currentColor" stroke-width="1.2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            考试尚未开始，仅可预览题目
          </div>
          <div v-else class="countdown-banner" :class="{ urgent: countdownUrgent }">
            <svg viewBox="0 0 16 16" width="16" height="16">
              <circle cx="8" cy="8" r="6.5" fill="none" stroke="currentColor" stroke-width="1.3"/>
              <polyline points="8,4 8,8 11,9.5" stroke="currentColor" stroke-width="1.3" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            剩余时间: <strong>{{ remainingDisplay }}</strong>
            <span v-if="!canEarlySubmit" class="early-hint">
              &nbsp;· 开考 {{ examData.earlySubmitMinutes }} 分钟后可交卷
            </span>
          </div>

          <!-- 无题目 -->
          <div v-if="!current" class="empty-state">
            <p>暂无题目数据</p>
          </div>

          <template v-else>
            <!-- 编程题 -->
            <div v-if="current.questionType === 'programming'" :key="current.questionId" class="question-card programming-card">
              <div class="q-header">
                <span class="q-type-badge" :style="{ background: typeColors[current.questionType] + '14', color: typeColors[current.questionType] }">
                  {{ typeNames[current.questionType] }}
                </span>
                <span class="q-score">{{ current.score }} 分</span>
                <span v-if="currentAnswered" class="q-answered-tag">已作答</span>
              </div>
              <div class="q-stem">
                <span class="q-number">{{ currentIndex + 1 }}.</span>
                <span v-html="current.content" />
              </div>
              <div class="code-area">
                <textarea
                  :value="current.answer ?? ''"
                  @input="e => { current.answer = (e.target as HTMLTextAreaElement).value; markDirty() }"
                  class="code-textarea"
                  placeholder="在此编写代码..."
                  :readonly="isReadonly"
                  spellcheck="false"
                />
              </div>
              <div class="q-footer">
                <button class="nav-btn nav-prev" :disabled="currentIndex === 0" @click="prevQuestion">
                  <svg viewBox="0 0 14 14" width="12" height="12">
                    <polyline points="9,3 5,7 9,11" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                  上一题
                </button>
                <div class="q-footer-center">
                  <span class="q-position">{{ currentIndex + 1 }} / {{ examData.questions.length }}</span>
                </div>
                <button
                  v-if="currentIndex === examData.questions.length - 1 && !isReadonly"
                  class="nav-btn nav-submit"
                  :disabled="submitting"
                  @click="handleSubmit"
                >
                  {{ submitting ? '提交中...' : '交卷' }}
                </button>
                <button
                  v-else-if="currentIndex < examData.questions.length - 1 && !isReadonly"
                  class="nav-btn nav-next"
                  @click="nextQuestion"
                >
                  下一题
                  <svg viewBox="0 0 14 14" width="12" height="12">
                    <polyline points="5,3 9,7 5,11" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </button>
              </div>
            </div>

            <!-- 其他题型 -->
            <div v-else class="question-card">
              <div class="q-header">
                <span
                  class="q-type-badge"
                  :style="{ background: typeColors[current.questionType] + '14', color: typeColors[current.questionType] }"
                >
                  {{ typeNames[current.questionType] }}
                </span>
                <span class="q-score">{{ current.score }} 分</span>
                <span v-if="currentAnswered" class="q-answered-tag">已作答</span>
              </div>

              <div class="q-stem">
                <span class="q-number">{{ currentIndex + 1 }}.</span>
                <span v-html="current.content" />
              </div>

              <div class="q-options">
                <!-- 单选 / 判断 -->
                <template v-if="(current.questionType === 'single' || current.questionType === 'judge') && current.options">
                  <div
                    v-for="opt in current.options"
                    :key="opt.label"
                    class="option-item"
                    :class="{ selected: current.answer === opt.label }"
                    @click="selectSingle(current, opt.label)"
                  >
                    <span class="option-radio" :class="{ checked: current.answer === opt.label }">
                      <span v-if="current.answer === opt.label" class="radio-dot" />
                    </span>
                    <span class="option-label">{{ opt.label }}</span>
                    <span class="option-content">{{ opt.content }}</span>
                  </div>
                </template>

                <!-- 多选 -->
                <template v-else-if="current.questionType === 'multiple' && current.options">
                  <div
                    v-for="opt in current.options"
                    :key="opt.label"
                    class="option-item"
                    :class="{ selected: isMultiSelected(current, opt.label) }"
                    @click="toggleMulti(current, opt.label)"
                  >
                    <span class="option-checkbox" :class="{ checked: isMultiSelected(current, opt.label) }">
                      <svg v-if="isMultiSelected(current, opt.label)" viewBox="0 0 12 12" width="10" height="10">
                        <polyline points="2,6 5,9 10,3" stroke="#fff" stroke-width="1.6" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                      </svg>
                    </span>
                    <span class="option-label">{{ opt.label }}</span>
                    <span class="option-content">{{ opt.content }}</span>
                  </div>
                </template>

                <!-- 填空 -->
                <div v-else-if="current.questionType === 'fill'" class="fill-area">
                  <input
                    :value="current.answer ?? ''"
                    @input="e => { current.answer = (e.target as HTMLInputElement).value; markDirty() }"
                    type="text"
                    class="fill-input"
                    placeholder="请输入答案..."
                    :readonly="isReadonly"
                  />
                </div>

                <!-- 简答 -->
                <div v-else-if="current.questionType === 'short-answer'" class="fill-area">
                  <textarea
                    :value="current.answer ?? ''"
                    @input="e => { current.answer = (e.target as HTMLTextAreaElement).value; markDirty() }"
                    class="sa-textarea"
                    placeholder="请在此输入你的回答..."
                    rows="6"
                    :readonly="isReadonly"
                  />
                </div>
              </div>

              <div class="q-footer">
                <button
                  class="nav-btn nav-prev"
                  :disabled="currentIndex === 0"
                  @click="prevQuestion"
                >
                  <svg viewBox="0 0 14 14" width="12" height="12">
                    <polyline points="9,3 5,7 9,11" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                  上一题
                </button>

                <div class="q-footer-center">
                  <span class="q-position">{{ currentIndex + 1 }} / {{ examData.questions.length }}</span>
                </div>

                <button
                  v-if="currentIndex === examData.questions.length - 1 && !isReadonly"
                  class="nav-btn nav-submit"
                  :disabled="submitting"
                  @click="handleSubmit"
                >
                  <svg viewBox="0 0 14 14" width="12" height="12">
                    <polyline points="2,7 5,10 12,3" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                  {{ submitting ? '提交中...' : '交卷' }}
                </button>

                <button
                  v-else-if="currentIndex < examData.questions.length - 1 && !isReadonly"
                  class="nav-btn nav-next"
                  @click="nextQuestion"
                >
                  下一题
                  <svg viewBox="0 0 14 14" width="12" height="12">
                    <polyline points="5,3 9,7 5,11" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </button>
              </div>
            </div>
          </template>
        </main>

        <!-- AI 助教侧边栏 -->
        <AIChat
          v-if="courseIdFromQuery"
          mode="embedded"
          :visible="aiSidebarVisible"
          :course-id="courseIdFromQuery"
          :course-name="courseNameFromQuery"
          @update:visible="aiSidebarVisible = $event"
        />
      </div>
    </template>
  </MainLayout>
</template>

<style scoped>
.exam-container {
  display: flex;
  height: calc(100vh - 64px);
  background: linear-gradient(135deg, #e0f7ff 0%, #b3e5fc 25%, #e1f5fe 50%, #e3f2fd 75%, #f5f9ff 100%);
}

/* ==================== 加载 ==================== */
.loading-full {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: calc(100vh - 64px);
  gap: 16px;
  color: #7a9aba;
  font-size: 15px;
}

.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid rgba(0, 102, 255, 0.12);
  border-top-color: #0066ff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ==================== 左侧边栏 ==================== */
.exam-sidebar {
  width: 200px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-right: 1px solid rgba(0, 102, 255, 0.06);
  box-shadow: 2px 0 12px rgba(0, 102, 255, 0.04);
  position: relative;
  transition: width 0.25s ease;
}

.exam-sidebar.collapsed {
  width: 0;
  border-right: none;
}

.sidebar-toggle {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 28px;
  margin-top: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-left: none;
  border-radius: 0 6px 6px 0;
  background: #fff;
  color: rgba(0, 0, 0, 0.25);
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 0;
}

.sidebar-toggle:hover {
  color: #0066ff;
  background: #f0f4ff;
}

.sidebar-header {
  padding: 20px 16px 16px;
  border-bottom: 1px solid rgba(0, 102, 255, 0.06);
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid rgba(0, 102, 255, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.5);
  color: #0066ff;
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 12px;
}

.back-btn:hover {
  background: rgba(0, 102, 255, 0.06);
  border-color: rgba(0, 102, 255, 0.25);
}

.sidebar-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a3a5a;
  line-height: 1.4;
  margin: 0 0 4px;
}

.sidebar-course {
  font-size: 12px;
  color: #7a9aba;
  margin: 0 0 4px;
}

.sidebar-desc {
  font-size: 12px;
  color: #a0b8d0;
  margin: 0 0 8px;
  line-height: 1.5;
}

.sidebar-progress {
  font-size: 13px;
  color: #7a9aba;
  margin: 8px 0 2px;
}

.sidebar-progress strong {
  color: #0066ff;
}

.sidebar-score {
  font-size: 12px;
  color: #a0b8d0;
  margin: 0;
}

.question-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding: 16px;
  overflow-y: auto;
  align-content: start;
}

.q-num-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  aspect-ratio: 1;
  border: 1.5px solid rgba(0, 102, 255, 0.10);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.5);
  color: #4a6a8a;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
}

.q-num-btn:hover {
  border-color: rgba(0, 102, 255, 0.25);
  background: rgba(255, 255, 255, 0.8);
  color: #0066ff;
}

.q-num-btn.active {
  border-color: #0066ff;
  background: rgba(0, 102, 255, 0.08);
  color: #0066ff;
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.10);
}

.q-num-btn.answered {
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 3px 10px rgba(0, 102, 255, 0.20);
}

.q-num-btn.answered.active {
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.25), 0 3px 10px rgba(0, 102, 255, 0.20);
}

/* 侧边栏底部 */
.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(0, 102, 255, 0.06);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sidebar-status {
  text-align: center;
  padding: 10px 0;
  font-size: 13px;
  font-weight: 600;
  border-radius: 10px;
}

.sidebar-status.submitted-status {
  color: #00a844;
  background: rgba(0, 200, 83, 0.08);
}

.sidebar-status.timeup-status {
  color: #e53935;
  background: rgba(229, 57, 53, 0.08);
}

.sidebar-status.notstarted-status {
  color: #ff9800;
  background: rgba(255, 152, 0, 0.08);
}

.ai-sidebar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 10px 0;
  border: 1px solid rgba(0, 102, 255, 0.12);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.5);
  color: #0066ff;
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
}

.ai-sidebar-btn:hover {
  background: rgba(0, 102, 255, 0.06);
  border-color: rgba(0, 102, 255, 0.25);
}

.ai-sidebar-btn.active {
  background: linear-gradient(135deg, #0066ff, #00d4ff);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 3px 10px rgba(0, 102, 255, 0.2);
}

/* ==================== 右侧主区域 ==================== */
.exam-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px;
  overflow-y: auto;
}

.status-banner,
.countdown-banner {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  max-width: 760px;
  padding: 10px 16px;
  margin-bottom: 16px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
}

.countdown-banner {
  background: rgba(0, 102, 255, 0.06);
  color: #0066ff;
  font-size: 15px;
}

.countdown-banner.urgent {
  background: rgba(229, 57, 53, 0.08);
  color: #c62828;
  animation: pulse-urgent 1s ease-in-out infinite alternate;
}

@keyframes pulse-urgent {
  from { opacity: 0.8; }
  to { opacity: 1; }
}

.countdown-banner strong {
  font-size: 17px;
  font-variant-numeric: tabular-nums;
}

.early-hint {
  font-size: 13px;
  opacity: 0.7;
}

.status-banner.submitted-banner {
  background: rgba(0, 200, 83, 0.08);
  color: #00a844;
}

.status-banner.timeup-banner {
  background: rgba(229, 57, 53, 0.08);
  color: #c62828;
}

.status-banner.notstarted-banner {
  background: rgba(255, 152, 0, 0.08);
  color: #e65100;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #7a9aba;
  font-size: 15px;
}

.question-card {
  width: 100%;
  max-width: 760px;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.80);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 32px rgba(0, 102, 255, 0.05);
  padding: 32px;
  align-self: flex-start;
}

/* ====== 题目标题行 ====== */
.q-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.q-type-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.q-score {
  font-size: 13px;
  color: #7a9aba;
  font-weight: 500;
}

.q-answered-tag {
  margin-left: auto;
  font-size: 12px;
  color: #00c853;
  background: rgba(0, 200, 83, 0.08);
  padding: 3px 10px;
  border-radius: 6px;
  font-weight: 500;
}

/* ====== 题干 ====== */
.q-stem {
  font-size: 16px;
  font-weight: 500;
  color: #1a3a5a;
  line-height: 1.8;
  margin-bottom: 28px;
}

.q-stem :deep(p) {
  margin: 0;
}

.q-number {
  font-weight: 700;
  color: #0066ff;
  margin-right: 4px;
}

/* ====== 选项区 ====== */
.q-options {
  flex: 1;
  margin-bottom: 28px;
}

.option-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 10px;
  border: 1.5px solid rgba(0, 102, 255, 0.06);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all 0.2s ease;
}

.option-item:hover {
  border-color: rgba(0, 102, 255, 0.15);
  background: rgba(255, 255, 255, 0.8);
}

.option-item.selected {
  border-color: #0066ff;
  background: rgba(0, 102, 255, 0.04);
  box-shadow: 0 0 0 2px rgba(0, 102, 255, 0.08);
}

.option-radio {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  border: 2px solid #c0d0e0;
  border-radius: 50%;
  margin-top: 1px;
  transition: all 0.2s ease;
}

.option-radio.checked {
  border-color: #0066ff;
}

.radio-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #0066ff;
}

.option-checkbox {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  border: 2px solid #c0d0e0;
  border-radius: 4px;
  margin-top: 1px;
  transition: all 0.2s ease;
}

.option-checkbox.checked {
  border-color: #0066ff;
  background: #0066ff;
}

.option-label {
  font-size: 15px;
  font-weight: 600;
  color: #4a6a8a;
  flex-shrink: 0;
  min-width: 18px;
}

.option-item.selected .option-label {
  color: #0066ff;
}

.option-content {
  font-size: 15px;
  color: #2a4a6a;
  line-height: 1.6;
}

/* ====== 填空 / 简答 ====== */
.fill-area {
  padding: 8px 0;
}

.fill-input {
  width: 100%;
  padding: 14px 18px;
  border: 1.5px solid rgba(0, 102, 255, 0.10);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.5);
  font-size: 15px;
  font-family: inherit;
  color: #1a3a5a;
  outline: none;
  transition: all 0.2s ease;
}

.fill-input:focus {
  border-color: #0066ff;
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.08);
}

.fill-input::placeholder {
  color: #b0c8e0;
}

.sa-textarea {
  width: 100%;
  padding: 14px 18px;
  border: 1.5px solid rgba(0, 102, 255, 0.10);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.5);
  font-size: 15px;
  font-family: inherit;
  color: #1a3a5a;
  outline: none;
  resize: vertical;
  min-height: 120px;
  line-height: 1.7;
  transition: all 0.2s ease;
}

.sa-textarea:focus {
  border-color: #0066ff;
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.08);
}

.sa-textarea::placeholder {
  color: #b0c8e0;
}

/* ====== 编程题代码区 ====== */
.code-area {
  flex: 1;
  margin-bottom: 20px;
}

.code-textarea {
  width: 100%;
  min-height: 300px;
  padding: 18px;
  border: 1.5px solid rgba(0, 102, 255, 0.10);
  border-radius: 12px;
  background: #1a1a2e;
  color: #e0e0e0;
  font-family: 'JetBrains Mono', 'Fira Code', 'Cascadia Code', 'Consolas', monospace;
  font-size: 14px;
  line-height: 1.7;
  outline: none;
  resize: vertical;
  tab-size: 2;
  transition: all 0.2s ease;
}

.code-textarea:focus {
  border-color: #0066ff;
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.08);
}

.code-textarea::placeholder {
  color: #666;
}

/* ====== 底部导航 ====== */
.q-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 20px;
  border-top: 1px solid rgba(0, 102, 255, 0.06);
  gap: 12px;
}

.q-footer-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.q-position {
  font-size: 13px;
  color: #7a9aba;
  font-weight: 500;
}

.nav-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 22px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
}

.nav-prev {
  background: rgba(255, 255, 255, 0.7);
  color: #4a6a8a;
  border: 1px solid rgba(0, 102, 255, 0.08);
}

.nav-prev:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(0, 102, 255, 0.15);
  color: #1a3a5a;
}

.nav-prev:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.nav-next {
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  color: #fff;
  box-shadow: 0 4px 14px rgba(0, 102, 255, 0.25);
}

.nav-next:hover {
  box-shadow: 0 6px 24px rgba(0, 102, 255, 0.40);
  transform: translateY(-1px);
}

.nav-next:active {
  transform: translateY(0);
}

.nav-submit {
  background: linear-gradient(135deg, #00c853 0%, #00e676 100%);
  color: #fff;
  box-shadow: 0 4px 14px rgba(0, 200, 83, 0.30);
}

.nav-submit:hover:not(:disabled) {
  box-shadow: 0 6px 24px rgba(0, 200, 83, 0.45);
  transform: translateY(-1px);
}

.nav-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .exam-sidebar {
    width: 160px;
  }

  .exam-sidebar.collapsed {
    width: 0;
  }

  .question-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 6px;
    padding: 12px;
  }

  .exam-main {
    padding: 20px;
  }

  .question-card {
    padding: 24px 20px;
  }
}

@media (max-width: 480px) {
  .exam-main {
    padding: 16px;
  }

  .question-card {
    padding: 20px 16px;
    border-radius: 16px;
  }

  .q-stem {
    font-size: 15px;
  }

  .nav-btn {
    padding: 8px 16px;
    font-size: 13px;
  }
}
</style>
