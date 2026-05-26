<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import CourseLayout from '@/layouts/CourseLayout.vue'
import { getMaterialList, downloadMaterial, batchDownloadMaterials } from '@/api/course'
import { getStudentHomeworkList } from '@/api/homework'
import { getStudentExamList } from '@/api/exam'
import { useAIStore } from '@/stores/ai'
import AIChat from '@/components/AIChat.vue'

const route = useRoute()
const router = useRouter()
const aiStore = useAIStore()

const activeTab = computed(() => (route.query.tab as string) || 'chapters')
const courseId = computed(() => route.params.id as string)
const courseName = computed(() => route.query.name as string || '')

const tabTitles: Record<string, string> = {
  chapters: '课程章节',
  homework: '作业',
  exam: '考试',
  lab: '实验',
  material: '资料',
}

const placeholderContent: Record<string, string> = {
  chapters: '课程章节内容区域（待接入后端API）',
  homework: '作业列表区域（待接入后端API）',
  lab: '实验列表区域（待接入后端API）',
  material: '资料列表区域（待接入后端API）',
}

// ==================== 课程资料 ====================
interface Material {
  id: number
  courseId: number
  fileName: string
  fileSize: number
  fileType: string
  fileExt: string
  duration: string | null
  createTime: string
}

const materials = ref<Material[]>([])
const materialsLoading = ref(false)

async function loadMaterials() {
  const courseId = route.params.id as string
  if (!courseId) return
  materialsLoading.value = true
  try {
    const list = await getMaterialList(courseId)
    materials.value = list.map(item => ({
      id: item.id,
      courseId: item.courseId,
      fileName: item.fileName,
      fileSize: item.fileSize,
      fileType: item.fileType,
      fileExt: item.fileExt,
      duration: item.duration,
      createTime: item.createTime,
    }))
  } catch {
    // 错误已由拦截器统一处理
  } finally {
    materialsLoading.value = false
  }
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

function getFileIcon(ext: string): string {
  const upper = ext?.toUpperCase() || ''
  const pdfTypes = ['PDF']
  const officeTypes = ['DOC', 'DOCX', 'PPT', 'PPTX', 'XLS', 'XLSX']
  const codeTypes = ['JS', 'TS', 'VUE', 'JAVA', 'PY', 'GO', 'TSX', 'JSX']
  const imageTypes = ['PNG', 'JPG', 'JPEG', 'GIF', 'SVG', 'WEBP']
  const videoTypes = ['MP4', 'AVI', 'MOV', 'WMV']
  const zipTypes = ['ZIP', 'RAR', '7Z', 'TAR', 'GZ']

  if (pdfTypes.includes(upper)) return 'pdf'
  if (officeTypes.includes(upper)) return 'office'
  if (codeTypes.includes(upper)) return 'code'
  if (imageTypes.includes(upper)) return 'image'
  if (videoTypes.includes(upper)) return 'video'
  if (zipTypes.includes(upper)) return 'zip'
  return 'other'
}

// ==================== 下载 ====================
const selectedIds = ref<number[]>([])
const batchDownloading = ref(false)

function toggleSelect(id: number) {
  const idx = selectedIds.value.indexOf(id)
  if (idx === -1) {
    selectedIds.value.push(id)
  } else {
    selectedIds.value.splice(idx, 1)
  }
}

function isSelected(id: number) {
  return selectedIds.value.includes(id)
}

function clearSelection() {
  selectedIds.value = []
}

async function triggerDownloadBlob(materialId: number, fileName: string) {
  const res = await downloadMaterial(materialId)
  const blob: Blob = res.data
  const disposition: string = res.headers?.['content-disposition'] || ''
  const match = disposition.match(/filename\*?=(?:UTF-8'')?([^;]+)/)
  const name = match ? decodeURIComponent(match[1]) : fileName
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = name
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

async function handleDownload(mat: Material) {
  try {
    await triggerDownloadBlob(mat.id, mat.fileName)
    ElMessage.success('下载成功')
  } catch {
    // 错误已由拦截器统一处理
  }
}

async function handleBatchDownload() {
  if (selectedIds.value.length === 0) return
  batchDownloading.value = true
  try {
    const res = await batchDownloadMaterials(selectedIds.value)
    const blob: Blob = res.data
    const disposition: string = res.headers?.['content-disposition'] || ''
    const match = disposition.match(/filename\*?=(?:UTF-8'')?([^;]+)/)
    const name = match ? decodeURIComponent(match[1]) : 'materials.zip'
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = name
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success(`已下载 ${selectedIds.value.length} 个文件`)
    clearSelection()
  } catch {
    // 错误已由拦截器统一处理
  } finally {
    batchDownloading.value = false
  }
}

// ==================== 作业列表 ====================
interface HomeworkItem {
  id: number
  title: string
  deadline: string
  status: number
}

const homeworkList = ref<HomeworkItem[]>([])
const homeworkLoading = ref(false)

async function loadHomework() {
  const courseId = route.params.id as string
  if (!courseId) return
  homeworkLoading.value = true
  try {
    homeworkList.value = await getStudentHomeworkList(courseId)
  } catch {
    // 错误已由拦截器统一处理
  } finally {
    homeworkLoading.value = false
  }
}

function formatDeadline(dt: string): string {
  return dt.replace(' ', '  ')
}

function goDoHomework(id: number) {
  router.push({
    name: 'homeworkDo',
    params: { id },
    query: { courseId: courseId.value, courseName: courseName.value },
  })
}

// ==================== 考试列表 ====================
interface ExamItem {
  id: number
  title: string
  startTime: string
  endTime: string
  duration: number
  status: number
}

const examList = ref<ExamItem[]>([])
const examLoading = ref(false)

async function loadExams() {
  const courseId = route.params.id as string
  if (!courseId) return
  examLoading.value = true
  try {
    examList.value = await getStudentExamList(courseId)
  } catch {
    // 错误已由拦截器统一处理
  } finally {
    examLoading.value = false
  }
}

function formatExamTime(dt: string): string {
  return dt.replace('T', ' ').slice(0, 16)
}

const examStatusLabels: Record<number, { label: string; color: string; bg: string }> = {
  0: { label: '未开始', color: '#ff9800', bg: 'rgba(255, 152, 0, 0.08)' },
  1: { label: '进行中', color: '#0066ff', bg: 'rgba(0, 102, 255, 0.08)' },
  2: { label: '已提交', color: '#00c853', bg: 'rgba(0, 200, 83, 0.08)' },
  3: { label: '已结束', color: '#9e9e9e', bg: 'rgba(158, 158, 158, 0.08)' },
}

function goDoExam(id: number) {
  router.push({
    name: 'examDo',
    params: { id },
    query: { courseId: courseId.value, courseName: courseName.value },
  })
}

// 切换到资料 tab 时加载
watch(activeTab, (tab) => {
  if (tab === 'material' && materials.value.length === 0) {
    loadMaterials()
  }
  if (tab === 'homework' && homeworkList.value.length === 0) {
    loadHomework()
  }
  if (tab === 'exam' && examList.value.length === 0) {
    loadExams()
  }
  if (tab === 'ai') {
    aiStore.initCourse(courseId.value, courseName.value)
    aiStore.setFullMode()
  }
})

onMounted(() => {
  if (activeTab.value === 'material') {
    loadMaterials()
  }
  if (activeTab.value === 'homework') {
    loadHomework()
  }
  if (activeTab.value === 'exam') {
    loadExams()
  }
  if (activeTab.value === 'ai') {
    aiStore.initCourse(courseId.value, courseName.value)
    aiStore.setFullMode()
  }
})
</script>

<template>
  <CourseLayout>
    <div class="detail-content">
      <!-- 内容区头部 -->
      <div class="content-header">
        <h2 class="content-title">{{ tabTitles[activeTab] || '课程内容' }}</h2>
      </div>

      <!-- ==================== 课程资料 ==================== -->
      <div v-if="activeTab === 'material'" class="section">
        <div class="section-header">
          <h2 class="section-title">课程资料</h2>
          <button
            v-if="selectedIds.length > 0"
            class="download-btn"
            :disabled="batchDownloading"
            @click="handleBatchDownload"
          >
            <svg viewBox="0 0 16 16" width="14" height="14">
              <path d="M8 12V3M4 7l4 5 4-5" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
              <line x1="2" y1="13" x2="14" y2="13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
            <span>{{ batchDownloading ? '下载中...' : `批量下载 (${selectedIds.length})` }}</span>
          </button>
        </div>

        <!-- 加载中 -->
        <div v-if="materialsLoading" class="loading-area">
          <p>加载中...</p>
        </div>

        <!-- 空状态 -->
        <div v-else-if="materials.length === 0" class="placeholder-area">
          <div class="placeholder-card">
            <div class="placeholder-icon">
              <svg viewBox="0 0 64 64" class="ph-svg">
                <defs>
                  <linearGradient id="phGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" style="stop-color:#00d4ff;stop-opacity:1" />
                    <stop offset="100%" style="stop-color:#0066ff;stop-opacity:1" />
                  </linearGradient>
                </defs>
                <path d="M20 50V14h20l8 8v28z" fill="none" stroke="url(#phGrad)" stroke-width="1.5"/>
                <line x1="20" y1="26" x2="50" y2="26" stroke="url(#phGrad)" stroke-width="1" opacity="0.3"/>
                <line x1="20" y1="34" x2="44" y2="34" stroke="url(#phGrad)" stroke-width="1" opacity="0.25"/>
                <line x1="20" y1="42" x2="38" y2="42" stroke="url(#phGrad)" stroke-width="1" opacity="0.25"/>
                <path d="M40 14v8h8" fill="none" stroke="url(#phGrad)" stroke-width="1.5"/>
              </svg>
            </div>
            <p class="placeholder-text">暂无课程资料</p>
          </div>
        </div>

        <!-- 资料列表 — 文件夹网格 -->
        <div v-else class="material-grid">
          <div v-for="mat in materials" :key="mat.id" class="material-card" :class="{ selected: isSelected(mat.id) }">
            <div class="card-actions">
              <span
                class="card-check"
                :class="{ checked: isSelected(mat.id) }"
                @click="toggleSelect(mat.id)"
                :title="isSelected(mat.id) ? '取消选择' : '选择'"
              >
                <svg v-if="isSelected(mat.id)" viewBox="0 0 16 16" width="14" height="14">
                  <circle cx="8" cy="8" r="7" fill="currentColor" stroke="none"/>
                  <polyline points="5,8 7,10 11,6" stroke="#fff" stroke-width="1.6" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <svg v-else viewBox="0 0 16 16" width="14" height="14">
                  <circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.2" fill="none"/>
                </svg>
              </span>
            </div>
            <div class="card-icon" :class="'card-icon-' + getFileIcon(mat.fileExt)">
              <!-- PDF -->
              <svg v-if="getFileIcon(mat.fileExt) === 'pdf'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M6 2h10l4 4v16H6z"/>
                <path d="M16 2v4h4"/>
                <text x="12" y="18" text-anchor="middle" font-size="9" font-weight="bold" fill="currentColor">PDF</text>
              </svg>
              <!-- Office -->
              <svg v-if="getFileIcon(mat.fileExt) === 'office'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M6 2h8l4 4v16H6z"/>
                <path d="M14 2v4h4"/>
                <line x1="8" y1="12" x2="16" y2="12"/>
                <line x1="8" y1="16" x2="14" y2="16"/>
              </svg>
              <!-- Code -->
              <svg v-if="getFileIcon(mat.fileExt) === 'code'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M6 2h10l4 4v16H6z"/>
                <path d="M16 2v4h4"/>
                <line x1="9" y1="13" x2="11" y2="15"/>
                <line x1="11" y1="13" x2="9" y2="15"/>
                <line x1="14" y1="13" x2="16" y2="15"/>
              </svg>
              <!-- Image -->
              <svg v-if="getFileIcon(mat.fileExt) === 'image'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="4" y="4" width="16" height="16" rx="2"/>
                <circle cx="9" cy="9" r="1.5"/>
                <path d="M4 16l5-5 4 4 3-3 4 4"/>
              </svg>
              <!-- Video -->
              <svg v-if="getFileIcon(mat.fileExt) === 'video'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="3" y="5" width="14" height="14" rx="2"/>
                <polygon points="19,9 22,12 19,15" stroke-linejoin="round"/>
              </svg>
              <!-- Zip -->
              <svg v-if="getFileIcon(mat.fileExt) === 'zip'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M6 2h10l4 4v16H6z"/>
                <path d="M16 2v4h4"/>
                <line x1="10" y1="10" x2="14" y2="10"/>
                <line x1="12" y1="10" x2="12" y2="18"/>
              </svg>
              <!-- Other -->
              <svg v-if="getFileIcon(mat.fileExt) === 'other'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M6 2h10l4 4v16H6z"/>
                <path d="M16 2v4h4"/>
                <line x1="8" y1="12" x2="16" y2="12"/>
              </svg>
            </div>
            <span class="card-name" :title="mat.fileName">{{ mat.fileName }}</span>
            <span class="card-meta">{{ formatSize(mat.fileSize) }}</span>
            <button class="card-download" @click="handleDownload(mat)" title="下载">
              <svg viewBox="0 0 14 14" width="12" height="12">
                <path d="M7 10V2M3 6l4 4 4-4" stroke="currentColor" stroke-width="1.4" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                <line x1="2" y1="12" x2="12" y2="12" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
              </svg>
              <span>下载</span>
            </button>
          </div>
        </div>
      </div>

      <!-- ==================== 作业列表 ==================== -->
      <div v-else-if="activeTab === 'homework'" class="section">
        <div class="section-header">
          <h2 class="section-title">课程作业</h2>
        </div>

        <!-- 加载中 -->
        <div v-if="homeworkLoading" class="loading-area">
          <p>加载中...</p>
        </div>

        <!-- 空状态 -->
        <div v-else-if="homeworkList.length === 0" class="placeholder-area">
          <div class="placeholder-card">
            <div class="placeholder-icon">
              <svg viewBox="0 0 64 64" class="ph-svg">
                <defs>
                  <linearGradient id="hwGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" style="stop-color:#00d4ff;stop-opacity:1" />
                    <stop offset="100%" style="stop-color:#0066ff;stop-opacity:1" />
                  </linearGradient>
                </defs>
                <rect x="12" y="8" width="40" height="48" rx="4" fill="none" stroke="url(#hwGrad)" stroke-width="1.5"/>
                <line x1="22" y1="22" x2="42" y2="22" stroke="url(#hwGrad)" stroke-width="1" opacity="0.3"/>
                <line x1="22" y1="30" x2="38" y2="30" stroke="url(#hwGrad)" stroke-width="1" opacity="0.25"/>
                <line x1="22" y1="38" x2="34" y2="38" stroke="url(#hwGrad)" stroke-width="1" opacity="0.25"/>
                <path d="M26 46l4 4 8-8" stroke="url(#hwGrad)" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <p class="placeholder-text">暂无作业</p>
          </div>
        </div>

        <!-- 作业列表 -->
        <div v-else class="homework-list">
          <div v-for="item in homeworkList" :key="item.id" class="homework-item">
            <div class="homework-info">
              <span class="homework-title">{{ item.title }}</span>
              <span class="homework-deadline">
                <svg viewBox="0 0 16 16" width="13" height="13" class="deadline-icon">
                  <circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.2" fill="none"/>
                  <polyline points="8,4.5 8,8 11,9.5" stroke="currentColor" stroke-width="1.2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                截止时间: {{ formatDeadline(item.deadline) }}
              </span>
            </div>
            <div class="homework-action">
              <!-- 未提交（未到期）-->
              <button
                v-if="item.status === 0"
                class="do-btn"
                @click="goDoHomework(item.id)"
              >
                去做作业
                <svg viewBox="0 0 14 14" width="12" height="12" class="btn-arrow">
                  <polyline points="5,3 9,7 5,11" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </button>
              <!-- 已提交 -->
              <button
                v-else-if="item.status === 1"
                class="review-btn"
                @click="goDoHomework(item.id)"
              >
                <svg viewBox="0 0 14 14" width="12" height="12">
                  <circle cx="7" cy="7" r="5.5" stroke="currentColor" stroke-width="1.3" fill="none"/>
                  <path d="M7 5v3M7 9.5v.01" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
                </svg>
                查看
              </button>
              <!-- 已被批改 -->
              <button
                v-else-if="item.status === 2"
                class="grade-btn"
                @click="goDoHomework(item.id)"
              >
                <svg viewBox="0 0 14 14" width="12" height="12">
                  <polyline points="2,7 5,10 12,3" stroke="currentColor" stroke-width="1.4" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                查看成绩
              </button>
              <!-- 已过期 -->
              <span v-else class="deadline-tag">已过期</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ==================== 考试列表 ==================== -->
      <div v-else-if="activeTab === 'exam'" class="section">
        <div class="section-header">
          <h2 class="section-title">课程考试</h2>
        </div>

        <div v-if="examLoading" class="loading-area">
          <p>加载中...</p>
        </div>

        <div v-else-if="examList.length === 0" class="placeholder-area">
          <div class="placeholder-card">
            <div class="placeholder-icon">
              <svg viewBox="0 0 64 64" class="ph-svg">
                <defs>
                  <linearGradient id="examGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" style="stop-color:#00d4ff;stop-opacity:1" />
                    <stop offset="100%" style="stop-color:#0066ff;stop-opacity:1" />
                  </linearGradient>
                </defs>
                <rect x="12" y="8" width="40" height="48" rx="4" fill="none" stroke="url(#examGrad)" stroke-width="1.5"/>
                <line x1="22" y1="22" x2="42" y2="22" stroke="url(#examGrad)" stroke-width="1" opacity="0.3"/>
                <line x1="22" y1="30" x2="38" y2="30" stroke="url(#examGrad)" stroke-width="1" opacity="0.25"/>
                <line x1="22" y1="38" x2="34" y2="38" stroke="url(#examGrad)" stroke-width="1" opacity="0.25"/>
                <circle cx="38" cy="48" r="8" fill="none" stroke="url(#examGrad)" stroke-width="1.5"/>
                <polyline points="35,48 37.5,50.5 42,45" stroke="url(#examGrad)" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <p class="placeholder-text">暂无考试</p>
          </div>
        </div>

        <div v-else class="homework-list">
          <div v-for="item in examList" :key="item.id" class="homework-item">
            <div class="homework-info">
              <div style="display:flex;align-items:center;gap:10px;">
                <span class="homework-title">{{ item.title }}</span>
                <span
                  class="exam-status-tag"
                  :style="{
                    color: examStatusLabels[item.status]?.color,
                    background: examStatusLabels[item.status]?.bg,
                  }"
                >
                  {{ examStatusLabels[item.status]?.label }}
                </span>
              </div>
              <span class="homework-deadline">
                <svg viewBox="0 0 16 16" width="13" height="13" class="deadline-icon">
                  <circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.2" fill="none"/>
                  <polyline points="8,4.5 8,8 11,9.5" stroke="currentColor" stroke-width="1.2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                {{ formatExamTime(item.startTime) }} ~ {{ formatExamTime(item.endTime) }}
              </span>
            </div>
            <div class="homework-action">
              <button
                v-if="item.status === 0 || item.status === 1"
                class="do-btn"
                @click="goDoExam(item.id)"
              >
                {{ item.status === 0 ? '查看' : '去考试' }}
                <svg viewBox="0 0 14 14" width="12" height="12" class="btn-arrow">
                  <polyline points="5,3 9,7 5,11" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </button>
              <button
                v-else
                class="review-btn"
                @click="goDoExam(item.id)"
              >
                <svg viewBox="0 0 14 14" width="12" height="12">
                  <circle cx="7" cy="7" r="5.5" stroke="currentColor" stroke-width="1.3" fill="none"/>
                  <path d="M7 5v3M7 9.5v.01" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
                </svg>
                查看
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- ==================== AI 助教 ==================== -->
      <div v-else-if="activeTab === 'ai'" class="ai-section">
        <AIChat
          mode="full"
          :course-id="courseId"
          :course-name="courseName"
        />
      </div>

      <!-- ==================== 其他 Tab 占位 ==================== -->
      <div v-else class="placeholder-area">
        <div class="placeholder-card">
          <div class="placeholder-icon">
            <svg viewBox="0 0 64 64" class="ph-svg">
              <defs>
                <linearGradient id="phGrad2" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style="stop-color:#00d4ff;stop-opacity:1" />
                  <stop offset="100%" style="stop-color:#0066ff;stop-opacity:1" />
                </linearGradient>
              </defs>
              <rect x="8" y="12" width="48" height="40" rx="4" fill="none" stroke="url(#phGrad2)" stroke-width="1.5" />
              <line x1="8" y1="24" x2="56" y2="24" stroke="url(#phGrad2)" stroke-width="1" opacity="0.3" />
              <line x1="20" y1="34" x2="48" y2="34" stroke="url(#phGrad2)" stroke-width="1" opacity="0.25" />
              <line x1="20" y1="42" x2="40" y2="42" stroke="url(#phGrad2)" stroke-width="1" opacity="0.25" />
            </svg>
          </div>
          <p class="placeholder-text">{{ placeholderContent[activeTab] || '请选择功能模块' }}</p>
        </div>
      </div>
    </div>
  </CourseLayout>
</template>

<style scoped>
.detail-content {
  height: 100%;
}

.content-header {
  margin-bottom: 32px;
}

.content-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a3a5a;
  margin: 0;
}

/* ====== 课程资料 ====== */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 12px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a3a5a;
  margin: 0;
}

.download-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  background: rgba(255, 255, 255, 0.85);
  color: #0066ff;
  border: 1px solid rgba(0, 102, 255, 0.2);
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.download-btn:hover:not(:disabled) {
  background: #fff;
  border-color: #0066ff;
  box-shadow: 0 4px 14px rgba(0, 102, 255, 0.12);
}

.download-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-area {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  color: #7a9aba;
  font-size: 15px;
}

/* 资料网格 */
.material-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 14px;
}

.material-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 12px 14px;
  background: rgba(255, 255, 255, 0.75);
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 2px 8px rgba(0, 102, 255, 0.03);
  transition: all 0.2s ease;
  cursor: default;
}

.material-card:hover {
  background: rgba(255, 255, 255, 0.95);
  border-color: rgba(0, 102, 255, 0.12);
  box-shadow: 0 6px 20px rgba(0, 102, 255, 0.07);
  transform: translateY(-2px);
}

.material-card.selected {
  border-color: #0066ff;
  background: rgba(0, 102, 255, 0.04);
  box-shadow: 0 0 0 2px rgba(0, 102, 255, 0.12);
}

.card-actions {
  position: absolute;
  top: 6px;
  right: 6px;
  left: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.material-card:hover .card-actions,
.material-card.selected .card-actions {
  opacity: 1;
}

.card-check {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  color: #c0d0e0;
  cursor: pointer;
  transition: all 0.2s ease;
}

.card-check:hover {
  color: #0066ff;
}

.card-check.checked {
  color: #0066ff;
}

.card-download {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
  padding: 5px 0;
  margin-top: 2px;
  border: none;
  background: rgba(0, 102, 255, 0.06);
  border-radius: 8px;
  color: #0066ff;
  font-size: 12px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
}

.card-download:hover {
  background: rgba(0, 102, 255, 0.12);
}

.card-icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  flex-shrink: 0;
}

.card-icon svg {
  width: 32px;
  height: 32px;
}

.card-icon-pdf { background: rgba(255, 51, 51, 0.08); color: #e53935; }
.card-icon-office { background: rgba(0, 102, 255, 0.08); color: #0066ff; }
.card-icon-code { background: rgba(156, 39, 176, 0.08); color: #9c27b0; }
.card-icon-image { background: rgba(0, 200, 83, 0.08); color: #00c853; }
.card-icon-video { background: rgba(255, 152, 0, 0.08); color: #ff9800; }
.card-icon-zip { background: rgba(121, 85, 72, 0.08); color: #795548; }
.card-icon-other { background: rgba(158, 158, 158, 0.08); color: #757575; }

.card-name {
  display: block;
  width: 100%;
  font-size: 13px;
  font-weight: 500;
  color: #1a3a5a;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.card-meta {
  font-size: 11px;
  color: #b0c8e0;
}

/* ====== 占位区域（其他 Tab）====== */
.placeholder-area {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.placeholder-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  padding: 48px 64px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 24px;
  border: 1px solid rgba(0, 102, 255, 0.08);
  box-shadow: 0 8px 24px rgba(0, 102, 255, 0.04);
}

.placeholder-icon {
  width: 80px;
  height: 80px;
  opacity: 0.5;
}

.ph-svg {
  width: 100%;
  height: 100%;
}

.placeholder-text {
  font-size: 16px;
  color: #7a9aba;
  margin: 0;
}

/* ====== 作业列表 ====== */
.homework-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.homework-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  background: rgba(255, 255, 255, 0.75);
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 2px 8px rgba(0, 102, 255, 0.03);
  transition: all 0.2s ease;
}

.homework-item:hover {
  background: rgba(255, 255, 255, 0.95);
  border-color: rgba(0, 102, 255, 0.12);
  box-shadow: 0 6px 20px rgba(0, 102, 255, 0.07);
  transform: translateY(-2px);
}

.homework-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.homework-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a3a5a;
  line-height: 1.4;
}

.homework-deadline {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #7a9aba;
}

.deadline-icon {
  flex-shrink: 0;
}

.homework-action {
  flex-shrink: 0;
  margin-left: 24px;
}

.do-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 10px 22px;
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 14px rgba(0, 102, 255, 0.25);
}

.do-btn:hover {
  box-shadow: 0 6px 24px rgba(0, 102, 255, 0.4);
  transform: translateY(-1px);
}

.do-btn:active {
  transform: translateY(0);
}

.btn-arrow {
  transition: transform 0.2s ease;
}

.do-btn:hover .btn-arrow {
  transform: translateX(3px);
}

.review-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 10px 22px;
  background: rgba(255, 255, 255, 0.75);
  color: #4a6a8a;
  border: 1px solid rgba(0, 102, 255, 0.12);
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.3s ease;
}

.review-btn:hover {
  background: rgba(255, 255, 255, 0.95);
  border-color: rgba(0, 102, 255, 0.25);
  color: #0066ff;
  transform: translateY(-1px);
}

.grade-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 10px 22px;
  background: linear-gradient(135deg, #00c853 0%, #00e676 100%);
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 14px rgba(0, 200, 83, 0.25);
}

.grade-btn:hover {
  box-shadow: 0 6px 24px rgba(0, 200, 83, 0.40);
  transform: translateY(-1px);
}

.deadline-tag {
  display: inline-flex;
  align-items: center;
  padding: 6px 16px;
  background: rgba(160, 170, 190, 0.12);
  color: #9aabbe;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
}

.exam-status-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  line-height: 22px;
  white-space: nowrap;
}

/* ====== AI 助教 ====== */
.ai-section {
  height: calc(100vh - 64px - 64px - 32px);
  margin: -32px -40px;
  display: flex;
}

@media (max-width: 768px) {
  .ai-section {
    margin: -24px -20px;
    height: calc(100vh - 64px - 48px);
  }
}
</style>
