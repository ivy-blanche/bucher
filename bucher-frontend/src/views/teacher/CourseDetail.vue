<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import TeacherLayout from '@/layouts/TeacherLayout.vue'
import { createClass, uploadMaterial, getMaterialList, deleteCourseMaterial, downloadMaterial, batchDownloadMaterials, type ClassInfo, type MaterialInfo } from '@/api/course'

const route = useRoute()
const router = useRouter()

const courseId = computed(() => route.params.id as string)

// ==================== 课程基本信息 ====================
const course = ref({
  name: route.query.name as string || '加载中...',
  courseCode: route.query.courseCode as string || '',
  semester: route.query.semester as string || '',
  description: '',
  teacherName: '我',
  classCount: 0,
  studentCount: 0,
  materialCount: 0,
})

// ==================== Tab 系统 ====================
const tabs = [
  { key: 'classes', label: '班级管理' },
  { key: 'materials', label: '课程资料' },
  { key: 'members', label: '课程成员' },
  { key: 'settings', label: '课程信息' },
]
const activeTab = ref('classes')

// ==================== 班级管理 ====================
interface CourseClass {
  id: string
  name: string
  classCode: string
  studentCount: number
  createTime: string
}

const classes = ref<CourseClass[]>([])

// 创建班级弹窗
const showCreateClass = ref(false)
const newClassName = ref('')
const submittingClass = ref(false)
// 班级码展示
const showCodeDialog = ref(false)
const createdClassCode = ref('')
const createdClassName = ref('')

function openCreateClass() {
  newClassName.value = ''
  submittingClass.value = false
  showCreateClass.value = true
}

function closeCreateClass() {
  showCreateClass.value = false
  newClassName.value = ''
}

async function handleCreateClass() {
  if (!newClassName.value.trim()) {
    ElMessage.warning('请输入班级名称')
    return
  }
  submittingClass.value = true
  try {
    const res: ClassInfo = await createClass({
      courseId: courseId.value,
      name: newClassName.value.trim(),
    })
    createdClassName.value = res.name
    createdClassCode.value = res.inviteCode
    closeCreateClass()
    classes.value.unshift({
      id: res.id,
      name: res.name,
      classCode: res.inviteCode,
      studentCount: res.studentCount,
      createTime: res.createTime.slice(0, 10),
    })
    course.value.classCount = classes.value.length
    showCodeDialog.value = true
  } catch {
    // 错误已由拦截器统一处理
  } finally {
    submittingClass.value = false
  }
}

function closeCodeDialog() {
  showCodeDialog.value = false
  createdClassCode.value = ''
  createdClassName.value = ''
}

function copyClassCode(code: string) {
  navigator.clipboard.writeText(code).then(() => {
    ElMessage.success('班级码已复制到剪贴板')
  }).catch(() => {
    // 降级方案
    const ta = document.createElement('textarea')
    ta.value = code
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    ElMessage.success('班级码已复制到剪贴板')
  })
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

// 上传资料
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

function triggerUpload() {
  fileInput.value?.click()
}

async function handleFileSelect(e: Event) {
  const files = (e.target as HTMLInputElement).files
  if (!files || files.length === 0) return
  const file = files[0]
  uploading.value = true
  try {
    const res: MaterialInfo = await uploadMaterial(courseId.value, file)
    materials.value.unshift({
      id: res.id,
      courseId: res.courseId,
      fileName: res.fileName,
      fileSize: res.fileSize,
      fileType: res.fileType,
      fileExt: res.fileExt,
      duration: res.duration,
      createTime: res.createTime,
    })
    course.value.materialCount = materials.value.length
    ElMessage.success('资料上传成功')
  } catch {
    // 错误已由拦截器统一处理（不支持的文件类型、文件大小超限等）
  } finally {
    uploading.value = false
  }
  // 重置 input 以允许重复选择同文件
  ;(e.target as HTMLInputElement).value = ''
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

async function deleteMaterial(id: number) {
  try {
    await deleteCourseMaterial(id)
    materials.value = materials.value.filter(m => m.id !== id)
    selectedIds.value = selectedIds.value.filter(sid => sid !== id)
    course.value.materialCount = materials.value.length
    ElMessage.success('资料已删除')
  } catch {
    // 错误已由拦截器统一处理
  }
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

// ==================== 课程成员 ====================
interface ClassMemberGroup {
  className: string
  classCode: string
  students: { userNo: string; name: string }[]
}

const memberGroups = ref<ClassMemberGroup[]>([])

// ==================== 课程信息编辑 ====================
const editing = ref(false)
const editForm = ref({ name: '', semester: '', description: '' })

function startEdit() {
  editForm.value = {
    name: course.value.name,
    semester: course.value.semester,
    description: course.value.description,
  }
  editing.value = true
}

function cancelEdit() {
  editing.value = false
}

function saveEdit() {
  if (!editForm.value.name.trim()) {
    ElMessage.warning('课程名称不能为空')
    return
  }
  if (!editForm.value.semester.trim()) {
    ElMessage.warning('开设学期不能为空')
    return
  }
  course.value.name = editForm.value.name.trim()
  course.value.semester = editForm.value.semester.trim()
  course.value.description = editForm.value.description.trim()
  editing.value = false
  ElMessage.success('课程信息已更新')
}

function goBack() {
  router.push('/teacher/courses')
}

// ==================== 初始化加载 ====================
onMounted(async () => {
  // 加载资料列表（切换到此页面时重新获取）
  await loadMaterials()
})

const materialsLoading = ref(false)

async function loadMaterials() {
  materialsLoading.value = true
  try {
    const list = await getMaterialList(courseId.value)
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
    course.value.materialCount = materials.value.length
  } catch {
    // 错误已由拦截器统一处理
  } finally {
    materialsLoading.value = false
  }
}
</script>

<template>
  <TeacherLayout>
    <div class="page-container">
      <!-- ====== 页面头部 ====== -->
      <div class="page-header">
        <div class="header-top">
          <button class="back-btn" @click="goBack">
            <svg viewBox="0 0 12 12" width="16" height="16">
              <path d="M7.5 2L3.5 6L7.5 10" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>返回课程列表</span>
          </button>
        </div>
        <div class="header-main">
          <div class="header-info">
            <h1 class="course-title">{{ course.name }}</h1>
            <div class="course-tags">
              <span class="tag tag-code">{{ course.courseCode }}</span>
              <span class="tag tag-semester">{{ course.semester }}</span>
            </div>
          </div>
        </div>
        <!-- 统计卡片 -->
        <div class="stats-row">
          <div class="stat-card">
            <div class="stat-icon stat-icon-classes">
              <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M2 17c0-3 3-5 7-5s7 2 7 5M9 7a3 3 0 1 0 6 0 3 3 0 0 0-6 0Z"/>
                <path d="M14 7a3 3 0 1 0 6 0 3 3 0 0 0-6 0Z"/>
              </svg>
            </div>
            <div class="stat-body">
              <span class="stat-value">{{ course.classCount }}</span>
              <span class="stat-label">班级</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon stat-icon-students">
              <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="7" cy="6" r="3"/>
                <path d="M1 16c0-3 3-5 6-5s6 2 6 5"/>
              </svg>
            </div>
            <div class="stat-body">
              <span class="stat-value">{{ course.studentCount }}</span>
              <span class="stat-label">学生</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon stat-icon-materials">
              <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M4 4h12v2H4zM4 9h12v2H4zM4 14h8v2H4z"/>
              </svg>
            </div>
            <div class="stat-body">
              <span class="stat-value">{{ course.materialCount }}</span>
              <span class="stat-label">资料</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ====== 标签页导航 ====== -->
      <div class="tab-bar">
        <div
          v-for="tab in tabs"
          :key="tab.key"
          class="tab-item"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </div>
      </div>

      <!-- ====== 内容区域 ====== -->
      <div class="tab-content">

        <!-- ==================== 班级管理 ==================== -->
        <div v-if="activeTab === 'classes'" class="section">
          <div class="section-header">
            <h2 class="section-title">班级列表</h2>
            <button class="primary-btn" @click="openCreateClass">
              <svg viewBox="0 0 16 16" width="14" height="14">
                <line x1="8" y1="2" x2="8" y2="14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                <line x1="2" y1="8" x2="14" y2="8" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
              <span>创建班级</span>
            </button>
          </div>

          <!-- 空状态 -->
          <div v-if="classes.length === 0" class="empty-section">
            <svg viewBox="0 0 80 80" width="80" height="80" class="empty-icon">
              <rect x="15" y="20" width="50" height="40" rx="4" fill="none" stroke="#b0c8e0" stroke-width="2"/>
              <line x1="15" y1="30" x2="65" y2="30" stroke="#b0c8e0" stroke-width="1.5"/>
              <line x1="28" y1="38" x2="55" y2="38" stroke="#b0c8e0" stroke-width="1.5" opacity="0.5"/>
              <line x1="28" y1="46" x2="48" y2="46" stroke="#b0c8e0" stroke-width="1.5" opacity="0.5"/>
            </svg>
            <p class="empty-text">还没有班级，点击上方按钮创建第一个班级</p>
            <p class="empty-hint">创建班级后学生可通过班级码加入</p>
          </div>

          <!-- 班级卡片列表 -->
          <div v-else class="class-list">
            <div v-for="cls in classes" :key="cls.id" class="class-card">
              <div class="class-card-body">
                <div class="class-card-top">
                  <h3 class="class-name">{{ cls.name }}</h3>
                  <span class="class-student-badge">
                    <svg viewBox="0 0 16 16" width="13" height="13">
                      <circle cx="6" cy="5" r="2.5" stroke="currentColor" stroke-width="1.2" fill="none"/>
                      <path d="M1 14c0-2.5 2.5-4.5 5-4.5s5 2 5 4.5" stroke="currentColor" stroke-width="1.2" fill="none"/>
                    </svg>
                    {{ cls.studentCount }} 人
                  </span>
                </div>
                <!-- 班级码展示 -->
                <div class="class-code-section">
                  <span class="code-label">班级码</span>
                  <div class="code-display">
                    <span class="code-text">{{ cls.classCode }}</span>
                    <button class="copy-btn" @click="copyClassCode(cls.classCode)" title="复制班级码">
                      <svg viewBox="0 0 16 16" width="14" height="14">
                        <rect x="5" y="2" width="10" height="11" rx="1.5" stroke="currentColor" stroke-width="1.2" fill="none"/>
                        <rect x="1" y="5" width="10" height="11" rx="1.5" stroke="currentColor" stroke-width="1.2" fill="none"/>
                      </svg>
                    </button>
                  </div>
                </div>
                <div class="class-meta">
                  <span class="class-time">创建于 {{ cls.createTime }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 班级管理附加说明 -->
          <div class="info-banner">
            <svg viewBox="0 0 20 20" width="18" height="18" class="info-icon">
              <circle cx="10" cy="10" r="8" stroke="currentColor" stroke-width="1.3" fill="none"/>
              <line x1="10" y1="7" x2="10" y2="13" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
              <line x1="7" y1="10" x2="13" y2="10" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
            </svg>
            <span>学生可通过班级码加入课程班级，每个班级码唯一且不可更改</span>
          </div>
        </div>

        <!-- ==================== 课程资料 ==================== -->
        <div v-if="activeTab === 'materials'" class="section">
          <div class="section-header">
            <h2 class="section-title">课程资料</h2>
            <div class="header-actions">
              <span class="materials-tip">资料与课程绑定，所有班级共享</span>
              <button
                v-if="selectedIds.length > 0"
                class="primary-btn primary-btn-secondary"
                :disabled="batchDownloading"
                @click="handleBatchDownload"
              >
                <svg viewBox="0 0 16 16" width="14" height="14">
                  <path d="M8 12V3M4 7l4 5 4-5" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                  <line x1="2" y1="13" x2="14" y2="13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
                <span>{{ batchDownloading ? '下载中...' : `批量下载 (${selectedIds.length})` }}</span>
              </button>
              <button class="primary-btn" :disabled="uploading" @click="triggerUpload">
                <svg viewBox="0 0 16 16" width="14" height="14">
                  <path d="M8 2v9M4 7l4-5 4 5" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                  <line x1="2" y1="12" x2="14" y2="12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
                <span>{{ uploading ? '上传中...' : '上传资料' }}</span>
              </button>
            </div>
            <input
              ref="fileInput"
              type="file"
              style="display:none"
              @change="handleFileSelect"
            />
          </div>

          <!-- 加载中 -->
          <div v-if="materialsLoading" class="empty-section">
            <p class="empty-text" style="padding: 40px 0;">加载中...</p>
          </div>

          <!-- 空状态 -->
          <div v-else-if="materials.length === 0" class="empty-section">
            <svg viewBox="0 0 80 80" width="80" height="80" class="empty-icon">
              <path d="M20 65V15h25l15 15v35z" fill="none" stroke="#b0c8e0" stroke-width="2"/>
              <line x1="20" y1="50" x2="60" y2="50" stroke="#b0c8e0" stroke-width="1.5"/>
              <line x1="20" y1="58" x2="60" y2="58" stroke="#b0c8e0" stroke-width="1.5"/>
              <path d="M45 15v15h15" fill="none" stroke="#b0c8e0" stroke-width="1.5"/>
            </svg>
            <p class="empty-text">还没有上传资料</p>
            <p class="empty-hint">支持 PDF、Word、PPT、Excel、图片、视频等多种格式</p>
          </div>

          <!-- 资料列表 — 文件夹图标排列 -->
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
                <button class="card-delete" @click="deleteMaterial(mat.id)" title="删除">
                  <svg viewBox="0 0 16 16" width="12" height="12">
                    <line x1="4" y1="4" x2="12" y2="12" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
                    <line x1="12" y1="4" x2="4" y2="12" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
                  </svg>
                </button>
              </div>
              <div class="card-icon" :class="'card-icon-' + getFileIcon(mat.fileExt)">
                <svg v-if="getFileIcon(mat.fileExt) === 'pdf'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M6 2h10l4 4v16H6z"/>
                  <path d="M16 2v4h4"/>
                  <text x="12" y="18" text-anchor="middle" font-size="9" font-weight="bold" fill="currentColor">PDF</text>
                </svg>
                <svg v-if="getFileIcon(mat.fileExt) === 'office'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M6 2h8l4 4v16H6z"/>
                  <path d="M14 2v4h4"/>
                  <line x1="8" y1="12" x2="16" y2="12"/>
                  <line x1="8" y1="16" x2="14" y2="16"/>
                </svg>
                <svg v-if="getFileIcon(mat.fileExt) === 'code'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M6 2h10l4 4v16H6z"/>
                  <path d="M16 2v4h4"/>
                  <line x1="9" y1="13" x2="11" y2="15"/>
                  <line x1="11" y1="13" x2="9" y2="15"/>
                  <line x1="14" y1="13" x2="16" y2="15"/>
                </svg>
                <svg v-if="getFileIcon(mat.fileExt) === 'image'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="4" y="4" width="16" height="16" rx="2"/>
                  <circle cx="9" cy="9" r="1.5"/>
                  <path d="M4 16l5-5 4 4 3-3 4 4"/>
                </svg>
                <svg v-if="getFileIcon(mat.fileExt) === 'video'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="3" y="5" width="14" height="14" rx="2"/>
                  <polygon points="19,9 22,12 19,15" stroke-linejoin="round"/>
                </svg>
                <svg v-if="getFileIcon(mat.fileExt) === 'zip'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M6 2h10l4 4v16H6z"/>
                  <path d="M16 2v4h4"/>
                  <line x1="10" y1="10" x2="14" y2="10"/>
                  <line x1="12" y1="10" x2="12" y2="18"/>
                </svg>
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

        <!-- ==================== 课程成员 ==================== -->
        <div v-if="activeTab === 'members'" class="section">
          <div class="section-header">
            <h2 class="section-title">课程成员</h2>
          </div>

          <!-- 先选班级再展示学生 -->
          <div v-if="classes.length === 0" class="empty-section">
            <svg viewBox="0 0 80 80" width="80" height="80" class="empty-icon">
              <circle cx="40" cy="25" r="8" fill="none" stroke="#b0c8e0" stroke-width="2"/>
              <path d="M20 60c0-10 10-16 20-16s20 6 20 16" fill="none" stroke="#b0c8e0" stroke-width="2"/>
            </svg>
            <p class="empty-text">暂无班级，请先在「班级管理」中创建班级</p>
          </div>

          <!-- 按班级分组展示 -->
          <div v-else-if="memberGroups.length === 0">
            <!-- 有班级但无成员 -->
            <div v-for="cls in classes" :key="cls.id" class="member-group">
              <div class="member-group-header">
                <div class="group-title">
                  <svg viewBox="0 0 16 16" width="16" height="16">
                    <path d="M2 14c0-3 2.5-5 6-5s6 2 6 5M8 4a3 3 0 1 1 0 6 3 3 0 0 1 0-6Z" stroke="currentColor" stroke-width="1.3" fill="none"/>
                  </svg>
                  <span>{{ cls.name }}</span>
                  <span class="group-code">码：{{ cls.classCode }}</span>
                </div>
                <span class="group-count">{{ cls.studentCount }} 人</span>
              </div>
              <div class="member-empty-tip">暂无学生加入</div>
            </div>
          </div>

          <!-- 有成员数据时 -->
          <div v-else>
            <div v-for="group in memberGroups" :key="group.classCode" class="member-group">
              <div class="member-group-header">
                <div class="group-title">
                  <svg viewBox="0 0 16 16" width="16" height="16">
                    <path d="M2 14c0-3 2.5-5 6-5s6 2 6 5M8 4a3 3 0 1 1 0 6 3 3 0 0 1 0-6Z" stroke="currentColor" stroke-width="1.3" fill="none"/>
                  </svg>
                  <span>{{ group.className }}</span>
                  <span class="group-code">码：{{ group.classCode }}</span>
                </div>
                <span class="group-count">{{ group.students.length }} 人</span>
              </div>
              <div class="member-table">
                <div class="member-table-head">
                  <span class="col col-no">学号</span>
                  <span class="col col-name">姓名</span>
                </div>
                <div
                  v-for="(stu, idx) in group.students"
                  :key="stu.userNo"
                  class="member-row"
                  :class="{ even: idx % 2 === 0 }"
                >
                  <span class="col col-no">{{ stu.userNo }}</span>
                  <span class="col col-name">{{ stu.name }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- ==================== 课程信息 ==================== -->
        <div v-if="activeTab === 'settings'" class="section">
          <div class="section-header">
            <h2 class="section-title">课程信息</h2>
            <button v-if="!editing" class="primary-btn" @click="startEdit">
              <svg viewBox="0 0 16 16" width="14" height="14">
                <path d="M11 2l3 3-9 9H2v-3z" stroke="currentColor" stroke-width="1.3" fill="none" stroke-linejoin="round"/>
              </svg>
              <span>编辑</span>
            </button>
          </div>

          <!-- 查看模式 -->
          <div v-if="!editing" class="info-card">
            <div class="info-row">
              <span class="info-label">课程名称</span>
              <span class="info-value">{{ course.name }}</span>
            </div>
            <div class="info-divider"></div>
            <div class="info-row">
              <span class="info-label">课程编码</span>
              <span class="info-value code-value">{{ course.courseCode }}</span>
            </div>
            <div class="info-divider"></div>
            <div class="info-row">
              <span class="info-label">开设学期</span>
              <span class="info-value">{{ course.semester }}</span>
            </div>
            <div class="info-divider"></div>
            <div class="info-row">
              <span class="info-label">授课教师</span>
              <span class="info-value">{{ course.teacherName }}</span>
            </div>
            <div class="info-divider"></div>
            <div class="info-row info-row-desc">
              <span class="info-label">课程描述</span>
              <span class="info-value info-desc" :class="{ empty: !course.description }">
                {{ course.description || '暂无描述' }}
              </span>
            </div>
          </div>

          <!-- 编辑模式 -->
          <div v-else class="info-card">
            <div class="form-group">
              <label class="form-label">课程名称 <span class="required">*</span></label>
              <input v-model="editForm.name" class="form-input" placeholder="课程名称" maxlength="100" />
            </div>
            <div class="form-group">
              <label class="form-label">开设学期 <span class="required">*</span></label>
              <input v-model="editForm.semester" class="form-input" placeholder="例：2025-2026-2" />
            </div>
            <div class="form-group">
              <label class="form-label">课程描述</label>
              <textarea v-model="editForm.description" class="form-input form-textarea" placeholder="课程描述" rows="3" maxlength="500"></textarea>
            </div>
            <div class="edit-actions">
              <button class="dialog-btn dialog-btn-cancel" @click="cancelEdit">取消</button>
              <button class="dialog-btn dialog-btn-confirm" @click="saveEdit">保存</button>
            </div>
          </div>
        </div>

      </div>
    </div>

    <!-- ==================== 创建班级弹窗 ==================== -->
    <teleport to="body">
      <transition name="dialog">
        <div v-if="showCreateClass" class="dialog-overlay" @click.self="closeCreateClass">
          <div class="dialog-card">
            <h3 class="dialog-title">创建班级</h3>
            <div class="form-group">
              <label class="form-label">
                班级名称 <span class="required">*</span>
              </label>
              <input
                v-model="newClassName"
                type="text"
                class="form-input"
                placeholder="例：2025级计算机1班"
                maxlength="50"
                @keyup.enter="handleCreateClass"
              />
            </div>
            <div class="dialog-actions">
              <button class="dialog-btn dialog-btn-cancel" @click="closeCreateClass">取消</button>
              <button
                class="dialog-btn dialog-btn-confirm"
                :disabled="submittingClass || !newClassName.trim()"
                @click="handleCreateClass"
              >
                {{ submittingClass ? '创建中...' : '确认创建' }}
              </button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- ==================== 班级码展示弹窗 ==================== -->
    <teleport to="body">
      <transition name="dialog">
        <div v-if="showCodeDialog" class="dialog-overlay" @click.self="closeCodeDialog">
          <div class="dialog-card code-dialog-card">
            <div class="code-dialog-icon">
              <svg viewBox="0 0 48 48" width="48" height="48">
                <defs>
                  <linearGradient id="codeGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" style="stop-color:#00d4ff" />
                    <stop offset="100%" style="stop-color:#0066ff" />
                  </linearGradient>
                </defs>
                <rect x="6" y="12" width="36" height="24" rx="4" fill="none" stroke="url(#codeGrad)" stroke-width="1.5"/>
                <line x1="6" y1="20" x2="42" y2="20" stroke="url(#codeGrad)" stroke-width="1" opacity="0.3"/>
                <line x1="14" y1="27" x2="34" y2="27" stroke="url(#codeGrad)" stroke-width="1" opacity="0.3"/>
                <line x1="18" y1="33" x2="30" y2="33" stroke="url(#codeGrad)" stroke-width="1" opacity="0.3"/>
              </svg>
            </div>
            <h3 class="dialog-title code-dialog-title">班级创建成功！</h3>
            <p class="code-dialog-desc">「{{ createdClassName }}」的班级码如下，请将班级码告知学生</p>
            <div class="code-display-large">
              <span class="code-text-large">{{ createdClassCode }}</span>
              <button class="copy-btn-large" @click="copyClassCode(createdClassCode)">
                <svg viewBox="0 0 16 16" width="16" height="16">
                  <rect x="5" y="2" width="10" height="11" rx="1.5" stroke="currentColor" stroke-width="1.2" fill="none"/>
                  <rect x="1" y="5" width="10" height="11" rx="1.5" stroke="currentColor" stroke-width="1.2" fill="none"/>
                </svg>
                <span>复制</span>
              </button>
            </div>
            <p class="code-dialog-hint">学生可在「我的课程」中输入班级码加入此班级</p>
            <div class="dialog-actions">
              <button class="dialog-btn dialog-btn-confirm" @click="closeCodeDialog">我知道了</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </TeacherLayout>
</template>

<style scoped>
.page-container {
  max-width: 1000px;
  margin: 0 auto;
}

/* ====== 页面头部 ====== */
.page-header {
  margin-bottom: 24px;
}

.header-top {
  margin-bottom: 12px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: none;
  background: rgba(0, 102, 255, 0.06);
  border-radius: 8px;
  color: #5a7a9a;
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
}

.back-btn:hover {
  background: rgba(0, 102, 255, 0.1);
  color: #0066ff;
}

.header-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.course-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a3a5a;
  margin: 0 0 10px;
}

.course-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
}

.tag-code {
  background: rgba(0, 102, 255, 0.08);
  color: #0066ff;
}

.tag-semester {
  background: rgba(0, 212, 255, 0.08);
  color: #0099cc;
}

/* 统计卡片 */
.stats-row {
  display: flex;
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.75);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 4px 16px rgba(0, 102, 255, 0.04);
  flex: 1;
  min-width: 0;
}

.stat-icon {
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  flex-shrink: 0;
}

.stat-icon svg {
  width: 22px;
  height: 22px;
}

.stat-icon-classes {
  background: linear-gradient(135deg, rgba(0, 102, 255, 0.12) 0%, rgba(0, 212, 255, 0.08) 100%);
  color: #0066ff;
}

.stat-icon-students {
  background: linear-gradient(135deg, rgba(0, 200, 83, 0.12) 0%, rgba(0, 230, 118, 0.08) 100%);
  color: #00c853;
}

.stat-icon-materials {
  background: linear-gradient(135deg, rgba(255, 152, 0, 0.12) 0%, rgba(255, 193, 7, 0.08) 100%);
  color: #ff9800;
}

.stat-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #1a3a5a;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #7a9aba;
  font-weight: 500;
}

/* ====== 标签页导航 ====== */
.tab-bar {
  display: flex;
  gap: 4px;
  margin-bottom: 24px;
  padding: 4px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 10px 16px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: #5a7a9a;
  cursor: pointer;
  transition: all 0.25s ease;
}

.tab-item:hover {
  color: #0066ff;
  background: rgba(0, 102, 255, 0.04);
}

.tab-item.active {
  color: #fff;
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  box-shadow: 0 4px 12px rgba(0, 102, 255, 0.25);
}

/* ====== 通用区块 ====== */
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 主按钮 */
.primary-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.primary-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(0, 102, 255, 0.25);
}

.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.primary-btn-secondary {
  background: rgba(255, 255, 255, 0.85);
  color: #0066ff;
  border: 1px solid rgba(0, 102, 255, 0.2);
}

.primary-btn-secondary:hover:not(:disabled) {
  background: #fff;
  border-color: #0066ff;
  box-shadow: 0 4px 14px rgba(0, 102, 255, 0.12);
}

/* 空状态 */
.empty-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 64px 20px;
  text-align: center;
}

.empty-icon {
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-text {
  font-size: 15px;
  color: #7a9aba;
  margin: 0 0 6px;
  font-weight: 500;
}

.empty-hint {
  font-size: 13px;
  color: #b0c8e0;
  margin: 0;
}

/* ====== 班级管理 ====== */
.class-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.class-card {
  padding: 20px;
  background: rgba(255, 255, 255, 0.75);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 4px 16px rgba(0, 102, 255, 0.04);
  transition: all 0.3s ease;
}

.class-card:hover {
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(0, 102, 255, 0.12);
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(0, 102, 255, 0.08);
}

.class-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.class-name {
  font-size: 16px;
  font-weight: 600;
  color: #1a3a5a;
  margin: 0;
}

.class-student-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  background: rgba(0, 200, 83, 0.08);
  color: #00a844;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.class-code-section {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 10px 14px;
  background: rgba(0, 102, 255, 0.04);
  border-radius: 10px;
  border: 1px dashed rgba(0, 102, 255, 0.15);
}

.code-label {
  font-size: 12px;
  color: #7a9aba;
  font-weight: 500;
  flex-shrink: 0;
}

.code-display {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
}

.code-text {
  font-size: 18px;
  font-weight: 700;
  color: #0066ff;
  letter-spacing: 3px;
  font-family: 'Courier New', monospace;
}

.copy-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  background: rgba(0, 102, 255, 0.1);
  border-radius: 8px;
  color: #0066ff;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.copy-btn:hover {
  background: rgba(0, 102, 255, 0.2);
}

.class-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.class-time {
  font-size: 12px;
  color: #b0c8e0;
}

.info-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 12px 16px;
  background: rgba(0, 102, 255, 0.04);
  border-radius: 10px;
  border: 1px solid rgba(0, 102, 255, 0.08);
  font-size: 13px;
  color: #7a9aba;
}

.info-icon {
  flex-shrink: 0;
  color: #0066ff;
  opacity: 0.6;
}

/* ====== 资料列表 — 文件夹网格 ====== */
.materials-tip {
  font-size: 13px;
  color: #b0c8e0;
}

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

.card-delete {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: none;
  background: transparent;
  border-radius: 6px;
  color: #c0d0e0;
  cursor: pointer;
  transition: all 0.2s ease;
}

.card-delete:hover {
  background: rgba(255, 77, 79, 0.1);
  color: #ff4d4f;
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

/* ====== 课程成员 ====== */
.member-group {
  margin-bottom: 20px;
}

.member-group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 12px 12px 0 0;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-bottom: none;
}

.group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1a3a5a;
}

.group-title svg {
  color: #0066ff;
}

.group-code {
  font-size: 12px;
  font-weight: 400;
  color: #b0c8e0;
}

.group-count {
  font-size: 13px;
  color: #7a9aba;
  font-weight: 500;
}

.member-table {
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 0 0 12px 12px;
  overflow: hidden;
}

.member-table-head {
  display: flex;
  padding: 10px 16px;
  background: rgba(0, 102, 255, 0.04);
  font-size: 12px;
  font-weight: 600;
  color: #7a9aba;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.member-row {
  display: flex;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.6);
  font-size: 14px;
  color: #3a5a7a;
  transition: background 0.2s ease;
}

.member-row.even {
  background: rgba(255, 255, 255, 0.3);
}

.member-row:hover {
  background: rgba(0, 102, 255, 0.04);
}

.col { flex: 1; }
.col-no { flex: 0 0 120px; }
.col-name { flex: 1; }

.member-empty-tip {
  padding: 24px 16px;
  text-align: center;
  font-size: 14px;
  color: #b0c8e0;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-top: none;
  border-radius: 0 0 12px 12px;
}

/* ====== 课程信息 ====== */
.info-card {
  padding: 4px 0;
  background: rgba(255, 255, 255, 0.75);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 4px 16px rgba(0, 102, 255, 0.04);
}

.info-row {
  display: flex;
  padding: 16px 24px;
  gap: 24px;
}

.info-row-desc {
  align-items: flex-start;
}

.info-label {
  flex: 0 0 100px;
  font-size: 14px;
  font-weight: 500;
  color: #7a9aba;
}

.info-value {
  flex: 1;
  font-size: 15px;
  color: #1a3a5a;
  font-weight: 500;
}

.info-value.code-value {
  font-family: 'Courier New', monospace;
  color: #0066ff;
  font-weight: 600;
}

.info-desc {
  line-height: 1.6;
  font-weight: 400;
}

.info-desc.empty {
  color: #b0c8e0;
  font-style: italic;
}

.info-divider {
  height: 1px;
  margin: 0 24px;
  background: linear-gradient(90deg, transparent, rgba(0, 102, 255, 0.08), transparent);
}

/* 编辑模式样式 */
.form-group {
  padding: 16px 24px 4px;
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
  padding: 11px 16px;
  background: rgba(0, 102, 255, 0.03);
  border: 1px solid rgba(0, 102, 255, 0.15);
  border-radius: 10px;
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
  resize: vertical;
  min-height: 80px;
}

.edit-actions {
  display: flex;
  gap: 12px;
  padding: 16px 24px 20px;
}

.dialog-btn {
  flex: 1;
  padding: 11px 0;
  border: none;
  border-radius: 10px;
  font-size: 14px;
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

/* ====== 通用弹窗 ====== */
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
  width: 440px;
  max-width: 90vw;
  max-height: 90vh;
  overflow-y: auto;
  padding: 32px 28px 24px;
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

.dialog-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

/* 班级码展示弹窗 */
.code-dialog-card {
  text-align: center;
}

.code-dialog-icon {
  margin-bottom: 16px;
  opacity: 0.8;
}

.code-dialog-title {
  margin-bottom: 12px;
}

.code-dialog-desc {
  font-size: 14px;
  color: #5a7a9a;
  margin: 0 0 20px;
  line-height: 1.5;
}

.code-display-large {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 18px 24px;
  background: rgba(0, 102, 255, 0.04);
  border-radius: 14px;
  border: 2px dashed rgba(0, 102, 255, 0.2);
  margin-bottom: 16px;
}

.code-text-large {
  font-size: 30px;
  font-weight: 800;
  color: #0066ff;
  letter-spacing: 6px;
  font-family: 'Courier New', monospace;
}

.copy-btn-large {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 8px 16px;
  border: none;
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  border-radius: 10px;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
}

.copy-btn-large:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(0, 102, 255, 0.3);
}

.code-dialog-hint {
  font-size: 12px;
  color: #b0c8e0;
  margin: 0 0 4px;
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

/* ====== 响应式 ====== */
@media (max-width: 768px) {
  .stats-row {
    flex-direction: column;
  }

  .course-title {
    font-size: 22px;
  }

  .class-list {
    grid-template-columns: 1fr;
  }

  .header-actions {
    flex-direction: column;
    align-items: flex-end;
  }
}

@media (max-width: 480px) {
  .course-title {
    font-size: 20px;
  }

  .info-row {
    flex-direction: column;
    padding: 12px 16px;
    gap: 4px;
  }

  .info-label {
    flex: none;
  }

  .code-display-large {
    flex-direction: column;
    gap: 12px;
  }

  .code-text-large {
    font-size: 24px;
    letter-spacing: 4px;
  }
}
</style>
