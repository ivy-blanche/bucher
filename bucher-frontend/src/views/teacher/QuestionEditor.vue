<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getQuestionList, getQuestionDetail, batchSaveQuestions } from '@/api/question'
import type { BatchSaveQuestion } from '@/api/question'

const route = useRoute()
const router = useRouter()
const bankId = route.params.bankId as string
const bankName = (route.query.name as string) || '未命名题库'

/* ==================== Types ==================== */
type QuestionType = 1 | 2 | 3 | 4 | 5

interface QuestionOption {
  id?: string | null
  label: string
  content: string
  isCorrect: boolean
  sortOrder: number
}

interface ProgrammingConfig {
  templateCode: string
  judge0LanguageId: number
  timeLimit: number
  memoryLimit: number
}

interface TestCase {
  input: string
  expectedOutput: string
  isSample: boolean
  sortOrder: number
}

interface Question {
  id: string | null
  type: QuestionType
  content: string
  options: QuestionOption[]
  answer: string | string[]
  analysis: string
  programmingConfig?: ProgrammingConfig
  testCases?: TestCase[]
  deleted?: boolean
  /** 标记是否已从后端加载完整内容 */
  _fullyLoaded?: boolean
}

interface EditForm {
  id: string | null
  type: QuestionType
  content: string
  options: QuestionOption[]
  answer: string | string[]
  analysis: string
  programmingConfig?: ProgrammingConfig
  testCases?: TestCase[]
}

/* ==================== Type Definitions ==================== */
const typeOrder: QuestionType[] = [1, 2, 3, 4, 5]

const typeConfig: Record<QuestionType, { numeral: string; label: string; short: string; icon: string; color: string }> = {
  1: { numeral: '一', label: '单选题',   short: '单选', icon: '○', color: '#0066ff' },
  2: { numeral: '二', label: '多选题',   short: '多选', icon: '□', color: '#7c3aed' },
  3: { numeral: '三', label: '填空题',   short: '填空', icon: '＿', color: '#0891b2' },
  4: { numeral: '四', label: '简答题',   short: '简答', icon: '〰', color: '#d97706' },
  5: { numeral: '五', label: '编程题',   short: '编程', icon: '</>', color: '#16a34a' },
}

/* ==================== Languages (Judge0) ==================== */
interface LanguageOption {
  id: number
  name: string
  template: string
}

const languages: LanguageOption[] = [
  { id: 62, name: 'Java', template: 'import java.util.Scanner;\npublic class Main {\n    public static void main(String[] args) {\n        // 在此编写代码\n    }\n}' },
  { id: 71, name: 'Python', template: '# 在此编写代码\n\ndef main():\n    pass\n\nif __name__ == "__main__":\n    main()' },
  { id: 54, name: 'C++', template: '#include <iostream>\nusing namespace std;\n\nint main() {\n    // 在此编写代码\n    return 0;\n}' },
  { id: 50, name: 'C', template: '#include <stdio.h>\n\nint main() {\n    // 在此编写代码\n    return 0;\n}' },
  { id: 51, name: 'C#', template: 'using System;\n\nclass Program {\n    static void Main() {\n        // 在此编写代码\n    }\n}' },
  { id: 63, name: 'JavaScript', template: '// 在此编写代码\nfunction main() {\n    \n}\n\nmain();' },
  { id: 74, name: 'TypeScript', template: '// 在此编写代码\nfunction main(): void {\n    \n}\n\nmain();' },
  { id: 60, name: 'Go', template: 'package main\n\nimport "fmt"\n\nfunc main() {\n    // 在此编写代码\n    fmt.Scan()\n}' },
  { id: 73, name: 'Rust', template: 'fn main() {\n    // 在此编写代码\n}' },
]

const languageMap = new Map(languages.map(l => [l.id, l]))
const defaultLanguage = languages[0]

/* ==================== State ==================== */
const questions = ref<Question[]>([])
const selectedId = ref<number | null>(null)
const loading = ref(false)
const submitting = ref(false)
const searchQuery = ref('')

/** 已修改但未保存的题目 ID 集合 */
const dirtyIds = ref<Set<number>>(new Set())
/** 每个题目的最新快照（已保存的版本），用于脏比较和撤销 */
const snapshotMap = ref<Map<number, Question>>(new Map())
/** 已从后端加载完整内容的题目 ID */
const fullyLoadedIds = ref<Set<number>>(new Set())

const defaultForms: Record<QuestionType, () => EditForm> = {
  1: () => ({
    id: null, type: 1, content: '',
    options: [{ label: 'A', content: '', isCorrect: false, sortOrder: 0 }, { label: 'B', content: '', isCorrect: false, sortOrder: 1 }],
    answer: '',
    analysis: '',
  }),
  2: () => ({
    id: null, type: 2, content: '',
    options: [{ label: 'A', content: '', isCorrect: false, sortOrder: 0 }, { label: 'B', content: '', isCorrect: false, sortOrder: 1 }],
    answer: [] as string[],
    analysis: '',
  }),
  3: () => ({
    id: null, type: 3, content: '',
    options: [],
    answer: '',
    analysis: '',
  }),
  4: () => ({
    id: null, type: 4, content: '',
    options: [],
    answer: '',
    analysis: '',
  }),
  5: () => ({
    id: null, type: 5, content: '',
    options: [],
    answer: '',
    analysis: '',
    programmingConfig: {
      templateCode: defaultLanguage.template,
      judge0LanguageId: defaultLanguage.id,
      timeLimit: 3000,
      memoryLimit: 256000,
    },
    testCases: [
      { input: '', expectedOutput: '', isSample: true, sortOrder: 1 },
    ],
  }),
}

const editForm = ref<EditForm>(defaultForms[1]())

/** 添加按钮闪烁效果 */
const flashAddSection = ref(true)

/* ==================== Computed ==================== */
const activeQuestions = computed(() =>
  questions.value.filter((q) => !q.deleted)
)

const filteredQuestions = computed(() => {
  const list = activeQuestions.value
  if (!searchQuery.value.trim()) return list
  const q = searchQuery.value.trim().toLowerCase()
  return list.filter((item) => item.content.toLowerCase().includes(q))
})

interface QuestionGroup {
  type: QuestionType
  numeral: string
  label: string
  icon: string
  color: string
  questions: Question[]
}

const groupedQuestions = computed(() => {
  const groups: QuestionGroup[] = []
  for (const type of typeOrder) {
    const list = filteredQuestions.value.filter((q) => q.type === type)
    if (list.length > 0) {
      groups.push({
        type,
        numeral: typeConfig[type].numeral,
        label: typeConfig[type].label,
        icon: typeConfig[type].icon,
        color: typeConfig[type].color,
        questions: list,
      })
    }
  }
  return groups
})

const selectedQuestion = computed(() => {
  if (selectedId.value === null) return null
  return questions.value.find((q) => q.id === selectedId.value) || null
})

const dirtyCount = computed(() => dirtyIds.value.size)
const currentIsDirty = computed(() => selectedId.value !== null && dirtyIds.value.has(selectedId.value))

/* ==================== API Calls ==================== */
async function loadQuestions() {
  loading.value = true
  try {
    const list = await getQuestionList(bankId)
    questions.value = list.map((item) => ({
      id: item.id,
      type: item.type as QuestionType,
      content: item.contentPreview,
      options: [],
      answer: '',
      analysis: '',
      _fullyLoaded: false,
    }))
  } catch {
    ElMessage.error('加载题目列表失败')
    questions.value = []
  } finally {
    loading.value = false
  }
}

async function loadQuestionDetail(id: string) {
  if (fullyLoadedIds.value.has(id)) return
  try {
    const detail = await getQuestionDetail(id)
    const idx = questions.value.findIndex((q) => q.id === id)
    if (idx === -1) return
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
      programmingConfig: detail.programmingConfig
        ? { ...detail.programmingConfig }
        : undefined,
      testCases: detail.testCases
        ? detail.testCases.map((tc) => ({ ...tc }))
        : undefined,
      _fullyLoaded: true,
    }
    fullyLoadedIds.value.add(id)
  } catch {
    ElMessage.error('加载题目详情失败')
  }
}

/* ==================== Dirty Detection ==================== */
function flushDirtyState() {
  const id = selectedId.value
  if (id === null) return

  const snapshot = snapshotMap.value.get(id)
  if (!snapshot) {
    dirtyIds.value = new Set(dirtyIds.value).add(id)
    return
  }

  const f = editForm.value
  const changed =
    f.content !== snapshot.content ||
    f.analysis !== snapshot.analysis ||
    JSON.stringify(f.options) !== JSON.stringify(snapshot.options) ||
    JSON.stringify(f.answer) !== JSON.stringify(snapshot.answer) ||
    JSON.stringify(f.programmingConfig) !== JSON.stringify(snapshot.programmingConfig) ||
    JSON.stringify(f.testCases) !== JSON.stringify(snapshot.testCases)

  const next = new Set(dirtyIds.value)
  if (changed) {
    next.add(id)
  } else {
    next.delete(id)
  }
  dirtyIds.value = next
}

/* ==================== Methods ==================== */
async function goBack() {
  if (dirtyCount.value > 0) {
    try {
      await ElMessageBox.confirm(
        `还有 ${dirtyCount.value} 道题目未保存，返回将放弃这些修改，是否继续？`,
        '未保存的修改',
        {
          confirmButtonText: '放弃修改',
          cancelButtonText: '取消',
          type: 'warning',
          distinguishCancelAndClose: true,
        }
      )
    } catch {
      return
    }
  }
  router.push({ name: 'teacherQuestionManage', params: { bankId }, query: { name: bankName } })
}

function previewText(content: string): string {
  // 去除 HTML 标签后取前 42 字符
  const plain = content.replace(/<[^>]*>/g, '')
  return plain.length > 42 ? plain.slice(0, 42) + '…' : plain
}

async function selectQuestion(id: string) {
  flushDirtyState()

  // 把当前表单内容写回 questions 数组
  if (selectedId.value !== null && editForm.value.id !== null) {
    const idx = questions.value.findIndex((q) => q.id === editForm.value.id)
    if (idx !== -1) {
      questions.value[idx] = {
        id: editForm.value.id,
        type: editForm.value.type,
        content: editForm.value.content,
        options: JSON.parse(JSON.stringify(editForm.value.options)),
        answer: Array.isArray(editForm.value.answer)
          ? [...editForm.value.answer]
          : editForm.value.answer,
        analysis: editForm.value.analysis,
        programmingConfig: editForm.value.programmingConfig
          ? JSON.parse(JSON.stringify(editForm.value.programmingConfig))
          : undefined,
        testCases: editForm.value.testCases
          ? JSON.parse(JSON.stringify(editForm.value.testCases))
          : undefined,
        _fullyLoaded: questions.value[idx]._fullyLoaded,
      }
    }
  }

  selectedId.value = id
  const q = questions.value.find((item) => item.id === id)
  if (!q) return

  // 懒加载完整数据
  if (q.id && !q._fullyLoaded) {
    await loadQuestionDetail(q.id)
    // 重新获取更新后的数据
    const updated = questions.value.find((item) => item.id === id)
    if (!updated) return

    if (!snapshotMap.value.has(id)) {
      snapshotMap.value.set(id, JSON.parse(JSON.stringify(updated)))
    }

    editForm.value = {
      id: updated.id,
      type: updated.type,
      content: updated.content,
      options: JSON.parse(JSON.stringify(updated.options)),
      answer: Array.isArray(updated.answer) ? [...updated.answer] : updated.answer,
      analysis: updated.analysis,
      programmingConfig: updated.programmingConfig
        ? JSON.parse(JSON.stringify(updated.programmingConfig))
        : undefined,
      testCases: updated.testCases
        ? JSON.parse(JSON.stringify(updated.testCases))
        : undefined,
    }
    return
  }

  if (!snapshotMap.value.has(id)) {
    snapshotMap.value.set(id, JSON.parse(JSON.stringify(q)))
  }

  editForm.value = {
    id: q.id,
    type: q.type,
    content: q.content,
    options: JSON.parse(JSON.stringify(q.options)),
    answer: Array.isArray(q.answer) ? [...q.answer] : q.answer,
    analysis: q.analysis,
    programmingConfig: q.programmingConfig
      ? JSON.parse(JSON.stringify(q.programmingConfig))
      : undefined,
    testCases: q.testCases
      ? JSON.parse(JSON.stringify(q.testCases))
      : undefined,
  }
}

function createNewQuestion(rawType: QuestionType) {
  flushDirtyState()

  // 移除已有的空题目（临时 id，未填写内容），避免误触堆积
  const emptyTempIds = questions.value
    .filter((q) => q.id! < 0 && !q.content.trim())
    .map((q) => q.id!)
  if (emptyTempIds.length > 0) {
    questions.value = questions.value.filter((q) => !emptyTempIds.includes(q.id!))
    const next = new Set(dirtyIds.value)
    for (const id of emptyTempIds) next.delete(id)
    dirtyIds.value = next
    if (selectedId.value && emptyTempIds.includes(selectedId.value)) {
      selectedId.value = null
    }
  }

  // v-for 遍历对象时 key 为字符串，确保转为数字
  const type = Number(rawType) as QuestionType
  const tempId = -(Date.now())
  const newQuestion: Question = {
    id: tempId,
    type,
    content: '',
    options:
      type === 1 || type === 2
        ? [
            { label: 'A', content: '', isCorrect: false, sortOrder: 0 },
            { label: 'B', content: '', isCorrect: false, sortOrder: 1 },
          ]
        : [],
    answer: type === 2 ? [] : '',
    analysis: '',
    programmingConfig: type === 5
      ? {
          templateCode: defaultLanguage.template,
          judge0LanguageId: defaultLanguage.id,
          timeLimit: 3000,
          memoryLimit: 256000,
        }
      : undefined,
    testCases: type === 5
      ? [{ input: '', expectedOutput: '', isSample: true, sortOrder: 1 }]
      : undefined,
    _fullyLoaded: true,
  }
  questions.value.push(newQuestion)
  selectedId.value = tempId

  editForm.value = {
    id: tempId,
    type,
    content: '',
    options: JSON.parse(JSON.stringify(newQuestion.options)),
    answer: type === 2 ? ([] as string[]) : '',
    analysis: '',
    programmingConfig: type === 5
      ? {
          templateCode: defaultLanguage.template,
          judge0LanguageId: defaultLanguage.id,
          timeLimit: 3000,
          memoryLimit: 256000,
        }
      : undefined,
    testCases: type === 5
      ? [{ input: '', expectedOutput: '', isSample: true, sortOrder: 1 }]
      : undefined,
  }

  const next = new Set(dirtyIds.value)
  next.add(tempId)
  dirtyIds.value = next

  setTimeout(() => {
    const el = document.getElementById(`q-item-${tempId}`)
    el?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }, 100)
}

/** 构建批量保存载荷 */
function buildBatchPayload(): BatchSaveQuestion[] {
  const ids = [...dirtyIds.value]
  return ids.map((id) => {
    const q = questions.value.find((item) => item.id === id)
    if (!q) {
      // 已从列表移除（删除操作已将 deleted 标记），在 questions 中查找 deleted 的
      const deleted = questions.value.find((item) => item.id === id && item.deleted)
      return {
        id: id != null && id !== '' ? id : null,
        type: 1 as QuestionType,
        content: '',
        answer: '',
        analysis: '',
        options: [],
        deleted: true,
      }
    }

    const options = q.options.map((opt) => ({
      label: opt.label,
      content: opt.content,
      isCorrect: opt.isCorrect,
      sortOrder: opt.sortOrder,
    }))

    let answer = ''
    if (q.type === 1 || q.type === 2) {
      // 选择题：后端根据 options.isCorrect 自动生成 answer
      answer = ''
    } else {
      answer = typeof q.answer === 'string' ? q.answer : q.answer.join(',')
    }

    return {
      id: q.id != null && q.id > 0 ? q.id : null,
      type: q.type,
      content: q.content,
      answer,
      analysis: q.analysis,
      options,
      programmingConfig: q.type === 5 && q.programmingConfig
        ? { ...q.programmingConfig }
        : undefined,
      testCases: q.type === 5 && q.testCases
        ? q.testCases.map((tc) => ({ ...tc }))
        : undefined,
      deleted: q.deleted || false,
    }
  })
}

/** 统一保存所有脏数据 */
async function saveAllDirty() {
  // 把当前表单写回 questions 数组，确保 buildBatchPayload 读取到最新数据
  if (selectedId.value !== null && editForm.value.id !== null) {
    const idx = questions.value.findIndex((q) => q.id === editForm.value.id)
    if (idx !== -1) {
      questions.value[idx] = {
        id: editForm.value.id,
        type: editForm.value.type,
        content: editForm.value.content,
        options: JSON.parse(JSON.stringify(editForm.value.options)),
        answer: Array.isArray(editForm.value.answer)
          ? [...editForm.value.answer]
          : editForm.value.answer,
        analysis: editForm.value.analysis,
        programmingConfig: editForm.value.programmingConfig
          ? JSON.parse(JSON.stringify(editForm.value.programmingConfig))
          : undefined,
        testCases: editForm.value.testCases
          ? JSON.parse(JSON.stringify(editForm.value.testCases))
          : undefined,
        _fullyLoaded: questions.value[idx]._fullyLoaded,
      }
    }
  }

  flushDirtyState()

  if (dirtyCount.value === 0) {
    ElMessage.info('没有需要保存的修改')
    return
  }

  const ids = [...dirtyIds.value]
  if (ids.length === 0) {
    ElMessage.info('没有需要保存的修改')
    return
  }

  submitting.value = true
  try {
    const payload = buildBatchPayload()
    await batchSaveQuestions({ groupId: bankId, questions: payload })

    // 保存成功：移除非新增题目的快照并移除 deleted 的题目
    const nextSnapshots = new Map(snapshotMap.value)
    for (const id of ids) {
      const q = questions.value.find((item) => item.id === id)
      if (!q) continue
      if (q.deleted) {
        // 彻底移除已删除的题目
        questions.value = questions.value.filter((item) => item.id !== id)
        nextSnapshots.delete(id)
        if (selectedId.value === id) {
          selectedId.value = null
        }
      } else if (q.id != null && q.id > 0) {
        // 已存在的题目：更新快照
        nextSnapshots.set(id, JSON.parse(JSON.stringify(q)))
      }
    }

    // 处理新增题目（临时 ID → 真实 ID）：需要重新加载列表获取真实 ID
    const hadNewQuestions = ids.some((id) => id < 0)
    if (hadNewQuestions) {
      await loadQuestions()
      // 清空所有快照和脏标记，因为 ID 已变化
      snapshotMap.value = new Map()
      fullyLoadedIds.value = new Set()
      selectedId.value = null
    } else {
      snapshotMap.value = nextSnapshots
    }

    dirtyIds.value = new Set()
    ElMessage.success(`已保存 ${ids.length} 道题目`)
  } catch {
    ElMessage.error('保存失败，请重试')
  } finally {
    submitting.value = false
  }
}

function deleteQuestion(id: string) {
  ElMessageBox.confirm('确认删除该题目吗？删除后不可恢复。', '删除确认', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      const q = questions.value.find((item) => item.id === id)
      if (q) {
        // 标记删除，batch-save 时提交
        q.deleted = true
      }

      // 如果是从后端加载的题目，保留在列表但标记删除；新建的题目直接移除
      if (id < 0) {
        questions.value = questions.value.filter((item) => item.id !== id)
      }

      // 清理脏标记和快照
      const nextDirty = new Set(dirtyIds.value)
      nextDirty.delete(id)
      dirtyIds.value = nextDirty

      const nextSnapshots = new Map(snapshotMap.value)
      nextSnapshots.delete(id)
      snapshotMap.value = nextSnapshots

      if (selectedId.value === id) {
        selectedId.value = null
      }
      ElMessage.success('题目已删除')
    })
    .catch(() => {})
}

/** 撤销当前题目的修改，回退到快照版本 */
function revertCurrent() {
  const id = selectedId.value
  if (id === null) return

  const snapshot = snapshotMap.value.get(id)
  if (!snapshot) {
    deleteQuestion(id)
    return
  }

  const idx = questions.value.findIndex((q) => q.id === id)
  if (idx !== -1) {
    questions.value[idx] = JSON.parse(JSON.stringify(snapshot))
  }

  editForm.value = {
    id: snapshot.id,
    type: snapshot.type,
    content: snapshot.content,
    options: JSON.parse(JSON.stringify(snapshot.options)),
    answer: Array.isArray(snapshot.answer) ? [...snapshot.answer] : snapshot.answer,
    analysis: snapshot.analysis,
    programmingConfig: snapshot.programmingConfig
      ? JSON.parse(JSON.stringify(snapshot.programmingConfig))
      : undefined,
    testCases: snapshot.testCases
      ? JSON.parse(JSON.stringify(snapshot.testCases))
      : undefined,
  }

  const next = new Set(dirtyIds.value)
  next.delete(id)
  dirtyIds.value = next

  ElMessage.success('已撤销修改')
}

/* ==================== Option Management ==================== */
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

/* ==================== Programming Question ==================== */
function onProgrammingLanguageChange(langId: number) {
  const lang = languageMap.get(langId)
  if (!lang || !editForm.value.programmingConfig) return
  if (editForm.value.programmingConfig.judge0LanguageId === langId) return
  editForm.value.programmingConfig.judge0LanguageId = langId
  editForm.value.programmingConfig.templateCode = lang.template
  flushDirtyState()
}

function addTestCase() {
  if (!editForm.value.testCases) {
    editForm.value.testCases = []
  }
  editForm.value.testCases.push({
    input: '',
    expectedOutput: '',
    isSample: false,
    sortOrder: editForm.value.testCases.length + 1,
  })
  flushDirtyState()
}

function removeTestCase(index: number) {
  if (editForm.value.testCases && editForm.value.testCases.length > 1) {
    editForm.value.testCases.splice(index, 1)
    editForm.value.testCases.forEach((tc, i) => { tc.sortOrder = i + 1 })
    flushDirtyState()
  }
}

function moveTestCase(index: number, direction: -1 | 1) {
  if (!editForm.value.testCases) return
  const target = index + direction
  if (target < 0 || target >= editForm.value.testCases.length) return
  const arr = editForm.value.testCases
  ;[arr[index], arr[target]] = [arr[target], arr[index]]
  arr.forEach((tc, i) => { tc.sortOrder = i + 1 })
  flushDirtyState()
}

/* ==================== Init ==================== */
onMounted(async () => {
  await loadQuestions()

  const selectedParam = route.query.selected as string | undefined
  if (selectedParam) {
    if (questions.value.some((q) => q.id === selectedParam)) {
      await selectQuestion(selectedParam)
    }
  }

  setTimeout(() => {
    flashAddSection.value = false
  }, 2400)
})
</script>

<template>
  <div class="editor-page">
    <!-- ===== Top Bar ===== -->
    <header class="editor-topbar">
      <div class="topbar-left">
        <button class="topbar-back" @click="goBack">
          <svg viewBox="0 0 20 20" width="18" height="18">
            <polyline points="12,4 6,10 12,16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <span>返回</span>
        </button>
        <div class="topbar-divider" />
        <div class="topbar-title">
          <svg class="title-icon" viewBox="0 0 20 20" width="20" height="20">
            <path d="M4 4h12v2H4zM4 8h12v2H4zM4 12h8v2H4z" fill="currentColor" opacity="0.3" />
            <path d="M14 12l3 3-3 3" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <h1>{{ bankName }}</h1>
          <span class="topbar-count">{{ activeQuestions.length }} 题</span>
        </div>
      </div>
      <div class="topbar-right">
        <button
          class="btn-save-all"
          :class="{ 'has-dirty': dirtyCount > 0 }"
          :disabled="submitting"
          @click="saveAllDirty"
        >
          <svg viewBox="0 0 20 20" width="16" height="16">
            <path d="M17 6l-9 9-4-4" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <span>保存</span>
          <span v-if="dirtyCount > 0" class="save-badge">{{ dirtyCount }}</span>
        </button>
      </div>
    </header>

    <!-- ===== Editor Body ===== -->
    <div class="editor-body">
      <!-- ===== Left: Question List ===== -->
      <aside class="question-sidebar">
        <div class="sidebar-header">
          <svg viewBox="0 0 20 20" width="16" height="16" class="sidebar-header-icon">
            <circle cx="10" cy="10" r="8" fill="none" stroke="currentColor" stroke-width="1.5" />
            <circle cx="10" cy="7" r="1.5" fill="currentColor" />
            <path d="M10 10v5" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
          </svg>
          <span>题目列表</span>
          <span v-if="dirtyCount > 0" class="sidebar-dirty-hint">{{ dirtyCount }} 项未保存</span>
        </div>

        <!-- Search -->
        <div class="sidebar-search">
          <svg class="search-icon" viewBox="0 0 20 20" width="15" height="15">
            <circle cx="8.5" cy="8.5" r="5.5" fill="none" stroke="currentColor" stroke-width="1.5" />
            <line x1="12.5" y1="12.5" x2="17" y2="17" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
          </svg>
          <input
            v-model="searchQuery"
            class="search-input"
            type="text"
            placeholder="搜索题目..."
          />
          <button v-if="searchQuery" class="search-clear" @click="searchQuery = ''">
            <svg viewBox="0 0 20 20" width="14" height="14">
              <line x1="5" y1="5" x2="15" y2="15" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
              <line x1="15" y1="5" x2="5" y2="15" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
            </svg>
          </button>
        </div>

        <div class="sidebar-scroll">
          <!-- Loading -->
          <div v-if="loading" class="sidebar-loading">
            <div class="loading-spinner" />
            <p class="loading-text">加载中...</p>
          </div>

          <!-- Question Groups -->
          <template v-else>
            <div
              v-for="group in groupedQuestions"
              :key="group.type"
              class="question-group"
            >
              <div class="group-header" :style="{ '--group-color': group.color }">
                <span class="group-numeral">{{ group.numeral }}</span>
                <span class="group-label">{{ group.label }}</span>
                <span class="group-badge">{{ group.questions.length }}</span>
              </div>
              <div class="group-items">
                <button
                  v-for="q in group.questions"
                  :key="q.id"
                  :id="`q-item-${q.id}`"
                  class="question-item"
                  :class="{
                    active: selectedId === q.id,
                    dirty: dirtyIds.has(q.id!),
                  }"
                  @click="selectQuestion(q.id!)"
                >
                  <span class="item-icon" :style="{ color: group.color }">{{ group.icon }}</span>
                  <span class="item-stem">{{ previewText(q.content) }}</span>
                  <span v-if="dirtyIds.has(q.id!)" class="item-dirty-dot" title="有未保存的修改" />
                  <button
                    class="item-delete"
                    @click.stop="deleteQuestion(q.id!)"
                    title="删除题目"
                  >
                    <svg viewBox="0 0 20 20" width="13" height="13">
                      <path d="M4 5h12M7 5V4a1 1 0 011-1h4a1 1 0 011 1v1M5 5v11a1 1 0 001 1h8a1 1 0 001-1V5" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                  </button>
                </button>
              </div>
            </div>

            <div v-if="groupedQuestions.length === 0" class="sidebar-empty">
              <svg viewBox="0 0 40 40" width="40" height="40" class="sidebar-empty-icon">
                <circle cx="20" cy="20" r="16" fill="none" stroke="currentColor" stroke-width="1.5" opacity="0.3" />
                <line x1="20" y1="12" x2="20" y2="28" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.3" />
                <line x1="12" y1="20" x2="28" y2="20" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.3" />
              </svg>
              <p class="sidebar-empty-text">
                {{ searchQuery ? '没有匹配的题目' : '题库暂无题目，点击上方按钮添加' }}
              </p>
            </div>
          </template>
        </div>
      </aside>

      <!-- ===== Right: Editor Panel ===== -->
      <main class="editor-main">
        <!-- Add Section -->
        <section class="add-section" :class="{ 'flash-attention': flashAddSection }">
          <div class="add-section-header">
            <svg viewBox="0 0 20 20" width="16" height="16" class="add-section-icon">
              <line x1="10" y1="3" x2="10" y2="17" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
              <line x1="3" y1="10" x2="17" y2="10" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
            </svg>
            <span>添加题目</span>
          </div>
          <div class="add-type-list">
            <button
              v-for="(cfg, type) in typeConfig"
              :key="type"
              class="add-type-btn"
              :style="{ '--btn-color': cfg.color }"
              @click="createNewQuestion(type as QuestionType)"
            >
              <span class="add-type-icon">{{ cfg.icon }}</span>
              <span class="add-type-label">{{ cfg.label }}</span>
            </button>
          </div>
        </section>

        <!-- Editor Form -->
        <section class="edit-section">
          <template v-if="selectedId !== null && selectedQuestion">
            <div class="edit-form">
              <!-- Form Header -->
              <div class="edit-form-header">
                <div class="edit-form-type" :style="{ '--type-color': typeConfig[editForm.type].color }">
                  <span class="edit-type-icon">{{ typeConfig[editForm.type].icon }}</span>
                  <span>{{ typeConfig[editForm.type].label }}</span>
                  <span v-if="currentIsDirty" class="edit-dirty-badge">未保存</span>
                </div>
                <div class="edit-form-actions">
                  <button class="edit-action-btn delete-btn" @click="deleteQuestion(selectedId!)">
                    <svg viewBox="0 0 20 20" width="15" height="15">
                      <path d="M4 5h12M7 5V4a1 1 0 011-1h4a1 1 0 011 1v1M5 5v11a1 1 0 001 1h8a1 1 0 001-1V5" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                    <span>删除</span>
                  </button>
                  <span class="edit-id-badge">#{{ editForm.id }}</span>
                </div>
              </div>

              <!-- Stem -->
              <div class="form-block">
                <label class="form-block-label">
                  题干
                  <span class="required-star">*</span>
                </label>
                <textarea
                  v-model="editForm.content"
                  class="form-textarea"
                  :class="{ 'has-content': editForm.content.trim() }"
                  placeholder="请输入题目题干..."
                  rows="3"
                  @blur="flushDirtyState"
                />
              </div>

              <!-- SINGLE / MULTIPLE CHOICE Options -->
              <template v-if="editForm.type === 1 || editForm.type === 2">
                <div class="form-block">
                  <div class="form-block-header">
                    <label class="form-block-label">选项</label>
                    <button
                      v-if="editForm.options.length < 6"
                      class="btn-add-option"
                      @click="addOption"
                    >
                      <svg viewBox="0 0 16 16" width="14" height="14">
                        <line x1="8" y1="2.5" x2="8" y2="13.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                        <line x1="2.5" y1="8" x2="13.5" y2="8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                      </svg>
                      <span>添加选项</span>
                    </button>
                  </div>
                  <div class="options-list">
                    <div
                      v-for="(opt, i) in editForm.options"
                      :key="i"
                      class="option-row"
                    >
                      <div class="option-select-col">
                        <button
                          v-if="editForm.type === 1"
                          class="option-radio"
                          :class="{ selected: editForm.answer === opt.label }"
                          @click="opt.isCorrect = true; editForm.answer = opt.label; flushDirtyState()"
                        >
                          <span v-if="editForm.answer === opt.label" class="option-radio-dot" />
                        </button>
                        <button
                          v-else
                          class="option-checkbox"
                          :class="{ selected: (editForm.answer as string[]).includes(opt.label) }"
                          @click="opt.isCorrect = !opt.isCorrect; toggleMultiAnswer(opt.label); flushDirtyState()"
                        >
                          <svg v-if="(editForm.answer as string[]).includes(opt.label)" viewBox="0 0 14 14" width="12" height="12">
                            <polyline points="3,7 6,10 11,3" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                          </svg>
                        </button>
                      </div>
                      <span class="option-label" :style="{ color: typeConfig[editForm.type].color }">{{ opt.label }}</span>
                      <input
                        v-model="opt.content"
                        class="option-input"
                        :placeholder="`选项 ${opt.label}`"
                        type="text"
                        @blur="flushDirtyState"
                      />
                      <button
                        v-if="editForm.options.length > 2"
                        class="option-remove"
                        @click="removeOption(i)"
                      >
                        <svg viewBox="0 0 16 16" width="13" height="13">
                          <line x1="4" y1="4" x2="12" y2="12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                          <line x1="12" y1="4" x2="4" y2="12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                        </svg>
                      </button>
                    </div>
                  </div>
                </div>
                <div class="form-block hint-block">
                  <span class="hint-text">
                    {{ editForm.type === 1 ? '点击选项前的 ○ 选择正确答案' : '点击选项前的 □ 选择所有正确答案' }}
                  </span>
                </div>
                <div v-if="editForm.type === 1 && editForm.answer" class="form-block answer-display-block">
                  <label class="form-block-label">参考答案</label>
                  <span class="answer-display-text">
                    选项 <strong>{{ editForm.answer }}</strong>
                  </span>
                </div>
              </template>

              <!-- FILL_BLANK -->
              <template v-if="editForm.type === 3">
                <div class="form-block">
                  <label class="form-block-label">
                    正确答案
                    <span class="required-star">*</span>
                  </label>
                  <div class="fill-blank-input-wrap">
                    <span class="fill-blank-prefix">答案：</span>
                    <input
                      v-model="editForm.answer"
                      class="form-input"
                      placeholder="填写空白处的答案"
                      type="text"
                      @blur="flushDirtyState"
                    />
                  </div>
                  <p class="form-hint">在题干中使用 ______（6 个下划线）标记空白位置</p>
                </div>
              </template>

              <!-- SHORT_ANSWER -->
              <template v-if="editForm.type === 4">
                <div class="form-block">
                  <label class="form-block-label">
                    参考答案
                    <span class="required-star">*</span>
                  </label>
                  <textarea
                    v-model="editForm.answer"
                    class="form-textarea"
                    :class="{ 'has-content': (editForm.answer as string).trim() }"
                    placeholder="请输入参考答案..."
                    rows="4"
                    @blur="flushDirtyState"
                  />
                </div>
              </template>

              <!-- PROGRAMMING -->
              <template v-if="editForm.type === 5">
                <!-- Template Code -->
                <div class="form-block">
                  <label class="form-block-label">模板代码</label>
                  <div class="template-code-wrap">
                    <div class="code-header">
                      <select
                        class="lang-select"
                        :value="editForm.programmingConfig?.judge0LanguageId"
                        @change="onProgrammingLanguageChange(Number(($event.target as HTMLSelectElement).value))"
                      >
                        <option
                          v-for="lang in languages"
                          :key="lang.id"
                          :value="lang.id"
                        >
                          {{ lang.name }}
                        </option>
                      </select>
                      <span class="code-lang-id">Judge0 ID: {{ editForm.programmingConfig?.judge0LanguageId }}</span>
                    </div>
                    <textarea
                      v-model="editForm.programmingConfig!.templateCode"
                      class="code-textarea"
                      rows="14"
                      spellcheck="false"
                      wrap="off"
                      @blur="flushDirtyState"
                    />
                  </div>
                </div>

                <!-- Limits -->
                <div class="form-block">
                  <label class="form-block-label">运行限制</label>
                  <div class="limits-row">
                    <div class="limit-item">
                      <label class="limit-label">时间限制 (ms)</label>
                      <input
                        v-model.number="editForm.programmingConfig!.timeLimit"
                        class="form-input"
                        type="number"
                        min="100"
                        step="100"
                        @blur="flushDirtyState"
                      />
                    </div>
                    <div class="limit-item">
                      <label class="limit-label">内存限制 (KB)</label>
                      <input
                        v-model.number="editForm.programmingConfig!.memoryLimit"
                        class="form-input"
                        type="number"
                        min="1024"
                        step="1024"
                        @blur="flushDirtyState"
                      />
                    </div>
                  </div>
                </div>

                <!-- Test Cases -->
                <div class="form-block">
                  <div class="form-block-header">
                    <label class="form-block-label">
                      测试用例
                      <span class="required-star">*</span>
                    </label>
                    <button class="btn-add-option" @click="addTestCase">
                      <svg viewBox="0 0 16 16" width="14" height="14">
                        <line x1="8" y1="2.5" x2="8" y2="13.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                        <line x1="2.5" y1="8" x2="13.5" y2="8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                      </svg>
                      <span>添加测试用例</span>
                    </button>
                  </div>
                  <div class="test-cases-list">
                    <div
                      v-for="(tc, i) in editForm.testCases"
                      :key="i"
                      class="test-case-card"
                      :class="{ 'is-sample': tc.isSample }"
                    >
                      <div class="tc-header">
                        <div class="tc-header-left">
                          <span class="tc-num">Case {{ i + 1 }}</span>
                          <label class="tc-sample-toggle">
                            <input
                              type="checkbox"
                              :checked="tc.isSample"
                              @change="tc.isSample = !tc.isSample; flushDirtyState()"
                            />
                            <span class="tc-sample-label">样例</span>
                          </label>
                        </div>
                        <div class="tc-header-actions">
                          <button
                            v-if="editForm.testCases!.length > 1"
                            class="tc-move-btn"
                            :disabled="i === 0"
                            @click="moveTestCase(i, -1)"
                            title="上移"
                          >
                            <svg viewBox="0 0 16 16" width="14" height="14">
                              <polyline points="8,12 4,7 12,7" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
                            </svg>
                          </button>
                          <button
                            v-if="editForm.testCases!.length > 1"
                            class="tc-move-btn"
                            :disabled="i === editForm.testCases!.length - 1"
                            @click="moveTestCase(i, 1)"
                            title="下移"
                          >
                            <svg viewBox="0 0 16 16" width="14" height="14">
                              <polyline points="8,4 4,9 12,9" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
                            </svg>
                          </button>
                          <button
                            v-if="editForm.testCases!.length > 1"
                            class="tc-remove-btn"
                            @click="removeTestCase(i)"
                            title="删除用例"
                          >
                            <svg viewBox="0 0 16 16" width="13" height="13">
                              <line x1="4" y1="4" x2="12" y2="12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                              <line x1="12" y1="4" x2="4" y2="12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                            </svg>
                          </button>
                        </div>
                      </div>
                      <div class="tc-body">
                        <div class="tc-field">
                          <label class="tc-field-label">输入</label>
                          <textarea
                            v-model="tc.input"
                            class="tc-textarea"
                            rows="3"
                            spellcheck="false"
                            wrap="off"
                            :placeholder="`测试用例 ${i + 1} 的输入`"
                            @blur="flushDirtyState"
                          />
                        </div>
                        <div class="tc-field">
                          <label class="tc-field-label">预期输出</label>
                          <textarea
                            v-model="tc.expectedOutput"
                            class="tc-textarea"
                            rows="3"
                            spellcheck="false"
                            wrap="off"
                            :placeholder="`测试用例 ${i + 1} 的预期输出`"
                            @blur="flushDirtyState"
                          />
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </template>

              <!-- Analysis -->
              <div class="form-block">
                <label class="form-block-label">
                  解析
                  <span class="optional-tag">选填</span>
                </label>
                <textarea
                  v-model="editForm.analysis"
                  class="form-textarea"
                  :class="{ 'has-content': editForm.analysis.trim() }"
                  placeholder="输入题目解析，帮助学生理解..."
                  rows="3"
                  @blur="flushDirtyState"
                />
              </div>

              <!-- Actions -->
              <div class="form-actions">
                <button
                  class="btn-revert"
                  :disabled="!currentIsDirty"
                  @click="revertCurrent"
                >
                  <svg viewBox="0 0 16 16" width="14" height="14">
                    <polyline points="4,8 8,4 12,8" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
                    <line x1="8" y1="4" x2="8" y2="14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                  </svg>
                  <span>撤销修改</span>
                </button>
                <p class="form-save-hint">
                  <template v-if="currentIsDirty">此题目有未保存的修改</template>
                  <template v-else>此题目已保存</template>
                </p>
              </div>
            </div>
          </template>

          <!-- Empty / Loading -->
          <div v-else class="edit-empty">
            <div class="edit-empty-graphic">
              <svg viewBox="0 0 60 60" width="60" height="60">
                <circle cx="30" cy="30" r="24" fill="none" stroke="currentColor" stroke-width="1.5" opacity="0.2" />
                <line x1="30" y1="18" x2="30" y2="42" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" opacity="0.3" />
                <line x1="18" y1="30" x2="42" y2="30" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" opacity="0.3" />
              </svg>
            </div>
            <p class="edit-empty-desc" v-if="loading">加载中...</p>
            <p class="edit-empty-desc" v-else>请点击上方「添加题目」按钮创建题目</p>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap');

/* ============================================================
   BASE
   ============================================================ */
.editor-page {
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(160deg, #f0f7ff 0%, #e8f4fd 40%, #f5faff 100%);
  background-attachment: fixed;
  overflow: hidden;
}

/* ============================================================
   TOP BAR
   ============================================================ */
.editor-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(0, 102, 255, 0.08);
  flex-shrink: 0;
  z-index: 10;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.topbar-back {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  background: transparent;
  border: none;
  border-radius: 10px;
  color: #5a7a9a;
  font-size: 13px;
  font-family: inherit;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.topbar-back:hover {
  background: rgba(0, 102, 255, 0.07);
  color: #0066ff;
}

.topbar-divider {
  width: 1px;
  height: 24px;
  background: rgba(0, 102, 255, 0.12);
}

.topbar-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  color: #0066ff;
  flex-shrink: 0;
}

.topbar-title h1 {
  font-size: 18px;
  font-weight: 600;
  color: #1a3a5a;
  margin: 0;
  letter-spacing: -0.02em;
}

.topbar-count {
  font-size: 12px;
  font-weight: 500;
  color: #7a9aba;
  background: rgba(0, 102, 255, 0.07);
  padding: 2px 10px;
  border-radius: 20px;
  white-space: nowrap;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* === Save All Button === */
.btn-save-all {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 20px;
  background: rgba(0, 102, 255, 0.06);
  border: 1.5px solid rgba(0, 102, 255, 0.12);
  border-radius: 12px;
  color: #5a7a9a;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-save-all:hover:not(:disabled) {
  background: rgba(0, 102, 255, 0.1);
  border-color: rgba(0, 102, 255, 0.25);
  color: #0066ff;
}

.btn-save-all:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-save-all.has-dirty {
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 2px 12px rgba(0, 102, 255, 0.2);
}

.btn-save-all.has-dirty:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(0, 102, 255, 0.3);
}

.save-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 20px;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
}

/* === Done Button === */
.btn-done {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 22px;
  background: rgba(0, 102, 255, 0.06);
  border: 1.5px solid rgba(0, 102, 255, 0.12);
  border-radius: 12px;
  color: #5a7a9a;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-done:hover {
  background: rgba(0, 102, 255, 0.1);
  border-color: rgba(0, 102, 255, 0.25);
  color: #0066ff;
}

/* ============================================================
   EDITOR BODY
   ============================================================ */
.editor-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  gap: 0;
}

/* ============================================================
   LEFT: QUESTION SIDEBAR
   ============================================================ */
.question-sidebar {
  width: 340px;
  min-width: 340px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(12px);
  border-right: 1px solid rgba(0, 102, 255, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 18px 20px 14px;
  font-size: 14px;
  font-weight: 600;
  color: #1a3a5a;
}

.sidebar-header-icon {
  color: #7a9aba;
  flex-shrink: 0;
}

.sidebar-dirty-hint {
  margin-left: auto;
  font-size: 11px;
  font-weight: 500;
  color: #d97706;
  background: rgba(217, 119, 6, 0.1);
  padding: 2px 8px;
  border-radius: 20px;
}

/* Search */
.sidebar-search {
  position: relative;
  margin: 0 16px 14px;
}

.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #b0c8e0;
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 9px 34px 9px 36px;
  background: rgba(0, 102, 255, 0.04);
  border: 1px solid rgba(0, 102, 255, 0.1);
  border-radius: 12px;
  font-size: 13px;
  font-family: inherit;
  color: #1a3a5a;
  outline: none;
  box-sizing: border-box;
  transition: all 0.3s ease;
}

.search-input::placeholder {
  color: #b0c8e0;
}

.search-input:focus {
  border-color: rgba(0, 102, 255, 0.3);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.06);
}

.search-clear {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: none;
  background: rgba(0, 102, 255, 0.08);
  border-radius: 50%;
  color: #7a9aba;
  cursor: pointer;
  transition: all 0.2s ease;
}

.search-clear:hover {
  background: rgba(0, 102, 255, 0.15);
  color: #0066ff;
}

/* Scrollable area */
.sidebar-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 20px;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 102, 255, 0.1) transparent;
}

.sidebar-scroll::-webkit-scrollbar {
  width: 4px;
}

.sidebar-scroll::-webkit-scrollbar-thumb {
  background: rgba(0, 102, 255, 0.15);
  border-radius: 4px;
}

.sidebar-scroll::-webkit-scrollbar-track {
  background: transparent;
}

/* Loading */
.sidebar-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid rgba(0, 102, 255, 0.1);
  border-top-color: #0066ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  font-size: 13px;
  color: #7a9aba;
  margin: 0;
}

/* Question Group */
.question-group {
  margin-bottom: 20px;
}

.question-group:last-child {
  margin-bottom: 0;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px 8px;
  border-bottom: 1.5px solid rgba(0, 102, 255, 0.06);
  margin-bottom: 6px;
}

.group-numeral {
  font-size: 13px;
  font-weight: 700;
  color: var(--group-color);
  opacity: 0.6;
  letter-spacing: 0.05em;
}

.group-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--group-color);
  flex: 1;
}

.group-badge {
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  background: var(--group-color);
  padding: 1px 8px;
  border-radius: 20px;
  opacity: 0.85;
}

/* Question Item */
.group-items {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.question-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 10px 10px 12px;
  background: transparent;
  border: none;
  border-radius: 10px;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.question-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  bottom: 6px;
  width: 3px;
  border-radius: 3px;
  background: transparent;
  transition: all 0.25s ease;
}

.question-item:hover {
  background: rgba(0, 102, 255, 0.04);
}

.question-item:hover .item-delete {
  opacity: 1;
}

.question-item.active {
  background: rgba(0, 102, 255, 0.07);
}

.question-item.active::before {
  background: linear-gradient(180deg, #0066ff, #00d4ff);
}

.question-item.dirty.active::before {
  background: linear-gradient(180deg, #d97706, #f59e0b);
}

.item-icon {
  font-size: 12px;
  flex-shrink: 0;
  width: 18px;
  text-align: center;
  opacity: 0.7;
}

.item-stem {
  flex: 1;
  font-size: 13px;
  color: #3a5a7a;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Dirty Dot */
.item-dirty-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #d97706;
  box-shadow: 0 0 6px rgba(217, 119, 6, 0.4);
  animation: dirty-pulse 2s ease-in-out infinite;
}

@keyframes dirty-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(0.85); }
}

.item-delete {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  border-radius: 6px;
  color: #b0c8e0;
  cursor: pointer;
  opacity: 0;
  transition: all 0.2s ease;
}

.item-delete:hover {
  background: rgba(255, 77, 79, 0.1);
  color: #ff4d4f;
}

/* Sidebar Empty */
.sidebar-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
  text-align: center;
}

.sidebar-empty-icon {
  color: #b0c8e0;
  margin-bottom: 12px;
}

.sidebar-empty-text {
  font-size: 13px;
  color: #7a9aba;
  margin: 0;
  line-height: 1.5;
}

/* ============================================================
   RIGHT: EDITOR MAIN
   ============================================================ */
.editor-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: rgba(248, 250, 255, 0.5);
}

/* ===== Add Section ===== */
.add-section {
  flex-shrink: 0;
  padding: 20px 28px 16px;
  border-bottom: 1px solid rgba(0, 102, 255, 0.06);
  background: rgba(255, 255, 255, 0.6);
}

.add-section-header {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  font-weight: 600;
  color: #7a9aba;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.add-section-icon {
  color: #b0c8e0;
}

.add-type-list {
  display: flex;
  gap: 10px;
}

.add-type-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  background: rgba(255, 255, 255, 0.8);
  border: 1.5px solid rgba(0, 102, 255, 0.1);
  border-radius: 12px;
  color: #3a5a7a;
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.25s ease;
}

.add-type-btn:hover {
  border-color: var(--btn-color);
  background: color-mix(in srgb, var(--btn-color) 6%, white);
  color: var(--btn-color);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px color-mix(in srgb, var(--btn-color) 12%, transparent);
}

.add-type-icon {
  font-size: 15px;
  line-height: 1;
}

.add-type-label {
  font-size: 13px;
}

/* ===== Edit Section ===== */
.edit-section {
  flex: 1;
  overflow-y: auto;
  padding: 24px 28px 32px;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 102, 255, 0.1) transparent;
}

.edit-section::-webkit-scrollbar {
  width: 4px;
}

.edit-section::-webkit-scrollbar-thumb {
  background: rgba(0, 102, 255, 0.15);
  border-radius: 4px;
}

.edit-section::-webkit-scrollbar-track {
  background: transparent;
}

.edit-form {
  max-width: 720px;
  margin: 0 auto;
}

.edit-form-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1.5px solid rgba(0, 102, 255, 0.06);
}

.edit-form-type {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--type-color);
}

.edit-type-icon {
  font-size: 18px;
}

.edit-dirty-badge {
  font-size: 11px;
  font-weight: 500;
  color: #d97706;
  background: rgba(217, 119, 6, 0.1);
  padding: 1px 8px;
  border-radius: 20px;
  margin-left: 4px;
}

.edit-form-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.edit-action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
}

.delete-btn {
  background: rgba(255, 77, 79, 0.08);
  color: #ff4d4f;
}

.delete-btn:hover {
  background: #ff4d4f;
  color: #fff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
}

.edit-id-badge {
  font-size: 12px;
  font-weight: 500;
  color: #b0c8e0;
  font-family: 'SF Mono', 'Fira Code', monospace;
}

/* ===== Form Blocks ===== */
.form-block {
  margin-bottom: 22px;
}

.form-block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.form-block-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #3a5a7a;
  margin-bottom: 8px;
}

.required-star {
  color: #ff4d4f;
  margin-left: 2px;
}

.optional-tag {
  font-weight: 400;
  font-size: 11px;
  color: #b0c8e0;
  margin-left: 6px;
}

.form-textarea {
  width: 100%;
  padding: 12px 16px;
  background: #fff;
  border: 1.5px solid rgba(0, 102, 255, 0.12);
  border-radius: 14px;
  font-size: 14px;
  font-family: inherit;
  color: #1a3a5a;
  outline: none;
  box-sizing: border-box;
  transition: all 0.3s ease;
  line-height: 1.6;
  resize: vertical;
}

.form-textarea::placeholder {
  color: #c0d4e8;
}

.form-textarea:focus {
  border-color: #0066ff;
  box-shadow: 0 0 0 4px rgba(0, 102, 255, 0.08);
  background: #fff;
}

.form-textarea.has-content {
  background: #fff;
}

.form-input {
  width: 100%;
  padding: 10px 16px;
  background: #fff;
  border: 1.5px solid rgba(0, 102, 255, 0.12);
  border-radius: 12px;
  font-size: 14px;
  font-family: inherit;
  color: #1a3a5a;
  outline: none;
  box-sizing: border-box;
  transition: all 0.3s ease;
}

.form-input::placeholder {
  color: #c0d4e8;
}

.form-input:focus {
  border-color: #0066ff;
  box-shadow: 0 0 0 4px rgba(0, 102, 255, 0.08);
}

/* ===== Options ===== */
.options-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px 6px 6px;
  background: #fff;
  border: 1.5px solid rgba(0, 102, 255, 0.08);
  border-radius: 12px;
  transition: all 0.2s ease;
}

.option-row:focus-within {
  border-color: rgba(0, 102, 255, 0.25);
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.05);
}

.option-select-col {
  flex-shrink: 0;
}

.option-radio,
.option-checkbox {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: 2px solid rgba(0, 102, 255, 0.2);
  background: transparent;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 0;
}

.option-radio {
  border-radius: 50%;
}

.option-radio.selected {
  border-color: #0066ff;
  background: #0066ff;
}

.option-radio-dot {
  width: 8px;
  height: 8px;
  background: #fff;
  border-radius: 50%;
}

.option-checkbox {
  border-radius: 5px;
}

.option-checkbox.selected {
  border-color: #7c3aed;
  background: #7c3aed;
  color: #fff;
}

.option-label {
  flex-shrink: 0;
  font-size: 14px;
  font-weight: 700;
  width: 20px;
  text-align: center;
  opacity: 0.7;
}

.option-input {
  flex: 1;
  padding: 8px 0;
  background: transparent;
  border: none;
  font-size: 14px;
  font-family: inherit;
  color: #1a3a5a;
  outline: none;
}

.option-input::placeholder {
  color: #c0d4e8;
}

.option-remove {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: none;
  background: transparent;
  border-radius: 6px;
  color: #c0d4e8;
  cursor: pointer;
  transition: all 0.2s ease;
}

.option-remove:hover {
  background: rgba(255, 77, 79, 0.08);
  color: #ff4d4f;
}

.btn-add-option {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  background: rgba(0, 102, 255, 0.06);
  border: 1px dashed rgba(0, 102, 255, 0.2);
  border-radius: 8px;
  color: #0066ff;
  font-size: 12px;
  font-family: inherit;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-add-option:hover {
  background: rgba(0, 102, 255, 0.1);
  border-color: rgba(0, 102, 255, 0.35);
}

/* Hint Block */
.hint-block {
  margin-top: -8px;
  margin-bottom: 22px;
}

.hint-text {
  font-size: 12px;
  color: #b0c8e0;
  font-weight: 400;
}

.answer-display-block {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
  margin-bottom: 22px;
  padding: 10px 16px;
  background: #f0f7ff;
  border-radius: 8px;
  border: 1px solid #d6e8ff;
}

.answer-display-block .form-block-label {
  margin-bottom: 0;
  font-size: 13px;
  color: #0066ff;
}

.answer-display-text {
  font-size: 14px;
  color: #1a2332;
}

.answer-display-text strong {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  margin-left: 2px;
  border-radius: 50%;
  background: #0066ff;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

/* ===== Fill Blank ===== */
.fill-blank-input-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.fill-blank-prefix {
  font-size: 14px;
  font-weight: 600;
  color: #3a5a7a;
  white-space: nowrap;
}

.form-hint {
  font-size: 12px;
  color: #b0c8e0;
  margin: 8px 0 0;
}

/* ===== Programming Question ===== */

/* Template Code */
.template-code-wrap {
  border: 1.5px solid rgba(0, 102, 255, 0.12);
  border-radius: 14px;
  overflow: hidden;
  transition: border-color 0.3s ease;
}

.template-code-wrap:focus-within {
  border-color: #0066ff;
  box-shadow: 0 0 0 4px rgba(0, 102, 255, 0.08);
}

.code-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: rgba(0, 102, 255, 0.04);
  border-bottom: 1px solid rgba(0, 102, 255, 0.08);
}

.lang-select {
  font-size: 12px;
  font-weight: 600;
  color: #16a34a;
  background: transparent;
  border: 1.5px solid rgba(22, 163, 74, 0.2);
  border-radius: 8px;
  padding: 4px 28px 4px 10px;
  font-family: inherit;
  cursor: pointer;
  outline: none;
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 16 16' xmlns='http://www.w3.org/2000/svg'%3E%3Cpolyline points='4,6 8,10 12,6' fill='none' stroke='%2316a34a' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  background-size: 12px;
  transition: border-color 0.2s ease;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.lang-select:hover {
  border-color: rgba(22, 163, 74, 0.5);
}

.lang-select:focus {
  border-color: #16a34a;
  box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.1);
}

.lang-select option {
  color: #1a3a5a;
  background: #fff;
  text-transform: none;
}

.code-lang-id {
  font-size: 11px;
  color: #b0c8e0;
  font-family: 'SF Mono', 'Fira Code', monospace;
}

.code-textarea {
  width: 100%;
  padding: 16px;
  border: none;
  background: #f8fafc;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #1a3a5a;
  outline: none;
  resize: vertical;
  tab-size: 4;
  white-space: pre;
  overflow: auto;
  box-sizing: border-box;
}

.code-textarea::placeholder {
  color: #c0d4e8;
}

/* Limits Row */
.limits-row {
  display: flex;
  gap: 16px;
}

.limit-item {
  flex: 1;
}

.limit-label {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: #7a9aba;
  margin-bottom: 6px;
}

/* Test Cases */
.test-cases-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.test-case-card {
  background: #fff;
  border: 1.5px solid rgba(0, 102, 255, 0.1);
  border-radius: 14px;
  overflow: hidden;
  transition: all 0.25s ease;
}

.test-case-card.is-sample {
  border-color: rgba(22, 163, 74, 0.2);
  background: rgba(22, 163, 74, 0.02);
}

.test-case-card:focus-within {
  border-color: rgba(0, 102, 255, 0.25);
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.05);
}

.tc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  background: rgba(0, 102, 255, 0.03);
  border-bottom: 1px solid rgba(0, 102, 255, 0.06);
}

.tc-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tc-num {
  font-size: 12px;
  font-weight: 600;
  color: #3a5a7a;
}

.tc-sample-toggle {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  font-size: 12px;
  color: #7a9aba;
}

.tc-sample-toggle input[type="checkbox"] {
  width: 14px;
  height: 14px;
  accent-color: #16a34a;
  cursor: pointer;
}

.tc-sample-label {
  font-weight: 500;
}

.tc-header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.tc-move-btn,
.tc-remove-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: none;
  background: transparent;
  border-radius: 6px;
  color: #b0c8e0;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tc-move-btn:hover:not(:disabled) {
  background: rgba(0, 102, 255, 0.08);
  color: #0066ff;
}

.tc-move-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.tc-remove-btn:hover {
  background: rgba(255, 77, 79, 0.08);
  color: #ff4d4f;
}

.tc-body {
  display: flex;
  gap: 1px;
  background: rgba(0, 102, 255, 0.06);
}

.tc-field {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.tc-field + .tc-field {
  border-left: 1px solid rgba(0, 102, 255, 0.06);
}

.tc-field-label {
  font-size: 11px;
  font-weight: 600;
  color: #7a9aba;
  padding: 6px 14px 0;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.tc-textarea {
  flex: 1;
  padding: 8px 14px 12px;
  border: none;
  background: transparent;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.5;
  color: #1a3a5a;
  outline: none;
  resize: vertical;
  white-space: pre;
  overflow: auto;
  min-height: 60px;
}

.tc-textarea::placeholder {
  color: #c0d4e8;
}

/* ===== Form Actions ===== */
.form-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 32px;
  padding-top: 20px;
  border-top: 1px solid rgba(0, 102, 255, 0.06);
}

.btn-revert {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 18px;
  background: rgba(0, 102, 255, 0.05);
  border: 1px solid rgba(0, 102, 255, 0.1);
  border-radius: 10px;
  color: #5a7a9a;
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-revert:hover:not(:disabled) {
  background: rgba(217, 119, 6, 0.08);
  border-color: rgba(217, 119, 6, 0.2);
  color: #d97706;
}

.btn-revert:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.form-save-hint {
  flex: 1;
  text-align: right;
  font-size: 12px;
  color: #b0c8e0;
  margin: 0;
}

/* ===== Empty State ===== */
.edit-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 40px;
  text-align: center;
}

.edit-empty-graphic {
  color: #b0c8e0;
  margin-bottom: 20px;
}

.edit-empty-title {
  font-size: 16px;
  font-weight: 600;
  color: #3a5a7a;
  margin: 0 0 8px;
}

.edit-empty-desc {
  font-size: 13px;
  color: #7a9aba;
  margin: 0;
  line-height: 1.6;
  max-width: 280px;
}

/* ===== Flash Attention Animation ===== */
.flash-attention {
  animation: flash-blink 2.4s ease-in-out;
}

@keyframes flash-blink {
  0%, 100% {
    background: rgba(255, 255, 255, 0.6);
    border-color: rgba(0, 102, 255, 0.06);
  }
  15%, 55% {
    background: rgba(0, 102, 255, 0.08);
    border-color: rgba(0, 102, 255, 0.25);
    box-shadow: 0 0 24px rgba(0, 102, 255, 0.12);
  }
  35%, 75% {
    background: rgba(255, 255, 255, 0.6);
    border-color: rgba(0, 102, 255, 0.06);
  }
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 1024px) {
  .question-sidebar {
    width: 280px;
    min-width: 280px;
  }
}

@media (max-width: 768px) {
  .question-sidebar {
    width: 240px;
    min-width: 240px;
  }

  .editor-topbar {
    padding: 0 16px;
  }

  .topbar-title h1 {
    font-size: 15px;
    max-width: 140px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .edit-form {
    max-width: 100%;
  }
}
</style>
