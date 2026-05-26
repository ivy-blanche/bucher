<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import MainLayout from '@/layouts/MainLayout.vue'
import { getHomeworkDoData, saveAnswer, submitHomework, runCode } from '@/api/homework'
import type { RunCodeResponse } from '@/api/homework'
import OJEditor from '@/components/OJEditor.vue'
import type { Problem } from '@/components/OJEditor.vue'
import { useAIStore } from '@/stores/ai'
import AIChat from '@/components/AIChat.vue'

const route = useRoute()
const router = useRouter()
const homeworkId = route.params.id as string
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
interface OptionItem {
  label: string
  content: string
}

interface QuestionItem {
  questionId: number
  questionType: 'single' | 'multiple' | 'judge' | 'fill' | 'short-answer' | 'programming'
  content: string
  score: number
  sortOrder: number
  /** 作答答案：单选题 "A"，多选题 "ABC"，填空/简答 文本，编程题填代码，未作答 null */
  answer: string | null
  options: OptionItem[] | null
  templateCode?: string
  programmingConfig?: {
    templateCode: string
    judge0LanguageId: number
    timeLimit: number
    memoryLimit: number
  }
  testCases?: {
    input: string
    expectedOutput: string
    isSample?: boolean
    sortOrder?: number
  }[]
}

interface HomeworkData {
  homeworkId: number
  title: string
  description: string
  courseName: string
  totalScore: number
  deadline: string
  status: number
  submitted: boolean
  questions: QuestionItem[]
}

const loading = ref(true)
const homework = reactive<HomeworkData>({
  homeworkId: 0,
  title: '',
  description: '',
  courseName: '',
  totalScore: 0,
  deadline: '',
  status: 0,
  submitted: false,
  questions: [],
})

/** 当前题目索引 */
const currentIndex = ref(0)
const current = computed(() => homework.questions[currentIndex.value] || null)

/** 侧边栏折叠 */
const sidebarCollapsed = ref(false)

/** 当前题最后保存的答案快照，用于判断是否有变更 */
const lastSavedAnswer = ref<string | null>(null)
const answerDirty = ref(false)

/** 是否已过截止时间 */
const isExpired = computed(() => {
  if (!homework.deadline) return false
  return new Date() > new Date(homework.deadline.replace(' ', 'T'))
})

/** 是否只读（已提交 或 已截止）*/
const isReadonly = computed(() => homework.submitted || isExpired.value)

// ==================== 题型元信息 ====================
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

// ==================== 编程题 ====================
const DEFAULT_JAVA_CODE = `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        // 在此处编写你的代码

    }
}`

const programmingCode = ref('')
const codeRunning = ref(false)
const ojEditorRef = ref<InstanceType<typeof OJEditor>>()

const programmingProblem = computed((): Problem | undefined => {
  const q = current.value
  if (!q || q.questionType !== 'programming') return undefined
  return {
    title: '编程题',
    content: q.content,
    samples: (q.testCases || []).map(tc => ({
      input: tc.input,
      output: tc.expectedOutput,
    })),
    templateCode: q.templateCode || q.programmingConfig?.templateCode || '',
    timeLimit: q.programmingConfig?.timeLimit,
    memoryLimit: q.programmingConfig?.memoryLimit,
  }
})

function getProgrammingCode(q: QuestionItem): string {
  if (q.answer) return q.answer
  if (q.templateCode) return q.templateCode
  if (q.programmingConfig?.templateCode) return q.programmingConfig.templateCode
  return DEFAULT_JAVA_CODE
}

function handleProgrammingCodeUpdate(code: string) {
  programmingCode.value = code
  if (current.value) {
    current.value.answer = code
  }
}

async function handleProgramRun(code: string, _stdin: string) {
  const q = current.value
  if (!q) return

  codeRunning.value = true
  try {
    const res = await runCode({
      homeworkId: homework.homeworkId,
      questionId: q.questionId,
      code,
    })

    if (!ojEditorRef.value) return

    if (!res.compileSuccess) {
      ojEditorRef.value.setExecError(res.compileOutput || '编译失败')
      return
    }

    const results = res.testCaseResults.map((tc, i) => ({
      name: `测试点 ${i + 1}`,
      passed: tc.passed,
      input: '',
      expected: tc.expectedOutput,
      actual: tc.stdout,
      time: tc.time ? Math.round(parseFloat(tc.time) * 1000) : undefined,
      memory: tc.memory,
    }))

    ojEditorRef.value.setTestResults(results)
  } catch {
    ElMessage.error('运行失败，请重试')
  } finally {
    codeRunning.value = false
  }
}

async function handleProgramSubmit(code: string) {
  if (current.value) {
    current.value.answer = code
  }
  try {
    await saveCurrentAnswer()
    ElMessage.success('代码已保存')
    // 跳转到下一题
    if (currentIndex.value < homework.questions.length - 1) {
      nextQuestion()
    }
  } catch {
    ElMessage.error('保存失败，请重试')
  }
}

// 切换题目时初始化编程题的代码
watch(current, (q) => {
  if (q && q.questionType === 'programming') {
    programmingCode.value = getProgrammingCode(q)
  }
}, { immediate: true })

// ==================== 数据初始化 ====================
let hasInited = false

async function loadHomework() {
  loading.value = true
  try {
    const res = await getHomeworkDoData(homeworkId)
    Object.assign(homework, res)

    // 按 sortOrder 排序
    homework.questions.sort((a, b) => a.sortOrder - b.sortOrder)

    // 记录每道题当前答案的快照（用于脏检测）
    syncLastSaved()
    hasInited = true
  } catch {
    // 错误由拦截器统一处理
  } finally {
    loading.value = false
  }
}

/** 同步 lastSavedAnswer 为当前题的实际 answer */
function syncLastSaved() {
  const q = homework.questions[currentIndex.value]
  lastSavedAnswer.value = q ? q.answer : null
  answerDirty.value = false
}

// ==================== 作答状态 ====================
function isAnswered(q: QuestionItem): boolean {
  return q.answer !== null && q.answer !== ''
}

function isMultiSelected(q: QuestionItem, label: string): boolean {
  return (q.answer || '').includes(label)
}

const currentAnswered = computed(() => current.value ? isAnswered(current.value) : false)

const answeredCount = computed(() => homework.questions.filter(isAnswered).length)

// ==================== 选项事件 ====================
function selectSingle(q: QuestionItem, label: string) {
  if (isReadonly.value) return
  q.answer = label
  answerDirty.value = true
  markDirty()
}

function toggleMulti(q: QuestionItem, label: string) {
  if (isReadonly.value) return
  const set = new Set((q.answer || '').split(''))
  if (set.has(label)) {
    set.delete(label)
  } else {
    set.add(label)
  }
  q.answer = [...set].sort().join('')
  answerDirty.value = true
  markDirty()
}

/** 填空/简答输入后标记脏 */
function markDirty() {
  answerDirty.value = true
}

/** 当前题 answer 有变化 → 脏 */
watch(
  () => current.value?.answer,
  () => {
    if (hasInited && current.value && !isReadonly.value) {
      answerDirty.value = lastSavedAnswer.value !== current.value.answer
    }
  },
)

// ==================== 暂存当前题（切题时调用）====================
async function saveCurrentAnswer(): Promise<void> {
  const q = current.value
  if (!q || isReadonly.value) return

  // 只有答案有变化才调接口
  if (!answerDirty.value) return

  try {
    await saveAnswer({
      homeworkId: homework.homeworkId,
      questionId: q.questionId,
      answer: q.answer ?? '',
    })
    syncLastSaved()
  } catch {
    // 暂存失败不影响切题，但抛出错误给调用方以便提示
    throw new Error('答案暂存失败')
  }
}

// ==================== 导航 ====================
async function goToQuestion(index: number) {
  if (index === currentIndex.value) return
  // 先暂存当前题
  try {
    await saveCurrentAnswer()
  } catch {
    ElMessage.warning('当前题答案暂存失败，请重试')
  }
  currentIndex.value = index
  syncLastSaved()
}

async function prevQuestion() {
  if (currentIndex.value <= 0) return
  try {
    await saveCurrentAnswer()
  } catch {
    ElMessage.warning('当前题答案暂存失败，请重试')
  }
  currentIndex.value--
  syncLastSaved()
}

async function nextQuestion() {
  if (currentIndex.value >= homework.questions.length - 1) return
  try {
    await saveCurrentAnswer()
  } catch {
    ElMessage.warning('当前题答案暂存失败，请重试')
  }
  currentIndex.value++
  syncLastSaved()
}

// ==================== 提交 ====================
const submitting = ref(false)

async function handleSubmit() {
  const unanswered = homework.questions.filter(q => !isAnswered(q))

  // 先保存当前题
  try {
    await saveCurrentAnswer()
  } catch {
    // 继续
  }

  if (unanswered.length > 0) {
    try {
      await ElMessageBox.confirm(
        `还有 ${unanswered.length} 道题未作答，确定要提交吗？`,
        '确认提交',
        { confirmButtonText: '确定提交', cancelButtonText: '继续作答', type: 'warning' },
      )
    } catch {
      return // 用户取消
    }
  } else {
    try {
      await ElMessageBox.confirm('所有题目已作答，确认提交？', '确认提交', {
        confirmButtonText: '确定提交',
        cancelButtonText: '再看看',
        type: 'info',
      })
    } catch {
      return
    }
  }

  submitting.value = true
  try {
    await submitHomework({ homeworkId: homework.homeworkId })
    ElMessage.success('作业提交成功！')
    router.back()
  } catch {
    submitting.value = false
  }
}

// ==================== 返回 ====================
function goBack() {
  router.back()
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadHomework()
})
</script>

<template>
  <MainLayout>
    <!-- 全局加载 -->
    <div v-if="loading" class="loading-full">
      <div class="loading-spinner" />
      <p>加载中...</p>
    </div>

    <template v-else>
      <div class="hw-container">
        <!-- 左侧题目导航 -->
        <aside class="hw-sidebar" :class="{ collapsed: sidebarCollapsed }">
          <template v-if="!sidebarCollapsed">
            <div class="sidebar-header">
              <button class="back-btn" @click="goBack">
                <svg viewBox="0 0 16 16" width="16" height="16">
                  <polyline points="10,3 5,8 10,13" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span>返回</span>
              </button>
              <h3 class="sidebar-title">{{ homework.title }}</h3>
              <p class="sidebar-course">{{ homework.courseName }}</p>
              <p class="sidebar-progress">
                已答 <strong>{{ answeredCount }}</strong> / {{ homework.questions.length }} 题
              </p>
            </div>

            <div class="question-grid">
              <button
                v-for="(q, idx) in homework.questions"
                :key="q.questionId"
                class="q-num-btn"
                :class="{
                  active: idx === currentIndex,
                  answered: isAnswered(q),
                }"
                @click="goToQuestion(idx)"
              >
                {{ idx + 1 }}
              </button>
            </div>

            <!-- 侧边栏底部 -->
            <div class="sidebar-footer">
              <div v-if="homework.submitted" class="sidebar-status submitted-status">已提交</div>
              <div v-else-if="isExpired" class="sidebar-status expired-status">已截止</div>
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
        <main class="hw-main" :class="{ 'hw-main-programming': current?.questionType === 'programming' }">
          <!-- 已提交提示 -->
          <div v-if="homework.submitted" class="status-banner submitted-banner">
            <svg viewBox="0 0 16 16" width="16" height="16">
              <circle cx="8" cy="8" r="7" fill="none" stroke="currentColor" stroke-width="1.2"/>
              <polyline points="5,8 7,10 11,5" stroke="currentColor" stroke-width="1.4" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            已提交，仅可查看
          </div>
          <!-- 已截止提示 -->
          <div v-else-if="isExpired" class="status-banner expired-banner">
            <svg viewBox="0 0 16 16" width="16" height="16">
              <circle cx="8" cy="8" r="7" fill="none" stroke="currentColor" stroke-width="1.2"/>
              <line x1="5" y1="5" x2="11" y2="11" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
              <line x1="11" y1="5" x2="5" y2="11" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
            </svg>
            作业已截止，仅可查看
          </div>

          <!-- 无题目 -->
          <div v-if="!current" class="empty-state">
            <p>暂无题目数据</p>
          </div>

          <template v-else>
            <!-- 编程题 -->
            <div v-if="current.questionType === 'programming'" class="programming-wrap">
              <OJEditor
                :key="current.questionId"
                ref="ojEditorRef"
                :problem="programmingProblem"
                :model-value="programmingCode"
                :readonly="isReadonly"
                :running="codeRunning"
                submit-label="下一题"
                @update:model-value="handleProgrammingCodeUpdate"
                @run="handleProgramRun"
                @submit="handleProgramSubmit"
              />
            </div>
            <!-- 其他题型 -->
            <div v-else class="question-card">
              <!-- 题目标题 -->
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

              <!-- 题干（支持 HTML） -->
              <div class="q-stem">
                <span class="q-number">{{ currentIndex + 1 }}.</span>
                <span v-html="current.content" />
              </div>

              <!-- 选项区 -->
              <div class="q-options">
                <!-- 单选题 / 判断题 -->
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

                <!-- 多选题 -->
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

                <!-- 填空题 -->
                <div v-else-if="current.questionType === 'fill'" class="fill-area">
                  <input
                    :value="current.answer ?? ''"
                    @input="e => { current.answer = (e.target as HTMLInputElement).value; markDirty() }"
                    type="text"
                    class="fill-input"
                    placeholder="请输入答案..."
                    :readonly="homework.submitted"
                  />
                </div>

                <!-- 简答题 -->
                <div v-else-if="current.questionType === 'short-answer'" class="fill-area">
                  <textarea
                    :value="current.answer ?? ''"
                    @input="e => { current.answer = (e.target as HTMLTextAreaElement).value; markDirty() }"
                    class="sa-textarea"
                    placeholder="请在此输入你的回答..."
                    rows="6"
                    :readonly="homework.submitted"
                  />
                </div>
              </div>

              <!-- 底部导航 -->
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
                  <span class="q-position">{{ currentIndex + 1 }} / {{ homework.questions.length }}</span>
                </div>

                <button
                  v-if="currentIndex === homework.questions.length - 1 && !isReadonly"
                  class="nav-btn nav-submit"
                  :disabled="submitting"
                  @click="handleSubmit"
                >
                  <svg viewBox="0 0 14 14" width="12" height="12">
                    <polyline points="2,7 5,10 12,3" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                  {{ submitting ? '提交中...' : '提交作业' }}
                </button>

                <button
                  v-else-if="currentIndex < homework.questions.length - 1 && !isReadonly"
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
.hw-container {
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
.hw-sidebar {
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

.hw-sidebar.collapsed {
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
  margin: 0 0 8px;
}

.sidebar-progress {
  font-size: 13px;
  color: #7a9aba;
  margin: 0;
}

.sidebar-progress strong {
  color: #0066ff;
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

.sidebar-status.submitted-status {
  color: #00a844;
  background: rgba(0, 200, 83, 0.08);
}

.sidebar-status.expired-status {
  color: #e53935;
  background: rgba(229, 57, 53, 0.08);
}

/* ==================== 右侧主区域 ==================== */
.hw-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px;
  overflow-y: auto;
}

.status-banner {
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

.status-banner.submitted-banner {
  background: rgba(0, 200, 83, 0.08);
  color: #00a844;
}

.status-banner.expired-banner {
  background: rgba(229, 57, 53, 0.08);
  color: #c62828;
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

/* 题目标题行 */
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

/* 题干 */
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

/* 选项区 */
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

/* 填空 / 简答 */
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

/* 底部导航 */
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

/* ==================== 编程题全屏布局 ==================== */
.hw-main.hw-main-programming {
  padding: 0;
  overflow: hidden;
  align-items: stretch;
}

.hw-main-programming .status-banner {
  flex-shrink: 0;
  margin-bottom: 0;
  border-radius: 0;
  padding: 10px 32px;
  max-width: none;
}

.programming-wrap {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
</style>
