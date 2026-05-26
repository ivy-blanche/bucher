<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import TeacherLayout from '@/layouts/TeacherLayout.vue'
import { getQuestionBankList, getQuestionList } from '@/api/question'
import { getCourseList } from '@/api/course'
import { saveExamDraft, publishExam, getCourseClassList } from '@/api/exam'
import type { QuestionBankItem } from '@/api/question'
import type { CourseListItem } from '@/api/course'

const router = useRouter()

/* ==================== 步骤定义 ==================== */
const steps = [
  { key: 'title', label: '基本信息', icon: '📋' },
  { key: 'bank', label: '选择题库', icon: '📚' },
  { key: 'scores', label: '分数设置', icon: '✏️' },
]
const currentStep = ref(0)

/* ==================== 表单数据 ==================== */
const title = ref('')
const sourceBankId = ref<string | null>(null)
const sourceBankName = ref('')

interface QuestionWithScore {
  questionId: string
  type: number
  typeName: string
  contentPreview: string
  content: string
  score: number
}

const questions = ref<QuestionWithScore[]>([])
const totalScore = computed(() => {
  const sum = questions.value.reduce((acc, q) => acc + (Number(q.score) || 0), 0)
  return Math.round(sum * 100) / 100
})

const scoredCount = computed(() => questions.value.filter((q) => (Number(q.score) || 0) > 0).length)

/* ==================== 题型配置 ==================== */
const typeOrder: number[] = [1, 2, 3, 4]
const typeLabelMap: Record<number, string> = {
  1: '单选题', 2: '多选题', 3: '填空题', 4: '简答题',
}
const typeColorMap: Record<number, string> = {
  1: '#0066ff', 2: '#7c3aed', 3: '#0891b2', 4: '#d97706',
}
const typeIconMap: Record<number, string> = {
  1: '○', 2: '□', 3: '＿', 4: '〰',
}
const typeNumeral: Record<number, string> = {
  1: '一', 2: '二', 3: '三', 4: '四',
}

/* ==================== 外部数据 ==================== */
const bankList = ref<QuestionBankItem[]>([])
const bankLoading = ref(false)

// 发布弹窗数据
const showPublishDialog = ref(false)
const publishing = ref(false)
const courseList = ref<CourseListItem[]>([])
const courseLoading = ref(false)
const selectedCourseId = ref<string | null>(null)
const classList = ref<{ id: string; name: string; studentCount: number }[]>([])
const classLoading = ref(false)
const selectedClassIds = ref<string[]>([])
const startTime = ref('')
const duration = ref(120)
const earlySubmitMinutes = ref(30)
const lateBanMinutes = ref(15)
const autoSubmit = ref(true)
const passScore = ref(60)
const description = ref('')

/* ==================== 计算导航状态 ==================== */
const canNext = computed(() => {
  if (currentStep.value === 0) return title.value.trim().length > 0
  if (currentStep.value === 1) return sourceBankId.value !== null
  if (currentStep.value === 2) return questions.value.length > 0
  return true
})

const stepSummary = computed(() => {
  if (currentStep.value === 0 && title.value) return title.value
  if (currentStep.value === 1 && sourceBankName.value) return sourceBankName.value
  if (currentStep.value === 2) return `共 ${questions.value.length} 题 · 总分 ${totalScore.value}`
  return ''
})

/* ==================== 步骤导航 ==================== */
function goNext() {
  if (!canNext.value) {
    if (currentStep.value === 0) ElMessage.warning('请填写考试名称')
    else if (currentStep.value === 1) ElMessage.warning('请选择一个题库')
    return
  }
  if (currentStep.value === 1) {
    loadBankQuestions()
  }
  if (currentStep.value < steps.length - 1) {
    currentStep.value++
  }
}

function goPrev() {
  if (currentStep.value > 0) currentStep.value--
}

function goToStep(index: number) {
  if (index < currentStep.value) {
    currentStep.value = index
    return
  }
  for (let i = currentStep.value; i < index; i++) {
    if (i === 0 && !title.value.trim()) {
      ElMessage.warning('请先填写考试名称')
      return
    }
    if (i === 1 && !sourceBankId.value) {
      ElMessage.warning('请先选择题库')
      return
    }
  }
  if (index === 2) loadBankQuestions()
  currentStep.value = index
}

/* ==================== 加载数据 ==================== */
async function loadBankList() {
  bankLoading.value = true
  try {
    bankList.value = await getQuestionBankList()
  } catch {
    bankList.value = []
  } finally {
    bankLoading.value = false
  }
}

async function loadBankQuestions() {
  if (!sourceBankId.value || questions.value.length > 0) return
  try {
    const list = await getQuestionList(sourceBankId.value)
    if (list.length === 0) {
      ElMessage.warning('该题库暂无题目，请选择其他题库')
      return
    }
    const defaultScore = Math.floor(100 / list.length * 100) / 100
    questions.value = list.map((q) => ({
      questionId: q.id,
      type: q.type,
      typeName: q.typeName,
      contentPreview: q.contentPreview,
      content: q.contentPreview,
      score: defaultScore,
    }))
  } catch {
    ElMessage.error('加载题目失败')
  }
}

async function loadCourses() {
  courseLoading.value = true
  try {
    courseList.value = await getCourseList()
  } catch {
    courseList.value = []
  } finally {
    courseLoading.value = false
  }
}

async function loadClasses() {
  if (!selectedCourseId.value) {
    classList.value = []
    return
  }
  classLoading.value = true
  try {
    classList.value = await getCourseClassList(selectedCourseId.value)
  } catch {
    classList.value = []
    selectedClassIds.value = []
  } finally {
    classLoading.value = false
  }
}

/* ==================== 初始化 ==================== */
onMounted(() => {
  loadCourses()
  loadBankList()
})

/* ==================== 选择题库 ==================== */
function selectBank(bank: QuestionBankItem) {
  if (sourceBankId.value === bank.id) return
  sourceBankId.value = bank.id
  sourceBankName.value = bank.name
  questions.value = []
}

/* ==================== 分数编辑 ==================== */
function onScoreInput(q: { score: number }) {
  const num = Number(q.score)
  if (isNaN(num) || num < 0) q.score = 0
}

function distributeEqually() {
  if (questions.value.length === 0) return
  const even = Math.floor(100 / questions.value.length * 100) / 100
  questions.value.forEach((q) => (q.score = even))
  ElMessage.success(`已平均分配，每题 ${even} 分，总分 ${totalScore.value}`)
}

/* ==================== 保存草稿 ==================== */
const saving = ref(false)

async function handleSaveDraft() {
  if (!validateForm()) return
  saving.value = true
  try {
    await saveExamDraft({
      title: title.value.trim(),
      courseId: selectedCourseId.value || '',
      sourceBankId: sourceBankId.value!,
      questions: questions.value.map((q) => ({
        questionId: q.questionId,
        score: q.score,
      })),
    })
    ElMessage.success('草稿保存成功')
    router.push('/teacher/exams')
  } catch {
    // 错误由拦截器统一处理
  } finally {
    saving.value = false
  }
}

/* ==================== 发布弹窗 ==================== */
const searchQueryE = ref('')

const groupedQuestions = computed(() => {
  const list = questions.value.filter((q) => {
    if (!searchQueryE.value.trim()) return true
    return q.contentPreview.toLowerCase().includes(searchQueryE.value.trim().toLowerCase())
  })
  const groups: { type: number; numeral: string; label: string; questions: QuestionWithScore[] }[] = []
  for (const type of typeOrder) {
    const qs = list.filter((q) => q.type === type)
    if (qs.length > 0) {
      groups.push({ type, numeral: typeNumeral[type], label: typeLabelMap[type], questions: qs })
    }
  }
  return groups
})

function openPublishDialog() {
  if (!validateForm()) return
  selectedCourseId.value = null
  selectedClassIds.value = []
  startTime.value = ''
  duration.value = 120
  earlySubmitMinutes.value = 30
  lateBanMinutes.value = 15
  autoSubmit.value = true
  passScore.value = 60
  description.value = ''
  classList.value = []
  showPublishDialog.value = true
}

function closePublishDialog() {
  showPublishDialog.value = false
}

function toggleClass(classId: string) {
  const idx = selectedClassIds.value.indexOf(classId)
  if (idx > -1) {
    selectedClassIds.value.splice(idx, 1)
  } else {
    selectedClassIds.value.push(classId)
  }
}

function canPublish() {
  return selectedCourseId.value !== null
    && selectedClassIds.value.length > 0
    && startTime.value.trim() !== ''
    && duration.value > 0
}

async function handlePublish() {
  if (!canPublish()) {
    ElMessage.warning('请填写完整的发布信息')
    return
  }
  publishing.value = true
  try {
    await publishExam({
      title: title.value.trim(),
      sourceBankId: sourceBankId.value!,
      questions: questions.value.map((q) => ({
        questionId: q.questionId,
        score: q.score,
      })),
      courseId: selectedCourseId.value!,
      classIds: selectedClassIds.value,
      startTime: new Date(startTime.value).toISOString(),
      duration: duration.value,
      earlySubmitMinutes: earlySubmitMinutes.value,
      lateBanMinutes: lateBanMinutes.value,
      autoSubmit: autoSubmit.value ? 1 : 0,
      passScore: passScore.value,
      description: description.value || undefined,
    })
    ElMessage.success('考试发布成功')
    closePublishDialog()
    router.push('/teacher/exams')
  } catch {
    // 错误由拦截器统一处理
  } finally {
    publishing.value = false
  }
}

/* ==================== 通用校验 ==================== */
function validateForm(): boolean {
  if (!title.value.trim()) {
    ElMessage.warning('请填写考试名称')
    currentStep.value = 0
    return false
  }
  if (!sourceBankId.value) {
    ElMessage.warning('请选择题库')
    currentStep.value = 1
    return false
  }
  if (questions.value.length === 0) {
    ElMessage.warning('题库中暂无题目')
    currentStep.value = 1
    return false
  }
  if (scoredCount.value === 0) {
    ElMessage.warning('请为至少一道题目设置分数')
    currentStep.value = 2
    return false
  }
  return true
}
</script>

<template>
  <TeacherLayout>
    <div class="assign-page">
      <!-- 页面头部 -->
      <div class="page-header">
        <button class="back-btn" @click="router.push('/teacher/exams')">
          <svg viewBox="0 0 20 20" width="18" height="18">
            <polyline points="12,4 6,10 12,16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <span>返回考试列表</span>
        </button>
        <h1 class="page-title">创建考试</h1>
      </div>

      <!-- 主体卡片 -->
      <div class="wizard-card">
        <!-- ============ 步骤指示器 ============ -->
        <div class="step-indicator">
          <div
            v-for="(step, idx) in steps"
            :key="step.key"
            class="step-node"
            :class="{
              'step-active': idx === currentStep,
              'step-done': idx < currentStep,
              'step-clickable': idx < currentStep,
            }"
            @click="goToStep(idx)"
          >
            <div class="step-circle">
              <svg v-if="idx < currentStep" viewBox="0 0 16 16" width="16" height="16">
                <polyline points="4,8 7,11 12,5" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
              <span v-else>{{ idx + 1 }}</span>
            </div>
            <div class="step-label">
              <span class="step-label-icon">{{ step.icon }}</span>
              <span class="step-label-text">{{ step.label }}</span>
            </div>
            <div v-if="idx < steps.length - 1" class="step-connector" :class="{ 'connector-done': idx < currentStep }" />
          </div>
        </div>

        <!-- ============ 步骤 1：基本信息 ============ -->
        <div v-show="currentStep === 0" class="step-content">
          <div class="step-panel">
            <div class="step-panel-header">
              <h2 class="panel-title">设置考试名称</h2>
              <p class="panel-desc">为你的考试取一个清晰的名称。</p>
            </div>
            <div class="form-area">
              <div class="input-group">
                <label class="input-label">考试名称 <span class="required">*</span></label>
                <input
                  v-model="title"
                  type="text"
                  class="title-input"
                  placeholder="例：期中考试"
                  maxlength="100"
                  @keyup.enter="goNext"
                />
                <div class="input-meta">
                  <span class="char-count">{{ title.length }} / 100</span>
                </div>
              </div>
              <div class="quick-templates">
                <span class="quick-label">快速填入：</span>
                <button
                  v-for="tpl in ['期中考试', '期末考试', '单元测试']"
                  :key="tpl"
                  class="quick-chip"
                  @click="title = tpl"
                >
                  {{ tpl }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- ============ 步骤 2：选择题库 ============ -->
        <div v-show="currentStep === 1" class="step-content">
          <div class="step-panel">
            <div class="step-panel-header">
              <h2 class="panel-title">选择题库</h2>
              <p class="panel-desc">选择一个题库作为考试题目来源，之后可为每道题设置分数。</p>
            </div>

            <div v-if="bankLoading" class="bank-loading">
              <div class="loading-spinner" />
              <span>加载题库中...</span>
            </div>

            <div v-else-if="bankList.length === 0" class="bank-empty">
              <svg viewBox="0 0 60 60" width="48" height="48" class="empty-icon">
                <rect x="10" y="8" width="40" height="44" rx="4" fill="none" stroke="currentColor" stroke-width="1.8" />
                <line x1="18" y1="20" x2="42" y2="20" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                <line x1="18" y1="28" x2="42" y2="28" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" opacity="0.4" />
                <line x1="18" y1="36" x2="34" y2="36" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" opacity="0.4" />
              </svg>
              <p class="empty-text">暂无题库，请先前往题库管理创建</p>
              <button class="link-btn" @click="router.push('/teacher/question-bank')">去创建题库</button>
            </div>

            <div v-else class="bank-grid">
              <div
                v-for="bank in bankList"
                :key="bank.id"
                class="bank-card"
                :class="{ 'bank-selected': sourceBankId === bank.id }"
                @click="selectBank(bank)"
              >
                <div class="bank-card-top">
                  <div class="bank-radio">
                    <div class="bank-radio-dot" :class="{ 'radio-checked': sourceBankId === bank.id }">
                      <svg v-if="sourceBankId === bank.id" viewBox="0 0 12 12" width="12" height="12">
                        <circle cx="6" cy="6" r="4" fill="currentColor" />
                      </svg>
                    </div>
                  </div>
                  <div class="bank-cover">
                    <svg viewBox="0 0 24 24" width="24" height="24" class="bank-icon">
                      <path d="M4 6h16M4 12h16M4 18h12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                    </svg>
                  </div>
                  <div class="bank-meta">
                    <h3 class="bank-name">{{ bank.name }}</h3>
                    <span class="bank-count">{{ bank.questionCount }} 道题目</span>
                  </div>
                </div>
                <div class="bank-card-bottom">
                  <span class="bank-date">{{ bank.createTime }}</span>
                </div>
                <transition name="fade">
                  <div v-if="sourceBankId === bank.id" class="bank-selected-badge">
                    <svg viewBox="0 0 16 16" width="14" height="14">
                      <polyline points="3,8 6,11 13,4" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                    <span>已选</span>
                  </div>
                </transition>
              </div>
            </div>
          </div>
        </div>

        <!-- ============ 步骤 3：分数设置 ============ -->
        <div v-show="currentStep === 2" class="step-content">
          <div class="step-panel">
            <div class="step-panel-header">
              <h2 class="panel-title">设置分数</h2>
              <p class="panel-desc">为每道题设置分数。</p>
            </div>

            <!-- 分数汇总条 -->
            <div class="score-summary-bar">
              <div class="summary-item">
                <span class="summary-label">题量</span>
                <span class="summary-value">{{ questions.length }} 题</span>
              </div>
              <div class="summary-divider" />
              <div class="summary-item">
                <span class="summary-label">已计分</span>
                <span class="summary-value" :class="{ 'value-warning': scoredCount < questions.length }">
                  {{ scoredCount }} / {{ questions.length }}
                </span>
              </div>
              <div class="summary-divider" />
              <div class="summary-item summary-total">
                <span class="summary-label">总分</span>
                <span class="summary-value-total">{{ totalScore }}</span>
                <span class="summary-unit">分</span>
              </div>
              <div class="summary-actions">
                <button class="ghost-btn" @click="distributeEqually">
                  <svg viewBox="0 0 16 16" width="14" height="14">
                    <path d="M2 8h12M8 2v12" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                  </svg>
                  <span>平均分配</span>
                </button>
              </div>
            </div>

            <div v-if="questions.length === 0" class="questions-empty">
              <p>所选题库暂无题目，请返回选择其他题库。</p>
            </div>

            <!-- 题目分数列表 -->
            <template v-else>
              <div class="share-search-box">
                <svg class="search-svg" viewBox="0 0 20 20" width="14" height="14">
                  <circle cx="8.5" cy="8.5" r="5.5" fill="none" stroke="currentColor" stroke-width="1.5" />
                  <line x1="12.5" y1="12.5" x2="17" y2="17" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                </svg>
                <input v-model="searchQueryE" class="share-search-input" placeholder="搜索题目..." />
              </div>
              <div class="question-list-scroll">
                <div
                  v-for="group in groupedQuestions"
                  :key="group.type"
                  class="q-group"
                >
                  <div class="q-group-header" :style="{ '--g-color': typeColorMap[group.type] }">
                    <span class="qg-numeral">{{ group.numeral }}</span>
                    <span class="qg-label">{{ group.label }}</span>
                    <span class="qg-count">{{ group.questions.length }} 题</span>
                  </div>
                  <div class="q-group-body">
                    <div
                      v-for="(q, qi) in group.questions"
                      :key="q.questionId"
                      class="question-row"
                    >
                      <div class="qr-index">{{ qi + 1 }}</div>
                      <div class="qr-body">
                        <div class="qr-stem">{{ q.contentPreview }}</div>
                        <div class="qr-score-group">
                          <span class="qr-score-label">分数</span>
                          <div class="qr-score-input-wrap">
                            <input
                              v-model.number="q.score"
                              type="number"
                              class="qr-score-input"
                              min="0"
                              max="999"
                              step="0.5"
                              @input="onScoreInput(q)"
                            />
                            <span class="qr-score-unit">分</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>

        <!-- ============ 底部操作栏 ============ -->
        <div class="wizard-footer">
          <div class="footer-left">
            <div v-if="stepSummary" class="step-breadcrumb">
              <svg viewBox="0 0 16 16" width="14" height="14" class="crumb-icon">
                <path d="M2 4l6 4-6 4V4z" fill="currentColor" opacity="0.6" />
                <path d="M8 4l6 4-6 4V4z" fill="currentColor" opacity="0.3" />
              </svg>
              <span>{{ steps[currentStep].label }}：</span>
              <span class="crumb-value">{{ stepSummary }}</span>
            </div>
          </div>
          <div class="footer-right">
            <button
              v-if="currentStep > 0"
              class="footer-btn btn-prev"
              @click="goPrev"
            >
              <svg viewBox="0 0 16 16" width="14" height="14">
                <polyline points="10,4 6,8 10,12" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
              <span>上一步</span>
            </button>

            <button
              v-if="currentStep < steps.length - 1"
              class="footer-btn btn-next"
              @click="goNext"
            >
              <span>下一步</span>
              <svg viewBox="0 0 16 16" width="14" height="14">
                <polyline points="6,4 10,8 6,12" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </button>

            <template v-if="currentStep === steps.length - 1">
              <button class="footer-btn btn-draft" :disabled="saving" @click="handleSaveDraft">
                <svg viewBox="0 0 16 16" width="14" height="14">
                  <path d="M12 2H4a1 1 0 00-1 1v10a1 1 0 001 1h8a1 1 0 001-1V3a1 1 0 00-1-1z" fill="none" stroke="currentColor" stroke-width="1.5" />
                  <line x1="5" y1="8" x2="11" y2="8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                  <line x1="5" y1="11" x2="9" y2="11" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                </svg>
                <span>{{ saving ? '保存中...' : '存为草稿' }}</span>
              </button>
              <button class="footer-btn btn-publish" @click="openPublishDialog">
                <svg viewBox="0 0 16 16" width="14" height="14">
                  <path d="M8 1v9M4 6l4 4 4-4" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                  <path d="M2 12v2a1 1 0 001 1h10a1 1 0 001-1v-2" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                </svg>
                <span>发布考试</span>
              </button>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- ============ 发布弹窗 ============ -->
    <teleport to="body">
      <transition name="dialog">
        <div v-if="showPublishDialog" class="dialog-overlay" @click.self="closePublishDialog">
          <div class="publish-dialog">
            <!-- 弹窗头部 -->
            <div class="publish-dialog-header">
              <div class="publish-icon">
                <svg viewBox="0 0 24 24" width="24" height="24">
                  <path d="M12 2v14M8 12l4 4 4-4" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                  <path d="M4 16v3a2 2 0 002 2h12a2 2 0 002-2v-3" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                </svg>
              </div>
              <div>
                <h3 class="publish-dialog-title">发布考试</h3>
                <p class="publish-dialog-desc">选择目标课程、班级并设置考试时间</p>
              </div>
              <button class="dialog-close-btn" @click="closePublishDialog">
                <svg viewBox="0 0 16 16" width="16" height="16">
                  <line x1="3" y1="3" x2="13" y2="13" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                  <line x1="13" y1="3" x2="3" y2="13" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                </svg>
              </button>
            </div>

            <!-- 弹窗内容 -->
            <div class="publish-dialog-body">
              <!-- 课程选择 -->
              <div class="publish-field">
                <label class="publish-label">选择课程 <span class="required">*</span></label>
                <div class="course-select-grid">
                  <div
                    v-for="course in courseList"
                    :key="course.id"
                    class="course-option"
                    :class="{ 'course-selected': selectedCourseId === course.id }"
                    @click="selectedCourseId = course.id; selectedClassIds = []; loadClasses()"
                  >
                    <div class="course-option-radio">
                      <div class="course-radio-dot" :class="{ 'dot-checked': selectedCourseId === course.id }">
                        <svg v-if="selectedCourseId === course.id" viewBox="0 0 12 12" width="12" height="12">
                          <circle cx="6" cy="6" r="4" fill="currentColor" />
                        </svg>
                      </div>
                    </div>
                    <div class="course-option-body">
                      <span class="course-option-name">{{ course.name }}</span>
                      <span class="course-option-code">{{ course.courseCode }}</span>
                    </div>
                  </div>
                  <div v-if="courseList.length === 0 && !courseLoading" class="course-empty">
                    暂无可选课程
                  </div>
                  <div v-if="courseLoading" class="course-empty">加载中...</div>
                </div>
              </div>

              <!-- 班级选择 -->
              <div class="publish-field">
                <label class="publish-label">
                  选择班级 <span class="required">*</span>
                  <span v-if="selectedCourseId" class="field-hint">（可多选）</span>
                </label>
                <div v-if="!selectedCourseId" class="class-placeholder">
                  请先选择课程
                </div>
                <div v-else-if="classLoading" class="class-placeholder">
                  加载班级中...
                </div>
                <div v-else-if="classList.length === 0" class="class-placeholder">
                  该课程下暂无班级
                </div>
                <div v-else class="class-grid">
                  <div
                    v-for="cls in classList"
                    :key="cls.id"
                    class="class-chip"
                    :class="{ 'class-chip-selected': selectedClassIds.includes(cls.id) }"
                    @click="toggleClass(cls.id)"
                  >
                    <div class="class-chip-check">
                      <svg v-if="selectedClassIds.includes(cls.id)" viewBox="0 0 12 12" width="12" height="12">
                        <polyline points="2,6 5,9 10,3" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                      </svg>
                    </div>
                    <span class="class-chip-name">{{ cls.name }}</span>
                    <span class="class-chip-count">{{ cls.studentCount }} 人</span>
                  </div>
                </div>
              </div>

              <!-- 考试时间 -->
              <div class="publish-row">
                <div class="publish-field flex-1">
                  <label class="publish-label">开始时间 <span class="required">*</span></label>
                  <div class="time-input-wrap">
                    <svg viewBox="0 0 16 16" width="16" height="16" class="time-icon">
                      <rect x="2" y="3" width="12" height="12" rx="2" fill="none" stroke="currentColor" stroke-width="1.5" />
                      <line x1="2" y1="7" x2="14" y2="7" stroke="currentColor" stroke-width="1.5" />
                      <line x1="5" y1="1" x2="5" y2="4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                      <line x1="11" y1="1" x2="11" y2="4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                    </svg>
                    <input
                      v-model="startTime"
                      type="datetime-local"
                      class="time-input"
                    />
                  </div>
                </div>
                <div class="publish-field flex-1">
                  <label class="publish-label">考试时长（分钟）<span class="required">*</span></label>
                  <div class="time-input-wrap">
                    <input
                      v-model.number="duration"
                      type="number"
                      class="time-input duration-input"
                      min="1"
                      max="600"
                    />
                    <span class="duration-unit">分钟</span>
                  </div>
                </div>
              </div>

              <!-- 考试设置 -->
              <div class="publish-field">
                <label class="publish-label">考试设置</label>
                <div class="settings-grid">
                  <div class="setting-item">
                    <label class="setting-label">最早交卷时间</label>
                    <div class="setting-input-group">
                      <input v-model.number="earlySubmitMinutes" type="number" class="setting-input" min="0" max="600" />
                      <span class="setting-unit">分钟</span>
                    </div>
                  </div>
                  <div class="setting-item">
                    <label class="setting-label">最晚入场时间</label>
                    <div class="setting-input-group">
                      <input v-model.number="lateBanMinutes" type="number" class="setting-input" min="0" max="600" />
                      <span class="setting-unit">分钟</span>
                    </div>
                  </div>
                  <div class="setting-item">
                    <label class="setting-label">及格分</label>
                    <div class="setting-input-group">
                      <input v-model.number="passScore" type="number" class="setting-input" min="0" max="999" />
                      <span class="setting-unit">分</span>
                    </div>
                  </div>
                  <div class="setting-item">
                    <label class="setting-label">超时自动提交</label>
                    <div class="toggle-switch" :class="{ active: autoSubmit }" @click="autoSubmit = !autoSubmit">
                      <div class="toggle-knob" />
                    </div>
                  </div>
                </div>
              </div>

              <!-- 考试说明 -->
              <div class="publish-field">
                <label class="publish-label">考试说明</label>
                <textarea
                  v-model="description"
                  class="desc-textarea"
                  placeholder="输入本次考试的说明（选填）..."
                  rows="3"
                />
              </div>
            </div>

            <!-- 弹窗底部 -->
            <div class="publish-dialog-footer">
              <div class="footer-summary">
                <span v-if="selectedClassIds.length > 0" class="footer-summary-text">
                  将发布至 <strong>{{ selectedClassIds.length }}</strong> 个班级，{{ duration }} 分钟
                </span>
                <span v-else class="footer-summary-text footer-summary-muted">
                  请选择发布班级
                </span>
              </div>
              <div class="footer-actions">
                <button class="dialog-btn dialog-btn-cancel" @click="closePublishDialog">取消</button>
                <button
                  class="dialog-btn dialog-btn-publish"
                  :disabled="!canPublish() || publishing"
                  @click="handlePublish"
                >
                  {{ publishing ? '发布中...' : '确认发布' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </TeacherLayout>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700;800&display=swap');

.assign-page {
  max-width: 960px;
  margin: 0 auto;
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(0, 102, 255, 0.12);
  border-radius: 12px;
  color: #3a5a7a;
  font-size: 14px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.25s ease;
  flex-shrink: 0;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.95);
  color: #0066ff;
  transform: translateX(-2px);
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: #1a3a5a;
  margin: 0;
  letter-spacing: -0.3px;
}

/* ===== 主卡片 ===== */
.wizard-card {
  background: rgba(255, 255, 255, 0.78);
  border-radius: 28px;
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 12px 48px rgba(0, 102, 255, 0.08), 0 4px 16px rgba(0, 102, 255, 0.04);
  overflow: hidden;
}

/* ===== 步骤指示器 ===== */
.step-indicator {
  display: flex;
  align-items: center;
  padding: 32px 40px 0;
}

.step-node {
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
  flex: 1;
}

.step-node.step-clickable { cursor: pointer; }

.step-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
  flex-shrink: 0;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  background: rgba(0, 102, 255, 0.08);
  color: #7a9aba;
  border: 2px solid transparent;
}

.step-done .step-circle {
  background: linear-gradient(135deg, #0066ff, #00d4ff);
  color: #fff;
  box-shadow: 0 4px 12px rgba(0, 102, 255, 0.3);
}

.step-active .step-circle {
  background: #fff;
  color: #0066ff;
  border-color: #0066ff;
  box-shadow: 0 0 0 4px rgba(0, 102, 255, 0.12);
}

.step-label {
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.step-label-icon { font-size: 16px; }

.step-label-text {
  font-size: 14px;
  font-weight: 500;
  color: #3a5a7a;
}

.step-active .step-label-text {
  color: #0066ff;
  font-weight: 600;
}

.step-done .step-label-text { color: #0066ff; }

.step-connector {
  flex: 1;
  height: 2px;
  background: rgba(0, 102, 255, 0.12);
  margin: 0 16px;
  border-radius: 1px;
  transition: background 0.4s ease;
  min-width: 24px;
}

.connector-done {
  background: linear-gradient(90deg, #0066ff, #00d4ff);
}

/* ===== 步骤内容 ===== */
.step-content {
  animation: slideIn 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

.step-panel {
  padding: 32px 40px 24px;
}

.step-panel-header {
  margin-bottom: 28px;
}

.panel-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a3a5a;
  margin: 0 0 6px;
}

.panel-desc {
  font-size: 14px;
  color: #7a9aba;
  margin: 0;
}

/* ===== 步骤 1：标题输入 ===== */
.form-area { max-width: 560px; }

.input-group { margin-bottom: 20px; }

.input-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #3a5a7a;
  margin-bottom: 8px;
}

.required { color: #ff4d4f; }

.title-input {
  width: 100%;
  padding: 14px 18px;
  font-size: 17px;
  font-weight: 500;
  font-family: inherit;
  color: #1a3a5a;
  background: rgba(0, 102, 255, 0.03);
  border: 2px solid rgba(0, 102, 255, 0.12);
  border-radius: 14px;
  outline: none;
  box-sizing: border-box;
  transition: all 0.3s ease;
}

.title-input::placeholder { color: #b0c8e0; font-weight: 400; }

.title-input:focus {
  border-color: #0066ff;
  box-shadow: 0 0 0 4px rgba(0, 102, 255, 0.08);
  background: #fff;
}

.input-meta {
  display: flex;
  justify-content: flex-end;
  margin-top: 6px;
}

.char-count { font-size: 12px; color: #b0c8e0; }

.quick-templates {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-label { font-size: 13px; color: #7a9aba; flex-shrink: 0; }

.quick-chip {
  padding: 6px 14px;
  background: rgba(0, 102, 255, 0.06);
  border: 1px solid rgba(0, 102, 255, 0.12);
  border-radius: 20px;
  font-size: 13px;
  font-family: inherit;
  color: #0066ff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-chip:hover {
  background: rgba(0, 102, 255, 0.12);
  transform: translateY(-1px);
}

/* ===== 步骤 2：题库选择 ===== */
.bank-loading,
.bank-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 12px;
  color: #7a9aba;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid rgba(0, 102, 255, 0.1);
  border-top-color: #0066ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.bank-empty .empty-icon { color: #c0d8ee; margin-bottom: 8px; }
.bank-empty .empty-text { font-size: 14px; color: #7a9aba; margin: 0; }

.link-btn {
  padding: 8px 20px;
  background: rgba(0, 102, 255, 0.08);
  border: none;
  border-radius: 10px;
  color: #0066ff;
  font-size: 13px;
  font-family: inherit;
  font-weight: 500;
  cursor: pointer;
}

.link-btn:hover { background: rgba(0, 102, 255, 0.15); }

.bank-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}

.bank-card {
  position: relative;
  padding: 18px 20px;
  background: rgba(255, 255, 255, 0.6);
  border: 2px solid rgba(0, 102, 255, 0.08);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  overflow: hidden;
}

.bank-card:hover {
  border-color: rgba(0, 102, 255, 0.25);
  background: rgba(255, 255, 255, 0.8);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 102, 255, 0.08);
}

.bank-selected {
  border-color: #0066ff !important;
  background: rgba(0, 102, 255, 0.04) !important;
  box-shadow: 0 0 0 4px rgba(0, 102, 255, 0.08);
}

.bank-card-top { display: flex; align-items: flex-start; gap: 12px; margin-bottom: 10px; }
.bank-radio { flex-shrink: 0; padding-top: 2px; }

.bank-radio-dot {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 2px solid rgba(0, 102, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.bank-radio-dot.radio-checked { border-color: #0066ff; background: #0066ff; color: #fff; }

.bank-cover {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(0, 102, 255, 0.08), rgba(0, 212, 255, 0.06));
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #0066ff;
}

.bank-meta { flex: 1; min-width: 0; }

.bank-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a3a5a;
  margin: 0 0 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bank-count { font-size: 13px; color: #7a9aba; }
.bank-card-bottom { padding-left: 34px; }
.bank-date { font-size: 12px; color: #b0c8e0; }

.bank-selected-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  background: linear-gradient(135deg, #0066ff, #00d4ff);
  border-radius: 20px;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
}

/* ===== 步骤 3：分数设置 ===== */
.score-summary-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: linear-gradient(135deg, rgba(0, 102, 255, 0.04), rgba(0, 212, 255, 0.02));
  border: 1px solid rgba(0, 102, 255, 0.08);
  border-radius: 14px;
  margin-bottom: 20px;
}

.summary-item { display: flex; flex-direction: column; gap: 2px; }
.summary-label { font-size: 12px; color: #7a9aba; font-weight: 500; }
.summary-value { font-size: 18px; font-weight: 700; color: #1a3a5a; }
.value-warning { color: #f59e0b; }
.summary-total { flex: 1; }
.summary-value-total { font-size: 26px; font-weight: 800; color: #0066ff; letter-spacing: -0.5px; }
.summary-unit { font-size: 13px; color: #7a9aba; font-weight: 500; }
.summary-divider { width: 1px; height: 36px; background: rgba(0, 102, 255, 0.1); }
.summary-actions { margin-left: auto; }

.ghost-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(0, 102, 255, 0.12);
  border-radius: 10px;
  color: #0066ff;
  font-size: 13px;
  font-family: inherit;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.ghost-btn:hover {
  background: rgba(255, 255, 255, 0.95);
  border-color: rgba(0, 102, 255, 0.25);
}

.questions-empty {
  text-align: center;
  padding: 60px 20px;
  color: #7a9aba;
  font-size: 14px;
}

/* Search */
.share-search-box {
  position: relative;
  margin-bottom: 16px;
  max-width: 320px;
}

.search-svg {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #b0c8e0;
  pointer-events: none;
}

.share-search-input {
  width: 100%;
  padding: 9px 12px 9px 34px;
  background: rgba(0, 102, 255, 0.03);
  border: 1px solid rgba(0, 102, 255, 0.1);
  border-radius: 10px;
  font-size: 13px;
  font-family: inherit;
  color: #1a3a5a;
  outline: none;
  box-sizing: border-box;
  transition: all 0.25s ease;
}

.share-search-input::placeholder { color: #b0c8e0; }
.share-search-input:focus { border-color: rgba(0, 102, 255, 0.25); background: rgba(255, 255, 255, 0.85); }

.question-list-scroll {
  max-height: 420px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 102, 255, 0.1) transparent;
}

.q-group { margin-bottom: 20px; }

.q-group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 4px 10px;
  border-bottom: 1.5px solid rgba(0, 102, 255, 0.06);
  margin-bottom: 8px;
}

.qg-numeral { font-size: 12px; font-weight: 700; color: var(--g-color); opacity: 0.5; }
.qg-label { font-size: 14px; font-weight: 600; color: var(--g-color); flex: 1; }
.qg-count { font-size: 12px; color: #7a9aba; }

.q-group-body { display: flex; flex-direction: column; gap: 6px; }

.question-row {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(0, 102, 255, 0.06);
  border-radius: 12px;
}

.qr-index {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(0, 102, 255, 0.08);
  color: #0066ff;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.qr-body { flex: 1; min-width: 0; display: flex; align-items: center; gap: 16px; }
.qr-stem { flex: 1; font-size: 14px; color: #1a3a5a; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.qr-score-group { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.qr-score-label { font-size: 13px; color: #7a9aba; font-weight: 500; }
.qr-score-input-wrap { display: flex; align-items: center; gap: 4px; }

.qr-score-input {
  width: 72px;
  padding: 6px 10px;
  font-size: 15px;
  font-weight: 600;
  font-family: inherit;
  color: #0066ff;
  background: rgba(0, 102, 255, 0.04);
  border: 1.5px solid rgba(0, 102, 255, 0.12);
  border-radius: 8px;
  outline: none;
  text-align: center;
  transition: all 0.2s ease;
  -moz-appearance: textfield;
}

.qr-score-input::-webkit-outer-spin-button,
.qr-score-input::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }

.qr-score-input:focus {
  border-color: #0066ff;
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.1);
  background: #fff;
}

.qr-score-unit { font-size: 13px; color: #7a9aba; }

/* ===== 底部操作栏 ===== */
.wizard-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 40px;
  border-top: 1px solid rgba(0, 102, 255, 0.06);
  background: rgba(255, 255, 255, 0.4);
}

.footer-left { flex: 1; min-width: 0; }

.step-breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #7a9aba;
}

.crumb-icon { flex-shrink: 0; }

.crumb-value {
  color: #3a5a7a;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.footer-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.footer-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.btn-prev { background: rgba(0, 102, 255, 0.06); color: #5a7a9a; }
.btn-prev:hover { background: rgba(0, 102, 255, 0.1); }

.btn-next {
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  color: #fff;
}

.btn-next:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(0, 102, 255, 0.25);
}

.btn-draft {
  background: rgba(255, 255, 255, 0.85);
  color: #5a7a9a;
  border: 1px solid rgba(0, 102, 255, 0.12);
}

.btn-draft:hover:not(:disabled) {
  background: #fff;
  border-color: rgba(0, 102, 255, 0.25);
  transform: translateY(-1px);
}

.btn-publish {
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  color: #fff;
}

.btn-publish:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(0, 102, 255, 0.25);
}

/* ===== 发布弹窗 ===== */
.dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 30, 60, 0.45);
  backdrop-filter: blur(6px);
}

.publish-dialog {
  width: 580px;
  max-width: 92vw;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(24px);
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 30px 80px rgba(0, 102, 255, 0.18), 0 12px 32px rgba(0, 102, 255, 0.08);
  overflow: hidden;
}

.publish-dialog-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 24px 28px 0;
}

.publish-icon {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(0, 102, 255, 0.1), rgba(0, 212, 255, 0.06));
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0066ff;
  flex-shrink: 0;
}

.publish-dialog-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a3a5a;
  margin: 0 0 4px;
}

.publish-dialog-desc { font-size: 13px; color: #7a9aba; margin: 0; }

.dialog-close-btn {
  margin-left: auto;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 102, 255, 0.06);
  color: #7a9aba;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.dialog-close-btn:hover { background: rgba(0, 102, 255, 0.12); color: #3a5a7a; }

.publish-dialog-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px 28px;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.publish-field { display: flex; flex-direction: column; gap: 8px; }
.publish-label { font-size: 14px; font-weight: 600; color: #3a5a7a; }
.field-hint { font-weight: 400; color: #b0c8e0; font-size: 12px; }

.publish-row { display: flex; gap: 16px; }
.flex-1 { flex: 1; }

/* 课程选择 */
.course-select-grid {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 180px;
  overflow-y: auto;
  padding: 2px;
}

.course-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1.5px solid rgba(0, 102, 255, 0.08);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.course-option:hover { border-color: rgba(0, 102, 255, 0.2); background: rgba(0, 102, 255, 0.03); }
.course-selected { border-color: #0066ff !important; background: rgba(0, 102, 255, 0.04) !important; }
.course-option-radio { flex-shrink: 0; }

.course-radio-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid rgba(0, 102, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.dot-checked { border-color: #0066ff; background: #0066ff; color: #fff; }

.course-option-body { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.course-option-name { font-size: 14px; font-weight: 500; color: #1a3a5a; }
.course-option-code { font-size: 12px; color: #b0c8e0; }
.course-empty { text-align: center; padding: 24px; color: #b0c8e0; font-size: 13px; }

/* 班级 */
.class-placeholder {
  padding: 24px; text-align: center; color: #b0c8e0; font-size: 13px; background: rgba(0, 102, 255, 0.03); border-radius: 12px;
}

.class-grid { display: flex; flex-wrap: wrap; gap: 8px; }

.class-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.6);
  border: 1.5px solid rgba(0, 102, 255, 0.08);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.class-chip:hover { border-color: rgba(0, 102, 255, 0.2); background: rgba(255, 255, 255, 0.85); }
.class-chip-selected { border-color: #0066ff !important; background: rgba(0, 102, 255, 0.05) !important; }

.class-chip-check {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  border: 2px solid rgba(0, 102, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
  transition: all 0.2s ease;
}

.class-chip-selected .class-chip-check { background: #0066ff; border-color: #0066ff; }
.class-chip-name { font-size: 13px; font-weight: 500; color: #1a3a5a; }
.class-chip-count { font-size: 12px; color: #b0c8e0; }

/* 时间输入 */
.time-input-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: rgba(0, 102, 255, 0.03);
  border: 1.5px solid rgba(0, 102, 255, 0.12);
  border-radius: 12px;
  transition: all 0.3s ease;
}

.time-input-wrap:focus-within {
  border-color: #0066ff;
  box-shadow: 0 0 0 4px rgba(0, 102, 255, 0.08);
  background: #fff;
}

.time-icon { color: #b0c8e0; flex-shrink: 0; }

.time-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  font-family: inherit;
  color: #1a3a5a;
  background: transparent;
}

.time-input::-webkit-calendar-picker-indicator { opacity: 0.4; cursor: pointer; }
.time-input::-webkit-calendar-picker-indicator:hover { opacity: 0.8; }

.duration-input { max-width: 80px; text-align: center; }
.duration-unit { font-size: 13px; color: #7a9aba; white-space: nowrap; }

/* 考试设置 */
.settings-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  padding: 14px 16px;
  background: rgba(0, 102, 255, 0.02);
  border: 1px solid rgba(0, 102, 255, 0.08);
  border-radius: 12px;
}

.setting-item { display: flex; flex-direction: column; gap: 6px; }
.setting-label { font-size: 13px; font-weight: 500; color: #5a7a9a; }
.setting-input-group { display: flex; align-items: center; gap: 6px; }

.setting-input {
  width: 70px;
  padding: 6px 10px;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  color: #1a3a5a;
  background: #fff;
  border: 1.5px solid rgba(0, 102, 255, 0.12);
  border-radius: 8px;
  outline: none;
  text-align: center;
  transition: all 0.2s ease;
  -moz-appearance: textfield;
}

.setting-input:focus { border-color: #0066ff; box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.08); }
.setting-unit { font-size: 12px; color: #7a9aba; }

.toggle-switch {
  width: 44px;
  height: 24px;
  border-radius: 12px;
  background: rgba(0, 102, 255, 0.15);
  position: relative;
  cursor: pointer;
  transition: all 0.3s ease;
}

.toggle-switch.active { background: linear-gradient(135deg, #0066ff, #00d4ff); }

.toggle-knob {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  position: absolute;
  top: 2px;
  left: 2px;
  transition: all 0.3s ease;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.15);
}

.toggle-switch.active .toggle-knob { left: 22px; }

/* 考试说明 */
.desc-textarea {
  width: 100%;
  padding: 12px 16px;
  background: rgba(0, 102, 255, 0.02);
  border: 1.5px solid rgba(0, 102, 255, 0.1);
  border-radius: 12px;
  font-size: 14px;
  font-family: inherit;
  color: #1a3a5a;
  outline: none;
  box-sizing: border-box;
  transition: all 0.25s ease;
  line-height: 1.6;
  resize: vertical;
}

.desc-textarea::placeholder { color: #b0c8e0; }
.desc-textarea:focus { border-color: #0066ff; box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.08); background: #fff; }

/* 弹窗底部 */
.publish-dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 28px;
  border-top: 1px solid rgba(0, 102, 255, 0.06);
  background: rgba(255, 255, 255, 0.5);
}

.footer-summary-text { font-size: 13px; color: #3a5a7a; }
.footer-summary-text strong { color: #0066ff; }
.footer-summary-muted { color: #b0c8e0; }

.footer-actions { display: flex; gap: 10px; }

.dialog-btn {
  padding: 10px 24px;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.3s ease;
}

.dialog-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.dialog-btn-cancel { background: rgba(0, 102, 255, 0.06); color: #5a7a9a; }
.dialog-btn-cancel:hover { background: rgba(0, 102, 255, 0.1); }

.dialog-btn-publish {
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  color: #fff;
}

.dialog-btn-publish:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(0, 102, 255, 0.25);
}

/* 弹窗动画 */
.dialog-enter-active,
.dialog-leave-active { transition: all 0.3s ease; }
.dialog-enter-from,
.dialog-leave-to { opacity: 0; }
.dialog-enter-from .publish-dialog,
.dialog-leave-to .publish-dialog { transform: translateY(-20px) scale(0.96); }

.fade-enter-active,
.fade-leave-active { transition: opacity 0.25s ease; }
.fade-enter-from,
.fade-leave-to { opacity: 0; }

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .page-header { flex-wrap: wrap; }
  .step-indicator { padding: 24px 20px 0; }
  .step-label-text { display: none; }
  .step-connector { margin: 0 8px; min-width: 16px; }
  .step-panel { padding: 24px 20px; }

  .wizard-footer {
    flex-direction: column;
    gap: 12px;
    padding: 16px 20px;
  }

  .footer-left { width: 100%; }
  .footer-right { width: 100%; justify-content: stretch; }
  .footer-btn { flex: 1; justify-content: center; }
  .bank-grid { grid-template-columns: 1fr; }
  .score-summary-bar { flex-wrap: wrap; }
  .summary-actions { margin-left: 0; width: 100%; }
  .ghost-btn { justify-content: center; width: 100%; }

  .publish-dialog { max-height: 90vh; border-radius: 20px; }
  .publish-dialog-header,
  .publish-dialog-body,
  .publish-dialog-footer { padding-left: 20px; padding-right: 20px; }
  .publish-dialog-footer { flex-direction: column; gap: 12px; }
  .footer-actions { width: 100%; }
  .dialog-btn { flex: 1; text-align: center; }
  .publish-row { flex-direction: column; }
  .settings-grid { grid-template-columns: 1fr; }
}

@media (max-width: 480px) {
  .page-title { font-size: 20px; }
  .step-circle { width: 30px; height: 30px; font-size: 13px; }
  .wizard-card { border-radius: 20px; }
}
</style>
