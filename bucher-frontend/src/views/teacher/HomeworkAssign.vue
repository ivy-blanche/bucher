<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import TeacherLayout from '@/layouts/TeacherLayout.vue'
import { getQuestionBankList, getQuestionList, getQuestionDetail, batchSaveQuestions } from '@/api/question'
import { getCourseList } from '@/api/course'
import {
  saveHomeworkDraft,
  publishHomework,
  getCourseClassList,
} from '@/api/homework'
import type { QuestionBankItem, QuestionOption, BatchSaveQuestion } from '@/api/question'
import type { CourseListItem } from '@/api/course'
import type { CourseClassItem } from '@/api/homework'

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
  options: QuestionOption[]
  answer: string | string[]
  analysis: string
  score: number
  _fullyLoaded: boolean
}

interface EditForm {
  questionId: string
  type: number
  typeName: string
  content: string
  options: QuestionOption[]
  answer: string | string[]
  analysis: string
  score: number
}

const questions = ref<QuestionWithScore[]>([])
const totalScore = computed(() => {
  const sum = questions.value.reduce((acc, q) => acc + (Number(q.score) || 0), 0)
  return Math.round(sum * 100) / 100
})

const scoredCount = computed(() => questions.value.filter((q) => (Number(q.score) || 0) > 0).length)

/* ==================== 题目编辑状态 ==================== */
const selectedQId = ref<string | null>(null)
const dirtyIds = ref<Set<string>>(new Set())
const savingBank = ref(false)

const selectedQuestion = computed(() => {
  if (!selectedQId.value) return null
  return questions.value.find((q) => q.questionId === selectedQId.value) || null
})

const currentIsDirty = computed(() => selectedQId.value !== null && dirtyIds.value.has(selectedQId.value))

const editForm = ref<EditForm>({
  questionId: '', type: 1, typeName: '', content: '', options: [], answer: '', analysis: '', score: 0,
})

/* ==================== 题型配置 ==================== */
const typeColorMap: Record<number, string> = {
  1: '#0066ff', 2: '#7c3aed', 3: '#0891b2', 4: '#d97706',
}
const typeLabelMap: Record<number, string> = {
  1: '单选题', 2: '多选题', 3: '填空题', 4: '简答题',
}
const typeIconMap: Record<number, string> = {
  1: '○', 2: '□', 3: '＿', 4: '〰',
}
const typeOrder: number[] = [1, 2, 3, 4]
const typeNumeral: Record<number, string> = {
  1: '一', 2: '二', 3: '三', 4: '四',
}

const searchQueryE = ref('')

const groupedEditorQuestions = computed(() => {
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

/* ==================== 外部数据 ==================== */
const bankList = ref<QuestionBankItem[]>([])
const bankLoading = ref(false)

// 发布弹窗数据
const showPublishDialog = ref(false)
const publishing = ref(false)
const courseList = ref<CourseListItem[]>([])
const courseLoading = ref(false)
const selectedCourseId = ref<string | null>(null)
const classList = ref<CourseClassItem[]>([])
const classLoading = ref(false)
const selectedClassIds = ref<string[]>([])
const deadline = ref('')

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
    if (currentStep.value === 0) ElMessage.warning('请填写作业名称')
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
  // Going forward — validate intermediate steps
  for (let i = currentStep.value; i < index; i++) {
    if (i === 0 && !title.value.trim()) {
      ElMessage.warning('请先填写作业名称')
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
      options: [],
      answer: '',
      analysis: '',
      score: defaultScore,
      _fullyLoaded: false,
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
  // also update edit form if open
  if (selectedQId.value) {
    const q = questions.value.find((x) => x.questionId === selectedQId.value)
    if (q) editForm.value.score = q.score
  }
  ElMessage.success(`已平均分配，每题 ${even} 分，总分 ${totalScore.value}`)
}

/* ==================== 题目编辑 ==================== */
async function selectQuestion(qId: string) {
  flushDirtyState()
  // write back current form to questions array
  if (selectedQId.value && editForm.value.questionId) {
    const idx = questions.value.findIndex((q) => q.questionId === editForm.value.questionId)
    if (idx !== -1) {
      questions.value[idx] = {
        ...questions.value[idx],
        content: editForm.value.content,
        options: JSON.parse(JSON.stringify(editForm.value.options)),
        answer: Array.isArray(editForm.value.answer) ? [...editForm.value.answer] : editForm.value.answer,
        analysis: editForm.value.analysis,
        score: editForm.value.score,
        _fullyLoaded: questions.value[idx]._fullyLoaded,
      }
    }
  }

  selectedQId.value = qId
  const q = questions.value.find((item) => item.questionId === qId)
  if (!q) return

  // lazy load full detail
  if (q.questionId && !q._fullyLoaded) {
    await loadQuestionDetail(q.questionId)
    const updated = questions.value.find((item) => item.questionId === qId)
    if (!updated) return
    editForm.value = {
      questionId: updated.questionId,
      type: updated.type,
      typeName: updated.typeName,
      content: updated.content,
      options: JSON.parse(JSON.stringify(updated.options)),
      answer: Array.isArray(updated.answer) ? [...updated.answer] : updated.answer,
      analysis: updated.analysis,
      score: updated.score,
    }
    return
  }

  editForm.value = {
    questionId: q.questionId,
    type: q.type,
    typeName: q.typeName,
    content: q.content,
    options: JSON.parse(JSON.stringify(q.options)),
    answer: Array.isArray(q.answer) ? [...q.answer] : q.answer,
    analysis: q.analysis,
    score: q.score,
  }
}

async function loadQuestionDetail(qId: string) {
  const idx = questions.value.findIndex((q) => q.questionId === qId)
  if (idx === -1) return
  if (questions.value[idx]._fullyLoaded) return

  try {
    const detail = await getQuestionDetail(qId)
    const options: QuestionOption[] = (detail.options || []).map((opt) => ({
      id: opt.id,
      label: opt.label,
      content: opt.content,
      isCorrect: opt.isCorrect,
      sortOrder: opt.sortOrder,
    }))
    const answer: string | string[] =
      detail.type === 2 && detail.answer
        ? detail.answer.split(',').filter(Boolean)
        : detail.answer || ''

    questions.value[idx] = {
      ...questions.value[idx],
      content: detail.content,
      options,
      answer,
      analysis: detail.analysis || '',
      _fullyLoaded: true,
    }
  } catch {
    ElMessage.error('加载题目详情失败')
  }
}

function flushDirtyState() {
  const qId = selectedQId.value
  if (!qId) return

  const q = questions.value.find((item) => item.questionId === qId)
  if (!q) {
    dirtyIds.value = new Set(dirtyIds.value).add(qId)
    return
  }

  const f = editForm.value
  const changed =
    f.content !== q.content ||
    f.analysis !== q.analysis ||
    f.score !== q.score ||
    JSON.stringify(f.options) !== JSON.stringify(q.options) ||
    JSON.stringify(f.answer) !== JSON.stringify(q.answer)

  const next = new Set(dirtyIds.value)
  if (changed) {
    next.add(qId)
  } else {
    next.delete(qId)
  }
  dirtyIds.value = next
}

async function saveToBank() {
  flushDirtyState()
  if (dirtyIds.value.size === 0) {
    ElMessage.info('没有需要保存的修改')
    return
  }

  savingBank.value = true
  try {
    const ids = [...dirtyIds.value]
    const payload: BatchSaveQuestion[] = ids.map((id) => {
      const q = questions.value.find((item) => item.questionId === id)
      if (!q) {
        return { id: null, type: 1, content: '', answer: '', analysis: '', options: [], deleted: true }
      }
      const options = q.options.map((opt) => ({
        id: opt.id,
        label: opt.label,
        content: opt.content,
        isCorrect: opt.isCorrect,
        sortOrder: opt.sortOrder,
      }))
      let answer = ''
      if (q.type === 1 || q.type === 2) {
        answer = ''
      } else {
        answer = typeof q.answer === 'string' ? q.answer : q.answer.join(',')
      }
      return {
        id: q.questionId,
        type: q.type,
        content: q.content,
        answer,
        analysis: q.analysis,
        options,
        deleted: false,
      }
    })

    await batchSaveQuestions({ groupId: sourceBankId.value!, questions: payload })
    dirtyIds.value = new Set()
    ElMessage.success(`已保存 ${ids.length} 道题目到题库`)
  } catch {
    // error handled by interceptor
  } finally {
    savingBank.value = false
  }
}

function revertQuestion() {
  const qId = selectedQId.value
  if (!qId) return

  const q = questions.value.find((item) => item.questionId === qId)
  if (!q) return

  editForm.value = {
    questionId: q.questionId,
    type: q.type,
    typeName: q.typeName,
    content: q.content,
    options: JSON.parse(JSON.stringify(q.options)),
    answer: Array.isArray(q.answer) ? [...q.answer] : q.answer,
    analysis: q.analysis,
    score: q.score,
  }

  const next = new Set(dirtyIds.value)
  next.delete(qId)
  dirtyIds.value = next
}

/* ==================== 选项管理 ==================== */
function addOption() {
  if (editForm.value.options.length >= 6) {
    ElMessage.warning('最多支持 6 个选项')
    return
  }
  const nextLabel = String.fromCharCode(65 + editForm.value.options.length)
  editForm.value.options.push({ label: nextLabel, content: '', isCorrect: false, sortOrder: editForm.value.options.length })
}

function removeOption(index: number) {
  if (editForm.value.options.length <= 2) {
    ElMessage.warning('至少保留 2 个选项')
    return
  }
  editForm.value.options.splice(index, 1)
  editForm.value.options.forEach((opt, i) => {
    opt.label = String.fromCharCode(65 + i)
    opt.sortOrder = i
  })
  if (!Array.isArray(editForm.value.answer)) {
    const label = String.fromCharCode(65 + index)
    if (editForm.value.answer === label) {
      editForm.value.answer = ''
    }
  }
}

function toggleMultiAnswer(label: string) {
  const arr = editForm.value.answer as string[]
  const idx = arr.indexOf(label)
  if (idx === -1) {
    arr.push(label)
  } else {
    arr.splice(idx, 1)
  }
}

/* ==================== 保存草稿 ==================== */
const saving = ref(false)

async function handleSaveDraft() {
  if (!validateForm()) return
  saving.value = true
  try {
    await saveHomeworkDraft({
      title: title.value.trim(),
      courseId: selectedCourseId.value || '',
      sourceBankId: sourceBankId.value!,
      questions: questions.value.map((q) => ({
        questionId: q.questionId,
        score: q.score,
      })),
    })
    ElMessage.success('草稿保存成功')
    router.push('/teacher/homework')
  } catch {
    // 错误由拦截器统一处理
  } finally {
    saving.value = false
  }
}

/* ==================== 发布弹窗 ==================== */
function openPublishDialog() {
  if (!validateForm()) return
  selectedCourseId.value = null
  selectedClassIds.value = []
  deadline.value = ''
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
    && deadline.value.trim() !== ''
}

async function handlePublish() {
  if (!canPublish()) {
    ElMessage.warning('请选择班级和截止日期')
    return
  }
  if (new Date(deadline.value) <= new Date()) {
    ElMessage.warning('截止日期必须在当前时间之后')
    return
  }
  publishing.value = true
  try {
    await publishHomework({
      title: title.value.trim(),
      sourceBankId: sourceBankId.value!,
      questions: questions.value.map((q) => ({
        questionId: q.questionId,
        score: q.score,
      })),
      courseId: selectedCourseId.value!,
      classIds: selectedClassIds.value,
      deadline: deadline.value,
    })
    ElMessage.success('作业发布成功')
    closePublishDialog()
    router.push('/teacher/homework')
  } catch {
    // 错误由拦截器统一处理
  } finally {
    publishing.value = false
  }
}

/* ==================== 通用校验 ==================== */
function validateForm(): boolean {
  if (!title.value.trim()) {
    ElMessage.warning('请填写作业名称')
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

onMounted(() => {
  loadBankList()
})
</script>

<template>
  <TeacherLayout>
    <div class="assign-page">
      <!-- 页面头部 -->
      <div class="page-header">
        <button class="back-btn" @click="router.push('/teacher/homework')">
          <svg viewBox="0 0 20 20" width="18" height="18">
            <polyline points="12,4 6,10 12,16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <span>返回作业列表</span>
        </button>
        <h1 class="page-title">布置作业</h1>
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
              <h2 class="panel-title">设置作业名称与课程</h2>
              <p class="panel-desc">为你的作业取一个清晰的名称，并选择所属课程。</p>
            </div>
            <div class="form-area">
              <div class="input-group">
                <label class="input-label">作业名称 <span class="required">*</span></label>
                <input
                  v-model="title"
                  type="text"
                  class="title-input"
                  placeholder="例：第 3 章 Java 集合框架课后练习"
                  maxlength="100"
                  @keyup.enter="goNext"
                />
                <div class="input-meta">
                  <span class="char-count">{{ title.length }} / 100</span>
                </div>
              </div>

              <!-- 快捷占位 -->
              <div class="quick-templates">
                <span class="quick-label">快速填入：</span>
                <button
                  v-for="tpl in ['第 3 章课后练习', '期中复习作业', '综合实践任务']"
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
              <p class="panel-desc">选择一个题库作为作业题目来源，之后可为每道题设置分数。</p>
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
                <!-- 选中遮罩 -->
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

        <!-- ============ 步骤 3：分数设置 + 题目编辑 ============ -->
        <div v-show="currentStep === 2" class="step-content">
          <div class="step-panel">
            <div class="step-panel-header">
              <h2 class="panel-title">设置分数与编辑题目</h2>
              <p class="panel-desc">
                在左侧选择题目进行编辑，在右侧修改题目内容并设置分数。
              </p>
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
                <button
                  v-if="dirtyIds.size > 0"
                  class="ghost-btn btn-save-bank"
                  :disabled="savingBank"
                  @click="saveToBank"
                >
                  <svg viewBox="0 0 16 16" width="14" height="14">
                    <path d="M13 4l-7 7-3-3" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                  <span>{{ savingBank ? '保存中...' : `保存 ${dirtyIds.size} 题到题库` }}</span>
                </button>
              </div>
            </div>

            <!-- 空题目 -->
            <div v-if="questions.length === 0" class="questions-empty">
              <p>所选题库暂无题目，请返回选择其他题库。</p>
            </div>

            <!-- 分栏编辑区 -->
            <div v-else class="split-editor">
              <!-- 左侧：题目列表 -->
              <aside class="editor-sidebar">
                <div class="sidebar-search-box">
                  <svg class="search-svg" viewBox="0 0 20 20" width="14" height="14">
                    <circle cx="8.5" cy="8.5" r="5.5" fill="none" stroke="currentColor" stroke-width="1.5" />
                    <line x1="12.5" y1="12.5" x2="17" y2="17" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                  </svg>
                  <input v-model="searchQueryE" class="sidebar-search-input" placeholder="搜索题目..." />
                </div>
                <div v-if="dirtyIds.size > 0" class="sidebar-dirty-bar">
                  <span class="dirty-bar-text">{{ dirtyIds.size }} 项未保存到题库</span>
                </div>
                <div class="sidebar-list">
                  <div
                    v-for="group in groupedEditorQuestions"
                    :key="group.type"
                    class="editor-group"
                  >
                    <div class="editor-group-header" :style="{ '--g-color': typeColorMap[group.type] }">
                      <span class="eg-numeral">{{ group.numeral }}</span>
                      <span class="eg-label">{{ group.label }}</span>
                      <span class="eg-count">{{ group.questions.length }}</span>
                    </div>
                    <div class="editor-group-items">
                      <button
                        v-for="q in group.questions"
                        :key="q.questionId"
                        class="editor-item"
                        :class="{
                          'item-active': selectedQId === q.questionId,
                          'item-dirty': dirtyIds.has(q.questionId),
                        }"
                        @click="selectQuestion(q.questionId)"
                      >
                        <span class="ei-icon" :style="{ color: typeColorMap[q.type] }">{{ typeIconMap[q.type] }}</span>
                        <span class="ei-stem">{{ q.contentPreview }}</span>
                        <span class="ei-score-badge">{{ q.score }}分</span>
                        <span v-if="dirtyIds.has(q.questionId)" class="ei-dirty-dot" title="有未保存的修改" />
                      </button>
                    </div>
                  </div>
                  <div v-if="groupedEditorQuestions.length === 0 && searchQueryE" class="sidebar-no-match">
                    没有匹配的题目
                  </div>
                </div>
              </aside>

              <!-- 右侧：编辑面板 -->
              <main class="editor-main-panel">
                <template v-if="selectedQId && selectedQuestion">
                  <div class="editor-form">
                    <!-- Form Header -->
                    <div class="ef-header">
                      <div class="ef-type" :style="{ '--ef-color': typeColorMap[editForm.type] }">
                        <span class="ef-type-icon">{{ typeIconMap[editForm.type] }}</span>
                        <span>{{ editForm.typeName || typeLabelMap[editForm.type] }}</span>
                        <span v-if="currentIsDirty" class="ef-dirty-badge">未保存</span>
                      </div>
                      <div class="ef-actions">
                        <span class="ef-id-badge">#{{ editForm.questionId }}</span>
                      </div>
                    </div>

                    <!-- 题干 -->
                    <div class="ef-block">
                      <label class="ef-label">题干 <span class="required">*</span></label>
                      <textarea
                        v-model="editForm.content"
                        class="ef-textarea"
                        :class="{ 'has-text': editForm.content.trim() }"
                        placeholder="请输入题目题干..."
                        rows="3"
                        @input="flushDirtyState"
                      />
                    </div>

                    <!-- 选项（单选/多选） -->
                    <template v-if="editForm.type === 1 || editForm.type === 2">
                      <div class="ef-block">
                        <div class="ef-block-header">
                          <label class="ef-label">选项</label>
                          <button v-if="editForm.options.length < 6" class="btn-add-opt" @click="addOption">
                            <svg viewBox="0 0 16 16" width="13" height="13"><line x1="8" y1="2.5" x2="8" y2="13.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><line x1="2.5" y1="8" x2="13.5" y2="8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
                            <span>添加选项</span>
                          </button>
                        </div>
                        <div class="opt-list">
                          <div v-for="(opt, i) in editForm.options" :key="i" class="opt-row">
                            <div class="opt-select">
                              <button
                                v-if="editForm.type === 1"
                                class="opt-radio"
                                :class="{ 'opt-selected': editForm.answer === opt.label }"
                                @click="editForm.answer = opt.label; flushDirtyState()"
                              >
                                <span v-if="editForm.answer === opt.label" class="opt-radio-dot" />
                              </button>
                              <button
                                v-else
                                class="opt-checkbox"
                                :class="{ 'opt-selected': (editForm.answer as string[]).includes(opt.label) }"
                                @click="opt.isCorrect = !opt.isCorrect; toggleMultiAnswer(opt.label); flushDirtyState()"
                              >
                                <svg v-if="(editForm.answer as string[]).includes(opt.label)" viewBox="0 0 12 12" width="10" height="10">
                                  <polyline points="2,6 5,9 10,3" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                                </svg>
                              </button>
                            </div>
                            <span class="opt-label" :style="{ color: typeColorMap[editForm.type] }">{{ opt.label }}</span>
                            <input v-model="opt.content" class="opt-input" :placeholder="`选项 ${opt.label}`" @input="flushDirtyState" />
                            <button v-if="editForm.options.length > 2" class="opt-remove" @click="removeOption(i)">
                              <svg viewBox="0 0 16 16" width="12" height="12"><line x1="4" y1="4" x2="12" y2="12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><line x1="12" y1="4" x2="4" y2="12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
                            </button>
                          </div>
                        </div>
                        <p class="opt-hint">{{ editForm.type === 1 ? '点击 ○ 选择正确答案' : '点击 □ 选择所有正确答案' }}</p>
                      </div>
                    </template>

                    <!-- 填空 -->
                    <template v-if="editForm.type === 3">
                      <div class="ef-block">
                        <label class="ef-label">正确答案 <span class="required">*</span></label>
                        <div class="fill-wrap">
                          <span class="fill-prefix">答案：</span>
                          <input v-model="editForm.answer" class="ef-input" placeholder="填写空白处的答案" @input="flushDirtyState" />
                        </div>
                        <p class="ef-hint">在题干中使用 ______（6 个下划线）标记空白位置</p>
                      </div>
                    </template>

                    <!-- 简答 -->
                    <template v-if="editForm.type === 4">
                      <div class="ef-block">
                        <label class="ef-label">参考答案 <span class="required">*</span></label>
                        <textarea v-model="editForm.answer" class="ef-textarea" :class="{ 'has-text': (editForm.answer as string).trim() }" placeholder="请输入参考答案..." rows="4" @input="flushDirtyState" />
                      </div>
                    </template>

                    <!-- 解析 -->
                    <div class="ef-block">
                      <label class="ef-label">解析 <span class="opt-tag">选填</span></label>
                      <textarea v-model="editForm.analysis" class="ef-textarea" :class="{ 'has-text': editForm.analysis.trim() }" placeholder="输入题目解析，帮助学生理解..." rows="2" @input="flushDirtyState" />
                    </div>

                    <!-- 分数设置 -->
                    <div class="ef-divider" />
                    <div class="ef-block score-block">
                      <label class="ef-label">本题分数 <span class="required">*</span></label>
                      <div class="score-input-group">
                        <input
                          v-model.number="editForm.score"
                          type="number"
                          class="ef-score-input"
                          min="0"
                          max="999"
                          step="0.5"
                          @input="onScoreInput(editForm); flushDirtyState()"
                        />
                        <span class="score-unit">分</span>
                        <span v-if="editForm.score !== (questions.find(q => q.questionId === selectedQId)?.score ?? editForm.score)" class="score-change-hint">已修改</span>
                      </div>
                    </div>

                    <!-- 底部操作 -->
                    <div class="ef-actions">
                      <button class="ef-btn-revert" :disabled="!currentIsDirty" @click="revertQuestion">
                        <svg viewBox="0 0 16 16" width="13" height="13"><polyline points="4,8 8,4 12,8" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" /><line x1="8" y1="4" x2="8" y2="14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" /></svg>
                        <span>撤销修改</span>
                      </button>
                      <p class="ef-save-hint">
                        <template v-if="currentIsDirty">此题目有未保存的修改</template>
                        <template v-else>此题目已保存</template>
                      </p>
                    </div>
                  </div>
                </template>

                <!-- 未选择题目 -->
                <div v-else class="ef-empty">
                  <div class="ef-empty-graphic">
                    <svg viewBox="0 0 56 56" width="56" height="56">
                      <circle cx="28" cy="28" r="22" fill="none" stroke="currentColor" stroke-width="1.5" opacity="0.2" />
                      <line x1="28" y1="18" x2="28" y2="38" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.3" />
                      <line x1="18" y1="28" x2="38" y2="28" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.3" />
                    </svg>
                  </div>
                  <p class="ef-empty-text">在左侧选择题目开始编辑</p>
                </div>
              </main>
            </div>
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

            <!-- 最后一步显示操作按钮 -->
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
                <span>发布作业</span>
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
                <h3 class="publish-dialog-title">发布作业</h3>
                <p class="publish-dialog-desc">选择目标课程、班级并设置截止日期</p>
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

              <!-- 截止日期 -->
              <div class="publish-field">
                <label class="publish-label">截止日期 <span class="required">*</span></label>
                <div class="deadline-input-wrap">
                  <svg viewBox="0 0 16 16" width="16" height="16" class="deadline-icon">
                    <rect x="2" y="3" width="12" height="12" rx="2" fill="none" stroke="currentColor" stroke-width="1.5" />
                    <line x1="2" y1="7" x2="14" y2="7" stroke="currentColor" stroke-width="1.5" />
                    <line x1="5" y1="1" x2="5" y2="4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                    <line x1="11" y1="1" x2="11" y2="4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                  </svg>
                  <input
                    v-model="deadline"
                    type="datetime-local"
                    class="deadline-input"
                    :min="new Date().toISOString().slice(0, 16)"
                  />
                </div>
              </div>
            </div>

            <!-- 弹窗底部 -->
            <div class="publish-dialog-footer">
              <div class="footer-summary">
                <span v-if="selectedClassIds.length > 0" class="footer-summary-text">
                  将发布至 <strong>{{ selectedClassIds.length }}</strong> 个班级
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

/* ===== 页面头部 ===== */
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
  box-shadow:
    0 12px 48px rgba(0, 102, 255, 0.08),
    0 4px 16px rgba(0, 102, 255, 0.04);
  overflow: hidden;
}

/* ===== 步骤指示器 ===== */
.step-indicator {
  display: flex;
  align-items: center;
  padding: 32px 40px 0;
  gap: 0;
}

.step-node {
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
  flex: 1;
}

.step-node.step-clickable {
  cursor: pointer;
}

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

.step-label-icon {
  font-size: 16px;
}

.step-label-text {
  font-size: 14px;
  font-weight: 500;
  color: #3a5a7a;
  transition: color 0.3s ease;
}

.step-active .step-label-text {
  color: #0066ff;
  font-weight: 600;
}

.step-done .step-label-text {
  color: #0066ff;
}

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
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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
  line-height: 1.5;
}

/* ===== 步骤 1：标题输入 ===== */
.form-area {
  max-width: 560px;
}

.input-group {
  margin-bottom: 20px;
}

.input-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #3a5a7a;
  margin-bottom: 8px;
}

.required {
  color: #ff4d4f;
}

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

.title-input::placeholder {
  color: #b0c8e0;
  font-weight: 400;
}

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

.char-count {
  font-size: 12px;
  color: #b0c8e0;
}

.quick-templates {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-label {
  font-size: 13px;
  color: #7a9aba;
  flex-shrink: 0;
}

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

.empty-icon {
  color: #c0d8ee;
  margin-bottom: 8px;
}

.empty-text {
  font-size: 14px;
  color: #7a9aba;
  margin: 0;
}

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
  transition: all 0.2s ease;
}

.link-btn:hover {
  background: rgba(0, 102, 255, 0.15);
}

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

.bank-card-top {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 10px;
}

.bank-radio {
  flex-shrink: 0;
  padding-top: 2px;
}

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

.bank-radio-dot.radio-checked {
  border-color: #0066ff;
  background: #0066ff;
  color: #fff;
}

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

.bank-meta {
  flex: 1;
  min-width: 0;
}

.bank-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a3a5a;
  margin: 0 0 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bank-count {
  font-size: 13px;
  color: #7a9aba;
}

.bank-card-bottom {
  padding-left: 34px;
}

.bank-date {
  font-size: 12px;
  color: #b0c8e0;
}

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

/* 分数汇总条 */
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

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.summary-label {
  font-size: 12px;
  color: #7a9aba;
  font-weight: 500;
}

.summary-value {
  font-size: 18px;
  font-weight: 700;
  color: #1a3a5a;
}

.value-warning {
  color: #f59e0b;
}

.summary-total {
  flex: 1;
}

.summary-value-total {
  font-size: 26px;
  font-weight: 800;
  color: #0066ff;
  letter-spacing: -0.5px;
}

.summary-unit {
  font-size: 13px;
  color: #7a9aba;
  font-weight: 500;
}

.summary-divider {
  width: 1px;
  height: 36px;
  background: rgba(0, 102, 255, 0.1);
}

.summary-actions {
  margin-left: auto;
}

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

/* 题目列表 */
.questions-empty {
  text-align: center;
  padding: 60px 20px;
  color: #7a9aba;
  font-size: 14px;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.question-row {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding: 16px 18px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(0, 102, 255, 0.06);
  border-radius: 14px;
  transition: all 0.2s ease;
}

.question-row:hover {
  background: rgba(255, 255, 255, 0.8);
  border-color: rgba(0, 102, 255, 0.12);
}

.q-index {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(0, 102, 255, 0.08);
  color: #0066ff;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.q-body {
  flex: 1;
  min-width: 0;
}

.q-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.q-type-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  flex-shrink: 0;
}

.q-preview {
  font-size: 14px;
  color: #1a3a5a;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.q-bottom {
  display: flex;
  align-items: center;
}

.q-score-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.q-score-label {
  font-size: 13px;
  color: #7a9aba;
  font-weight: 500;
}

.q-score-input-wrap {
  display: flex;
  align-items: center;
  gap: 4px;
}

.q-score-input {
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

.q-score-input::-webkit-outer-spin-button,
.q-score-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.q-score-input:focus {
  border-color: #0066ff;
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.1);
  background: #fff;
}

.q-score-unit {
  font-size: 13px;
  color: #7a9aba;
}

/* ==========================================================
   分栏编辑器 (步骤 3)
   ========================================================== */

/* "保存到题库"按钮 */
.btn-save-bank {
  background: linear-gradient(135deg, rgba(0, 179, 0, 0.08), rgba(0, 200, 0, 0.04)) !important;
  border-color: rgba(0, 179, 0, 0.2) !important;
  color: #00a300 !important;
}
.btn-save-bank:hover {
  background: linear-gradient(135deg, rgba(0, 179, 0, 0.14), rgba(0, 200, 0, 0.08)) !important;
}

.split-editor {
  display: flex;
  gap: 0;
  border: 1px solid rgba(0, 102, 255, 0.08);
  border-radius: 16px;
  overflow: hidden;
  min-height: 420px;
  background: rgba(255, 255, 255, 0.5);
}

/* ===== 左侧边栏 ===== */
.editor-sidebar {
  width: 280px;
  min-width: 280px;
  background: rgba(248, 250, 255, 0.6);
  border-right: 1px solid rgba(0, 102, 255, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-search-box {
  position: relative;
  margin: 14px 14px 8px;
}

.search-svg {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #b0c8e0;
  pointer-events: none;
}

.sidebar-search-input {
  width: 100%;
  padding: 8px 12px 8px 34px;
  background: rgba(0, 102, 255, 0.04);
  border: 1px solid rgba(0, 102, 255, 0.1);
  border-radius: 10px;
  font-size: 13px;
  font-family: inherit;
  color: #1a3a5a;
  outline: none;
  box-sizing: border-box;
  transition: all 0.25s ease;
}

.sidebar-search-input::placeholder {
  color: #b0c8e0;
}

.sidebar-search-input:focus {
  border-color: rgba(0, 102, 255, 0.25);
  background: rgba(255, 255, 255, 0.85);
}

.sidebar-dirty-bar {
  margin: 0 14px 8px;
  padding: 6px 12px;
  background: rgba(217, 119, 6, 0.08);
  border-radius: 8px;
  font-size: 12px;
  color: #d97706;
  font-weight: 500;
  text-align: center;
}

.sidebar-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 14px 16px;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 102, 255, 0.1) transparent;
}

.sidebar-list::-webkit-scrollbar {
  width: 4px;
}

.sidebar-list::-webkit-scrollbar-thumb {
  background: rgba(0, 102, 255, 0.15);
  border-radius: 4px;
}

.editor-group {
  margin-bottom: 16px;
}

.editor-group:last-child {
  margin-bottom: 0;
}

.editor-group-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 4px 8px;
  border-bottom: 1.5px solid rgba(0, 102, 255, 0.06);
  margin-bottom: 4px;
}

.eg-numeral {
  font-size: 12px;
  font-weight: 700;
  color: var(--g-color);
  opacity: 0.5;
}

.eg-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--g-color);
  flex: 1;
}

.eg-count {
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  background: var(--g-color);
  padding: 1px 8px;
  border-radius: 20px;
  opacity: 0.8;
}

.editor-group-items {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.editor-item {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 8px 8px 8px 10px;
  background: transparent;
  border: none;
  border-radius: 8px;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.editor-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 5px;
  bottom: 5px;
  width: 3px;
  border-radius: 3px;
  background: transparent;
  transition: all 0.25s ease;
}

.editor-item:hover {
  background: rgba(0, 102, 255, 0.04);
}

.editor-item.item-active {
  background: rgba(0, 102, 255, 0.07);
}

.editor-item.item-active::before {
  background: linear-gradient(180deg, #0066ff, #00d4ff);
}

.editor-item.item-dirty.item-active::before {
  background: linear-gradient(180deg, #d97706, #f59e0b);
}

.ei-icon {
  font-size: 11px;
  flex-shrink: 0;
  width: 16px;
  text-align: center;
  opacity: 0.65;
}

.ei-stem {
  flex: 1;
  font-size: 12px;
  color: #3a5a7a;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ei-score-badge {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 600;
  color: #0066ff;
  background: rgba(0, 102, 255, 0.08);
  padding: 1px 7px;
  border-radius: 20px;
  white-space: nowrap;
}

.ei-dirty-dot {
  flex-shrink: 0;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #d97706;
  box-shadow: 0 0 5px rgba(217, 119, 6, 0.4);
  animation: dirty-pulse 2s ease-in-out infinite;
}

@keyframes dirty-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(0.85); }
}

.sidebar-no-match {
  text-align: center;
  padding: 40px 14px;
  color: #b0c8e0;
  font-size: 13px;
}

/* ===== 右侧编辑面板 ===== */
.editor-main-panel {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 102, 255, 0.1) transparent;
  background: rgba(255, 255, 255, 0.4);
}

.editor-main-panel::-webkit-scrollbar {
  width: 4px;
}

.editor-main-panel::-webkit-scrollbar-thumb {
  background: rgba(0, 102, 255, 0.15);
  border-radius: 4px;
}

.editor-form {
  max-width: 100%;
}

.ef-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1.5px solid rgba(0, 102, 255, 0.06);
}

.ef-type {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 14px;
  font-weight: 600;
  color: var(--ef-color);
}

.ef-type-icon {
  font-size: 16px;
}

.ef-dirty-badge {
  font-size: 11px;
  font-weight: 500;
  color: #d97706;
  background: rgba(217, 119, 6, 0.1);
  padding: 1px 8px;
  border-radius: 20px;
  margin-left: 4px;
}

.ef-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ef-id-badge {
  font-size: 11px;
  font-weight: 500;
  color: #b0c8e0;
  font-family: 'SF Mono', 'Fira Code', monospace;
}

.ef-block {
  margin-bottom: 18px;
}

.ef-block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.ef-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #3a5a7a;
  margin-bottom: 6px;
}

.ef-label .required {
  color: #ff4d4f;
}

.opt-tag {
  font-weight: 400;
  font-size: 11px;
  color: #b0c8e0;
  margin-left: 4px;
}

.ef-textarea {
  width: 100%;
  padding: 10px 14px;
  background: #fff;
  border: 1.5px solid rgba(0, 102, 255, 0.1);
  border-radius: 12px;
  font-size: 13px;
  font-family: inherit;
  color: #1a3a5a;
  outline: none;
  box-sizing: border-box;
  transition: all 0.25s ease;
  line-height: 1.6;
  resize: vertical;
}

.ef-textarea::placeholder {
  color: #c0d4e8;
}

.ef-textarea:focus {
  border-color: #0066ff;
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.08);
}

.ef-textarea.has-text {
  background: #fff;
}

.ef-input {
  width: 100%;
  padding: 9px 14px;
  background: #fff;
  border: 1.5px solid rgba(0, 102, 255, 0.1);
  border-radius: 10px;
  font-size: 13px;
  font-family: inherit;
  color: #1a3a5a;
  outline: none;
  box-sizing: border-box;
  transition: all 0.25s ease;
}

.ef-input::placeholder {
  color: #c0d4e8;
}

.ef-input:focus {
  border-color: #0066ff;
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.08);
}

/* 选项 */
.opt-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.opt-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 8px 5px 5px;
  background: #fff;
  border: 1.5px solid rgba(0, 102, 255, 0.07);
  border-radius: 10px;
  transition: all 0.2s ease;
}

.opt-row:focus-within {
  border-color: rgba(0, 102, 255, 0.2);
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.04);
}

.opt-select {
  flex-shrink: 0;
}

.opt-radio,
.opt-checkbox {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: 2px solid rgba(0, 102, 255, 0.2);
  background: transparent;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 0;
}

.opt-radio {
  border-radius: 50%;
}

.opt-radio.opt-selected {
  border-color: #0066ff;
  background: #0066ff;
}

.opt-radio-dot {
  width: 7px;
  height: 7px;
  background: #fff;
  border-radius: 50%;
}

.opt-checkbox {
  border-radius: 4px;
}

.opt-checkbox.opt-selected {
  border-color: #7c3aed;
  background: #7c3aed;
  color: #fff;
}

.opt-label {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 700;
  width: 18px;
  text-align: center;
  opacity: 0.65;
}

.opt-input {
  flex: 1;
  padding: 6px 0;
  background: transparent;
  border: none;
  font-size: 13px;
  font-family: inherit;
  color: #1a3a5a;
  outline: none;
}

.opt-input::placeholder {
  color: #c0d4e8;
}

.opt-remove {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  border-radius: 5px;
  color: #c0d4e8;
  cursor: pointer;
  transition: all 0.2s ease;
}

.opt-remove:hover {
  background: rgba(255, 77, 79, 0.08);
  color: #ff4d4f;
}

.btn-add-opt {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 3px 10px;
  background: rgba(0, 102, 255, 0.05);
  border: 1px dashed rgba(0, 102, 255, 0.18);
  border-radius: 7px;
  color: #0066ff;
  font-size: 12px;
  font-family: inherit;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-add-opt:hover {
  background: rgba(0, 102, 255, 0.1);
  border-color: rgba(0, 102, 255, 0.3);
}

.opt-hint {
  font-size: 11px;
  color: #b0c8e0;
  margin: 6px 0 0;
}

/* 填空 */
.fill-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
}

.fill-prefix {
  font-size: 13px;
  font-weight: 600;
  color: #3a5a7a;
  white-space: nowrap;
}

.ef-hint {
  font-size: 11px;
  color: #b0c8e0;
  margin: 6px 0 0;
}

/* 分隔线 */
.ef-divider {
  height: 1px;
  background: rgba(0, 102, 255, 0.08);
  margin: 16px 0 18px;
}

/* 分数输入 */
.score-block {
  padding: 12px 16px;
  background: linear-gradient(135deg, rgba(0, 102, 255, 0.03), rgba(0, 212, 255, 0.02));
  border: 1px solid rgba(0, 102, 255, 0.08);
  border-radius: 12px;
}

.score-input-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ef-score-input {
  width: 80px;
  padding: 7px 12px;
  font-size: 16px;
  font-weight: 700;
  font-family: inherit;
  color: #0066ff;
  background: #fff;
  border: 1.5px solid rgba(0, 102, 255, 0.15);
  border-radius: 10px;
  outline: none;
  text-align: center;
  transition: all 0.2s ease;
  -moz-appearance: textfield;
}

.ef-score-input::-webkit-outer-spin-button,
.ef-score-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.ef-score-input:focus {
  border-color: #0066ff;
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.1);
}

.score-unit {
  font-size: 13px;
  color: #7a9aba;
  font-weight: 500;
}

.score-change-hint {
  font-size: 11px;
  color: #d97706;
  background: rgba(217, 119, 6, 0.08);
  padding: 2px 8px;
  border-radius: 20px;
  font-weight: 500;
}

/* 编辑面板底部操作 */
.ef-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
  padding-top: 14px;
  border-top: 1px solid rgba(0, 102, 255, 0.06);
}

.ef-btn-revert {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 7px 16px;
  background: rgba(0, 102, 255, 0.04);
  border: 1px solid rgba(0, 102, 255, 0.1);
  border-radius: 8px;
  color: #5a7a9a;
  font-size: 12px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
}

.ef-btn-revert:hover:not(:disabled) {
  background: rgba(217, 119, 6, 0.08);
  border-color: rgba(217, 119, 6, 0.2);
  color: #d97706;
}

.ef-btn-revert:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.ef-save-hint {
  flex: 1;
  text-align: right;
  font-size: 11px;
  color: #b0c8e0;
  margin: 0;
}

/* 空状态 */
.ef-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 300px;
  text-align: center;
}

.ef-empty-graphic {
  color: #b0c8e0;
  margin-bottom: 16px;
}

.ef-empty-text {
  font-size: 13px;
  color: #7a9aba;
  margin: 0;
}

/* ===== 底部操作栏 ===== */
.wizard-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 40px;
  border-top: 1px solid rgba(0, 102, 255, 0.06);
  background: rgba(255, 255, 255, 0.4);
}

.footer-left {
  flex: 1;
  min-width: 0;
}

.step-breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #7a9aba;
}

.crumb-icon {
  flex-shrink: 0;
}

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

.footer-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-prev {
  background: rgba(0, 102, 255, 0.06);
  color: #5a7a9a;
}

.btn-prev:hover {
  background: rgba(0, 102, 255, 0.1);
}

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

/* ==========================================================
   发布弹窗
   ========================================================== */
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
  width: 540px;
  max-width: 92vw;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(24px);
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow:
    0 30px 80px rgba(0, 102, 255, 0.18),
    0 12px 32px rgba(0, 102, 255, 0.08);
  overflow: hidden;
}

/* 弹窗头部 */
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

.publish-dialog-desc {
  font-size: 13px;
  color: #7a9aba;
  margin: 0;
}

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

.dialog-close-btn:hover {
  background: rgba(0, 102, 255, 0.12);
  color: #3a5a7a;
}

/* 弹窗体 */
.publish-dialog-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px 28px;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.publish-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.publish-label {
  font-size: 14px;
  font-weight: 600;
  color: #3a5a7a;
}

.field-hint {
  font-weight: 400;
  color: #b0c8e0;
  font-size: 12px;
}

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

.course-option:hover {
  border-color: rgba(0, 102, 255, 0.2);
  background: rgba(0, 102, 255, 0.03);
}

.course-selected {
  border-color: #0066ff !important;
  background: rgba(0, 102, 255, 0.04) !important;
}

.course-option-radio {
  flex-shrink: 0;
}

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

.dot-checked {
  border-color: #0066ff;
  background: #0066ff;
  color: #fff;
}

.course-option-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.course-option-name {
  font-size: 14px;
  font-weight: 500;
  color: #1a3a5a;
}

.course-option-code {
  font-size: 12px;
  color: #b0c8e0;
}

.course-empty {
  text-align: center;
  padding: 24px;
  color: #b0c8e0;
  font-size: 13px;
}

/* 班级选择 */
.class-placeholder {
  padding: 24px;
  text-align: center;
  color: #b0c8e0;
  font-size: 13px;
  background: rgba(0, 102, 255, 0.03);
  border-radius: 12px;
}

.class-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

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

.class-chip:hover {
  border-color: rgba(0, 102, 255, 0.2);
  background: rgba(255, 255, 255, 0.85);
}

.class-chip-selected {
  border-color: #0066ff !important;
  background: rgba(0, 102, 255, 0.05) !important;
}

.class-chip-check {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  border: 2px solid rgba(0, 102, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s ease;
  color: #fff;
}

.class-chip-selected .class-chip-check {
  background: #0066ff;
  border-color: #0066ff;
}

.class-chip-name {
  font-size: 13px;
  font-weight: 500;
  color: #1a3a5a;
}

.class-chip-count {
  font-size: 12px;
  color: #b0c8e0;
}

/* 截止日期 */
.deadline-input-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: rgba(0, 102, 255, 0.03);
  border: 1.5px solid rgba(0, 102, 255, 0.12);
  border-radius: 12px;
  transition: all 0.3s ease;
}

.deadline-input-wrap:focus-within {
  border-color: #0066ff;
  box-shadow: 0 0 0 4px rgba(0, 102, 255, 0.08);
  background: #fff;
}

.deadline-icon {
  color: #b0c8e0;
  flex-shrink: 0;
}

.deadline-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  font-family: inherit;
  color: #1a3a5a;
  background: transparent;
}

.deadline-input::-webkit-calendar-picker-indicator {
  opacity: 0.4;
  cursor: pointer;
}

.deadline-input::-webkit-calendar-picker-indicator:hover {
  opacity: 0.8;
}

/* 弹窗底部 */
.publish-dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 28px;
  border-top: 1px solid rgba(0, 102, 255, 0.06);
  background: rgba(255, 255, 255, 0.5);
}

.footer-summary-text {
  font-size: 13px;
  color: #3a5a7a;
}

.footer-summary-text strong {
  color: #0066ff;
}

.footer-summary-muted {
  color: #b0c8e0;
}

.footer-actions {
  display: flex;
  gap: 10px;
}

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

.dialog-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.dialog-btn-cancel {
  background: rgba(0, 102, 255, 0.06);
  color: #5a7a9a;
}

.dialog-btn-cancel:hover {
  background: rgba(0, 102, 255, 0.1);
}

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
.dialog-leave-active {
  transition: all 0.3s ease;
}

.dialog-enter-from,
.dialog-leave-to {
  opacity: 0;
}

.dialog-enter-from .publish-dialog,
.dialog-leave-to .publish-dialog {
  transform: translateY(-20px) scale(0.96);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .page-header {
    flex-wrap: wrap;
  }

  .step-indicator {
    padding: 24px 20px 0;
  }

  .step-label-text {
    display: none;
  }

  .step-connector {
    margin: 0 8px;
    min-width: 16px;
  }

  .step-panel {
    padding: 24px 20px;
  }

  .wizard-footer {
    flex-direction: column;
    gap: 12px;
    padding: 16px 20px;
  }

  .footer-left {
    width: 100%;
  }

  .footer-right {
    width: 100%;
    justify-content: stretch;
  }

  .footer-btn {
    flex: 1;
    justify-content: center;
  }

  .bank-grid {
    grid-template-columns: 1fr;
  }

  .score-summary-bar {
    flex-wrap: wrap;
  }

  .summary-actions {
    margin-left: 0;
    width: 100%;
  }

  .ghost-btn {
    justify-content: center;
    width: 100%;
  }

  .split-editor {
    flex-direction: column;
    min-height: auto;
  }

  .editor-sidebar {
    width: 100%;
    min-width: 0;
    max-height: 220px;
    border-right: none;
    border-bottom: 1px solid rgba(0, 102, 255, 0.08);
  }

  .editor-main-panel {
    padding: 16px;
  }

  .publish-dialog {
    max-height: 90vh;
    border-radius: 20px;
  }

  .publish-dialog-header,
  .publish-dialog-body,
  .publish-dialog-footer {
    padding-left: 20px;
    padding-right: 20px;
  }

  .publish-dialog-footer {
    flex-direction: column;
    gap: 12px;
  }

  .footer-actions {
    width: 100%;
  }

  .dialog-btn {
    flex: 1;
    text-align: center;
  }
}

@media (max-width: 480px) {
  .page-title {
    font-size: 20px;
  }

  .step-circle {
    width: 30px;
    height: 30px;
    font-size: 13px;
  }

  .wizard-card {
    border-radius: 20px;
  }

  .question-row {
    flex-direction: column;
    gap: 10px;
  }
}
/* ===== 课程选择（步骤 0） ===== */
.course-loading-hint {
  font-size: 13px;
  color: #7a9aba;
  padding: 8px 0;
}

.course-chip-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.course-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(0, 102, 255, 0.04);
  border: 1px solid rgba(0, 102, 255, 0.12);
  border-radius: 10px;
  font-size: 14px;
  font-family: inherit;
  color: #4a6a8a;
  cursor: pointer;
  transition: all 0.2s ease;
}

.course-chip:hover {
  background: rgba(0, 102, 255, 0.08);
  border-color: rgba(0, 102, 255, 0.25);
}

.course-chip-active {
  background: rgba(0, 102, 255, 0.08);
  border-color: #0066ff;
  color: #0066ff;
  font-weight: 500;
}

.course-chip-code {
  font-size: 12px;
  opacity: 0.65;
}

.chip-check {
  flex-shrink: 0;
}
</style>
