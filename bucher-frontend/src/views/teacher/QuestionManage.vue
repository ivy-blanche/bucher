<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import TeacherLayout from '@/layouts/TeacherLayout.vue'
import { getQuestionList, batchSaveQuestions, uploadImportFile, executeImport, downloadImportTemplate } from '@/api/question'
import type { BatchSaveRequest, ImportExecuteResult } from '@/api/question'

const route = useRoute()
const router = useRouter()
const bankId = route.params.bankId as string
const bankName = (route.query.name as string) || '题库'

/* ---------- 题型定义 ---------- */
const questionTypes = [
  { value: '', label: '全部题型' },
  { value: 1, label: '单选题' },
  { value: 2, label: '多选题' },
  { value: 3, label: '填空题' },
  { value: 4, label: '简答题' },
  { value: 5, label: '编程题' },
]

const typeLabelMap: Record<number, string> = {
  1: '单选题',
  2: '多选题',
  3: '填空题',
  4: '简答题',
  5: '编程题',
}

const typeFilter = ref<number | string>('')
const loading = ref(false)

/* ---------- 题目数据 ---------- */
interface QuestionItem {
  id: string
  contentPreview: string
  type: number
  typeName: string
  difficulty: number
  createTime: string
}

const questionList = ref<QuestionItem[]>([])

async function loadQuestions() {
  loading.value = true
  try {
    questionList.value = await getQuestionList(bankId)
  } catch {
    ElMessage.error('加载题目列表失败')
    questionList.value = []
  } finally {
    loading.value = false
  }
}

/* ---------- 批量选择 ---------- */
const selectedIds = ref<Set<string>>(new Set())

const isAllSelected = computed(() => {
  return filteredList.value.length > 0 && filteredList.value.every((q) => selectedIds.value.has(q.id))
})

const isIndeterminate = computed(() => {
  const checked = filteredList.value.filter((q) => selectedIds.value.has(q.id))
  return checked.length > 0 && checked.length < filteredList.value.length
})

function toggleSelectAll() {
  if (isAllSelected.value) {
    selectedIds.value = new Set()
  } else {
    selectedIds.value = new Set(filteredList.value.map((q) => q.id))
  }
}

function toggleSelect(id: number) {
  const next = new Set(selectedIds.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  selectedIds.value = next
}

const filteredList = computed(() => {
  if (!typeFilter.value) return questionList.value
  return questionList.value.filter((q) => q.type === typeFilter.value)
})

function getTypeLabel(type: number): string {
  return typeLabelMap[type] || `未知(${type})`
}

/* ---------- 弹窗状态 ---------- */
const showImportDialog = ref(false)
const submitting = ref(false)

/* ---------- 导入流程状态 ---------- */
const importStep = ref<'upload' | 'preview' | 'result'>('upload')
const selectedFile = ref<File | null>(null)
const importFileKey = ref('')
const importRowCount = ref(0)
const importFileName = ref('')
const importResult = ref<ImportExecuteResult | null>(null)
const uploading = ref(false)
const importing = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

function goBack() {
  router.push('/teacher/question-bank')
}

function openImportDialog() {
  importStep.value = 'upload'
  selectedFile.value = null
  importFileKey.value = ''
  importRowCount.value = 0
  importFileName.value = ''
  importResult.value = null
  showImportDialog.value = true
}

function closeImportDialog() {
  showImportDialog.value = false
}

/* ---------- 导入流程 ---------- */

async function handleDownloadTemplate() {
  try {
    await downloadImportTemplate()
  } catch {
    ElMessage.error('模板下载失败')
  }
}

function handleSelectFile() {
  fileInput.value?.click()
}

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  selectedFile.value = file
  uploading.value = true

  try {
    const result = await uploadImportFile(file)
    importFileKey.value = result.fileKey
    importRowCount.value = result.rowCount
    importFileName.value = result.originalName
    importStep.value = 'preview'
    ElMessage.success(`文件解析成功，共 ${result.rowCount} 条数据`)
  } catch {
    selectedFile.value = null
    importStep.value = 'upload'
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function handleExecuteImport() {
  if (!importFileKey.value) return

  importing.value = true
  try {
    const result = await executeImport({
      groupId: bankId,
      fileKey: importFileKey.value,
    })
    importResult.value = result
    importStep.value = 'result'
    if (result.failCount === 0) {
      ElMessage.success(`成功导入 ${result.successCount} 道题目`)
    } else {
      ElMessage.warning(`导入完成：成功 ${result.successCount} 条，失败 ${result.failCount} 条`)
    }
    await loadQuestions()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    importing.value = false
  }
}

/* ---------- 操作 ---------- */

async function handleDelete(question: QuestionItem) {
  try {
    await ElMessageBox.confirm('确认删除该题目吗？删除后不可恢复。', '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    submitting.value = true
    await batchSaveQuestions({
      groupId: bankId,
      questions: [{
        id: question.id,
        type: question.type,
        content: '',
        answer: '',
        analysis: '',
        options: [],
        deleted: true,
      }],
    })
    ElMessage.success('删除成功')
    selectedIds.value.delete(question.id)
    await loadQuestions()
  } catch {
    // 用户取消或删除失败
  } finally {
    submitting.value = false
  }
}

async function handleBatchDelete() {
  const count = selectedIds.value.size
  if (count === 0) return
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${count} 道题目吗？删除后不可恢复。`, '批量删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    submitting.value = true
    const payload: BatchSaveRequest = {
      groupId: bankId,
      questions: [...selectedIds.value].map((id) => ({
        id,
        type: 1,
        content: '',
        answer: '',
        analysis: '',
        options: [],
        deleted: true,
      })),
    }
    await batchSaveQuestions(payload)
    ElMessage.success(`成功删除 ${count} 道题目`)
    selectedIds.value = new Set()
    await loadQuestions()
  } catch {
    // 用户取消或删除失败
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadQuestions()
})
</script>

<template>
  <TeacherLayout>
    <div class="page-container">
      <!-- 顶部导航 + 操作 -->
      <div class="page-header">
        <div class="header-left">
          <button class="back-btn" @click="goBack">
            <svg viewBox="0 0 20 20" width="18" height="18">
              <polyline points="12,4 6,10 12,16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
            <span>返回题库</span>
          </button>
          <h1 class="page-title">{{ bankName }} — 题目管理</h1>
        </div>
        <div class="header-actions">
          <button class="action-btn secondary-btn" @click="openImportDialog">
            <svg viewBox="0 0 20 20" width="16" height="16" class="btn-icon">
              <path d="M10 3v10m0 0l-3-3m3 3l3-3" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
              <path d="M3 15v2a2 2 0 002 2h10a2 2 0 002-2v-2" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
            </svg>
            <span>批量导入</span>
          </button>
          <button class="action-btn primary-btn" @click="router.push({ name: 'teacherQuestionEditor', params: { bankId }, query: { name: bankName } })">
            <svg viewBox="0 0 20 20" width="16" height="16" class="btn-icon">
              <line x1="10" y1="3" x2="10" y2="17" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
              <line x1="3" y1="10" x2="17" y2="10" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
            </svg>
            <span>创建题目</span>
          </button>
        </div>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <div class="filter-item">
          <label class="filter-label">题型筛选</label>
          <el-select
            v-model="typeFilter"
            placeholder="全部题型"
            size="large"
            clearable
            class="type-select"
          >
            <el-option
              v-for="opt in questionTypes"
              :key="String(opt.value)"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </div>
        <span class="filter-summary">
          共 <strong>{{ filteredList.length }}</strong> 题
        </span>
      </div>

      <!-- 题目表格 -->
      <div class="table-wrapper">
        <!-- 批量操作栏 -->
        <div v-if="selectedIds.size > 0" class="batch-bar">
          <span class="batch-info">已选 <strong>{{ selectedIds.size }}</strong> 题</span>
          <button class="batch-delete-btn" :disabled="submitting" @click="handleBatchDelete">
            <svg viewBox="0 0 20 20" width="15" height="15">
              <path d="M4 5h12M7 5V4a1 1 0 011-1h4a1 1 0 011 1v1M5 5v11a1 1 0 001 1h8a1 1 0 001-1V5" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
              <line x1="8" y1="9" x2="8" y2="14" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
              <line x1="12" y1="9" x2="12" y2="14" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
            </svg>
            <span>批量删除</span>
          </button>
        </div>

        <div v-if="loading" class="empty-state">
          <p class="empty-text">加载中...</p>
        </div>
        <table v-else class="question-table">
          <thead>
            <tr>
              <th class="col-checkbox">
                <span class="checkbox-wrap" @click="toggleSelectAll">
                  <input type="checkbox" class="checkbox-input" :checked="isAllSelected" />
                  <span class="checkbox-visual" :class="{ checked: isAllSelected, indeterminate: isIndeterminate }" />
                </span>
              </th>
              <th class="col-index">序号</th>
              <th class="col-stem">题干</th>
              <th class="col-type">题目类型</th>
              <th class="col-actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(q, index) in filteredList" :key="q.id" :class="{ 'row-selected': selectedIds.has(q.id) }">
              <td class="col-checkbox">
                <span class="checkbox-wrap" @click="toggleSelect(q.id)">
                  <input type="checkbox" class="checkbox-input" :checked="selectedIds.has(q.id)" />
                  <span class="checkbox-visual" :class="{ checked: selectedIds.has(q.id) }" />
                </span>
              </td>
              <td class="col-index">{{ index + 1 }}</td>
              <td class="col-stem" :title="q.contentPreview">{{ q.contentPreview }}</td>
              <td class="col-type"><span class="type-tag">{{ q.typeName }}</span></td>
              <td class="col-actions">
                <button class="action-link" @click="router.push({ name: 'teacherQuestionEditor', params: { bankId }, query: { name: bankName, selected: q.id } })">编辑</button>
                <button class="action-link action-link-danger" :disabled="submitting" @click="handleDelete(q)">删除</button>
              </td>
            </tr>
            <tr v-if="filteredList.length === 0">
              <td colspan="5" class="empty-cell">暂无题目，点击上方按钮创建第一道题目</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ======== 批量导入弹窗 ======== -->
    <teleport to="body">
      <transition name="dialog">
        <div v-if="showImportDialog" class="dialog-overlay" @click.self="closeImportDialog">
          <div class="dialog-card">
            <h3 class="dialog-title">批量导入题目</h3>
            <p class="dialog-desc">支持 Excel（.xlsx）格式导入，下载模板后按格式填写。</p>

            <!-- Step 1: 上传文件 -->
            <template v-if="importStep === 'upload'">
              <div class="import-template-area">
                <button class="template-download-btn" @click="handleDownloadTemplate">
                  <svg viewBox="0 0 20 20" width="16" height="16">
                    <path d="M10 3v10m0 0l-3-3m3 3l3-3" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                    <path d="M3 15v2a2 2 0 002 2h10a2 2 0 002-2v-2" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                  </svg>
                  <span>下载导入模板</span>
                </button>
              </div>

              <div class="form-group">
                <label class="form-label">上传已填好的文件</label>
                <div class="upload-area" :class="{ 'upload-disabled': uploading }" @click="handleSelectFile">
                  <svg viewBox="0 0 40 40" width="32" height="32" class="upload-icon">
                    <path d="M20 5v20m0 0l-6-6m6 6l6-6" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
                    <path d="M5 28v5a3 3 0 003 3h24a3 3 0 003-3v-5" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" />
                  </svg>
                  <p v-if="uploading" class="upload-text">上传中...</p>
                  <p v-else class="upload-text">点击选择文件</p>
                  <p class="upload-hint">支持 .xlsx 格式</p>
                </div>
                <input ref="fileInput" type="file" accept=".xlsx" style="display:none" @change="handleFileChange" />
              </div>
            </template>

            <!-- Step 2: 预览确认 -->
            <template v-if="importStep === 'preview'">
              <div class="import-preview">
                <div class="preview-icon">
                  <svg viewBox="0 0 40 40" width="36" height="36">
                    <rect x="6" y="3" width="28" height="34" rx="3" fill="none" stroke="currentColor" stroke-width="2" />
                    <line x1="12" y1="14" x2="28" y2="14" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                    <line x1="12" y1="20" x2="28" y2="20" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                    <line x1="12" y1="26" x2="22" y2="26" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                  </svg>
                </div>
                <p class="preview-filename">{{ importFileName }}</p>
                <p class="preview-count">共 <strong>{{ importRowCount }}</strong> 条数据，确认导入？</p>
              </div>
            </template>

            <!-- Step 3: 导入结果 -->
            <template v-if="importStep === 'result' && importResult">
              <div class="import-result">
                <div class="result-icon" :class="importResult.failCount === 0 ? 'result-success' : 'result-warning'">
                  <svg v-if="importResult.failCount === 0" viewBox="0 0 40 40" width="40" height="40">
                    <circle cx="20" cy="20" r="16" fill="none" stroke="currentColor" stroke-width="2.5" />
                    <polyline points="14,20 18,24 26,16" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                  <svg v-else viewBox="0 0 40 40" width="40" height="40">
                    <circle cx="20" cy="20" r="16" fill="none" stroke="currentColor" stroke-width="2.5" />
                    <line x1="15" y1="15" x2="25" y2="25" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" />
                    <line x1="25" y1="15" x2="15" y2="25" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" />
                  </svg>
                </div>
                <p class="result-title">{{ importResult.failCount === 0 ? '导入成功' : '导入完成' }}</p>
                <div class="result-stats">
                  <span class="stat stat-success">成功 {{ importResult.successCount }} 条</span>
                  <span v-if="importResult.failCount > 0" class="stat stat-fail">失败 {{ importResult.failCount }} 条</span>
                  <span class="stat stat-total">共 {{ importResult.totalCount }} 条</span>
                </div>
                <div v-if="importResult.errors.length > 0" class="result-errors">
                  <p class="errors-title">失败详情：</p>
                  <div v-for="err in importResult.errors" :key="err.row" class="error-item">
                    <span class="error-row">第 {{ err.row }} 行</span>
                    <span class="error-reason">{{ err.reason }}</span>
                  </div>
                </div>
              </div>
            </template>

            <div class="dialog-actions">
              <template v-if="importStep === 'upload'">
                <button class="dialog-btn dialog-btn-cancel" @click="closeImportDialog">取消</button>
                <button class="dialog-btn dialog-btn-confirm" disabled>请先上传文件</button>
              </template>
              <template v-if="importStep === 'preview'">
                <button class="dialog-btn dialog-btn-cancel" :disabled="importing" @click="closeImportDialog">取消</button>
                <button class="dialog-btn dialog-btn-confirm" :disabled="importing" @click="handleExecuteImport">
                  {{ importing ? '导入中...' : '确认导入' }}
                </button>
              </template>
              <template v-if="importStep === 'result'">
                <button class="dialog-btn dialog-btn-confirm" @click="closeImportDialog">完成</button>
              </template>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </TeacherLayout>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600;700&display=swap');

.page-container {
  max-width: 1200px;
  margin: 0 auto;
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* ===== 页面头部 ===== */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(0, 102, 255, 0.12);
  border-radius: 10px;
  color: #3a5a7a;
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.95);
  color: #0066ff;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a3a5a;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
}

/* ===== 操作按钮 ===== */
.action-btn {
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

.primary-btn {
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  color: #fff;
}

.primary-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(0, 102, 255, 0.25);
}

.secondary-btn {
  background: rgba(255, 255, 255, 0.8);
  color: #3a5a7a;
  border: 1px solid rgba(0, 102, 255, 0.12);
}

.secondary-btn:hover {
  background: rgba(255, 255, 255, 0.95);
  border-color: rgba(0, 102, 255, 0.25);
  transform: translateY(-1px);
}

.btn-icon {
  flex-shrink: 0;
}

/* ===== 筛选栏 ===== */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-label {
  font-size: 14px;
  font-weight: 500;
  color: #3a5a7a;
  white-space: nowrap;
}

.type-select {
  width: 180px;
}

:deep(.el-select__wrapper) {
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(0, 102, 255, 0.12);
  border-radius: 10px;
  box-shadow: none;
  padding: 8px 12px;
  font-family: inherit;
}

:deep(.el-select__wrapper:hover) {
  border-color: rgba(0, 102, 255, 0.3);
}

:deep(.el-select__wrapper.is-focused) {
  border-color: #0066ff;
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.08);
}

.filter-summary {
  font-size: 14px;
  color: #7a9aba;
}

.filter-summary strong {
  color: #1a3a5a;
}

/* ===== 表格 ===== */
.table-wrapper {
  background: rgba(255, 255, 255, 0.75);
  border-radius: 20px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow:
    0 8px 24px rgba(0, 102, 255, 0.06),
    0 2px 8px rgba(0, 102, 255, 0.03);
  overflow: hidden;
}

/* 批量操作栏 */
.batch-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 20px;
  background: linear-gradient(135deg, rgba(255, 77, 79, 0.06) 0%, rgba(255, 77, 79, 0.02) 100%);
  border-bottom: 1px solid rgba(255, 77, 79, 0.1);
}

.batch-info {
  font-size: 14px;
  color: #3a5a7a;
}

.batch-info strong {
  color: #ff4d4f;
  font-weight: 600;
}

.batch-delete-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 16px;
  background: #ff4d4f;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-family: inherit;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.batch-delete-btn:hover:not(:disabled) {
  background: #ff7875;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
}

.batch-delete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.question-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.question-table thead {
  background: linear-gradient(135deg, rgba(0, 102, 255, 0.06) 0%, rgba(0, 212, 255, 0.04) 100%);
}

.question-table th {
  text-align: left;
  padding: 16px 20px;
  font-weight: 600;
  color: #3a5a7a;
  font-size: 13px;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 102, 255, 0.08);
}

.question-table td {
  padding: 16px 20px;
  color: #1a3a5a;
  border-bottom: 1px solid rgba(0, 102, 255, 0.05);
}

.question-table tbody tr:last-child td {
  border-bottom: none;
}

.question-table tbody tr:hover {
  background: rgba(0, 102, 255, 0.03);
}

/* 列宽 */
.col-checkbox {
  width: 48px;
  text-align: center;
}

.col-index {
  width: 64px;
  text-align: center;
}

.col-stem {
  min-width: 280px;
  max-width: 500px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.col-type {
  width: 120px;
}

.col-actions {
  width: 160px;
}

.col-index {
  text-align: center;
}

/* 题型的标签 */
.type-tag {
  display: inline-block;
  padding: 3px 12px;
  background: rgba(0, 102, 255, 0.08);
  color: #0066ff;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

/* 选中行高亮 */
.row-selected {
  background: rgba(0, 102, 255, 0.04) !important;
}

/* 自定义复选框 */
.checkbox-wrap {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  line-height: 1;
}

.checkbox-input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
  pointer-events: none;
}

.checkbox-visual {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border: 2px solid rgba(0, 102, 255, 0.25);
  border-radius: 5px;
  background: #fff;
  transition: all 0.2s ease;
  position: relative;
  flex-shrink: 0;
}

.checkbox-visual.checked {
  background: linear-gradient(135deg, #0066ff, #00d4ff);
  border-color: #0066ff;
}

.checkbox-visual.checked::after {
  content: '';
  position: absolute;
  width: 5px;
  height: 9px;
  border: solid #fff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
  top: 1px;
}

.checkbox-visual.indeterminate::before {
  content: '';
  position: absolute;
  width: 10px;
  height: 2px;
  background: #0066ff;
  border-radius: 1px;
}

/* 操作按钮 */
.action-link {
  background: none;
  border: none;
  padding: 4px 12px;
  font-size: 13px;
  font-family: inherit;
  color: #0066ff;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  position: relative;
}

.action-link + .action-link::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 1px;
  height: 14px;
  background: rgba(0, 102, 255, 0.15);
}

.action-link:hover {
  color: #00d4ff;
}

.action-link-danger {
  color: #ff4d4f;
}

.action-link-danger:hover {
  color: #ff7875;
}

.action-link-danger:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* 空状态 */
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
}

.empty-text {
  font-size: 15px;
  color: #7a9aba;
  margin: 0;
}

.empty-cell {
  text-align: center !important;
  color: #7a9aba !important;
  padding: 60px 20px !important;
  font-size: 14px;
}

/* ===== 弹窗通用 ===== */
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
  max-height: 80vh;
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
  margin: 0 0 8px;
  text-align: center;
}

.dialog-desc {
  font-size: 13px;
  color: #7a9aba;
  text-align: center;
  margin: 0 0 20px;
  line-height: 1.5;
}

/* 表单 */
.form-group {
  margin-bottom: 18px;
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
  resize: vertical;
  box-sizing: border-box;
  transition: all 0.3s ease;
  line-height: 1.6;
}

.form-textarea::placeholder {
  color: #b0c8e0;
}

.form-textarea:focus {
  border-color: #0066ff;
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.08);
  background: rgba(255, 255, 255, 0.9);
}

.readonly-field {
  padding: 12px 16px;
  background: rgba(0, 102, 255, 0.04);
  border: 1px solid rgba(0, 102, 255, 0.1);
  border-radius: 12px;
  font-size: 15px;
  color: #3a5a7a;
}

/* 选项行 */
.option-row {
  margin-bottom: 10px;
}

.option-input-wrapper {
  display: flex;
  gap: 8px;
  align-items: center;
}

.option-remove-btn {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 77, 79, 0.08);
  border: none;
  border-radius: 8px;
  color: #ff4d4f;
  cursor: pointer;
  transition: all 0.2s ease;
}

.option-remove-btn:hover {
  background: rgba(255, 77, 79, 0.15);
}

.add-option-btn {
  display: block;
  width: 100%;
  padding: 8px;
  background: rgba(0, 102, 255, 0.04);
  border: 1px dashed rgba(0, 102, 255, 0.2);
  border-radius: 10px;
  color: #0066ff;
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 18px;
}

.add-option-btn:hover {
  background: rgba(0, 102, 255, 0.08);
  border-color: rgba(0, 102, 255, 0.35);
}

/* 导入模板 */
.import-template-area {
  text-align: center;
  margin-bottom: 20px;
}

.template-download-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  background: rgba(0, 102, 255, 0.06);
  border: 1px solid rgba(0, 102, 255, 0.15);
  border-radius: 10px;
  color: #0066ff;
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
}

.template-download-btn:hover {
  background: rgba(0, 102, 255, 0.1);
}

.upload-area {
  border: 2px dashed rgba(0, 102, 255, 0.2);
  border-radius: 16px;
  padding: 36px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(0, 102, 255, 0.02);
}

.upload-area:hover {
  border-color: #0066ff;
  background: rgba(0, 102, 255, 0.04);
}

.upload-icon {
  color: #b0c8e0;
  margin-bottom: 8px;
}

.upload-text {
  font-size: 14px;
  color: #3a5a7a;
  margin: 0 0 4px;
}

.upload-hint {
  font-size: 12px;
  color: #b0c8e0;
  margin: 0;
}

.upload-disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.upload-disabled:hover {
  border-color: rgba(0, 102, 255, 0.2);
  background: rgba(0, 102, 255, 0.02);
}

/* ---------- 导入预览 ---------- */
.import-preview {
  text-align: center;
  padding: 16px 0 8px;
}

.preview-icon {
  color: #0066ff;
  margin-bottom: 12px;
}

.preview-filename {
  font-size: 15px;
  font-weight: 500;
  color: #1a3a5a;
  margin: 0 0 8px;
  word-break: break-all;
}

.preview-count {
  font-size: 16px;
  color: #3a5a7a;
  margin: 0;
}

.preview-count strong {
  color: #0066ff;
  font-size: 22px;
  font-weight: 700;
}

/* ---------- 导入结果 ---------- */
.import-result {
  text-align: center;
  padding: 8px 0 4px;
}

.result-icon {
  margin-bottom: 12px;
}

.result-success {
  color: #22c55e;
}

.result-warning {
  color: #f59e0b;
}

.result-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a3a5a;
  margin: 0 0 16px;
}

.result-stats {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
}

.stat {
  font-size: 14px;
  padding: 4px 14px;
  border-radius: 20px;
  font-weight: 500;
}

.stat-success {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.stat-fail {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.stat-total {
  background: rgba(0, 102, 255, 0.08);
  color: #0066ff;
}

.result-errors {
  text-align: left;
  background: rgba(245, 158, 11, 0.05);
  border-radius: 12px;
  padding: 12px 16px;
  max-height: 180px;
  overflow-y: auto;
}

.errors-title {
  font-size: 13px;
  font-weight: 600;
  color: #f59e0b;
  margin: 0 0 8px;
}

.error-item {
  display: flex;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
  line-height: 1.5;
}

.error-row {
  color: #f59e0b;
  font-weight: 500;
  white-space: nowrap;
  flex-shrink: 0;
}

.error-reason {
  color: #7a9aba;
}

/* 弹窗按钮 */
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

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .col-actions {
    width: auto;
  }

  .col-stem {
    max-width: 160px;
  }

  .question-table th,
  .question-table td {
    padding: 12px 14px;
    font-size: 13px;
  }

  .dialog-card {
    padding: 24px 20px 20px;
  }
}
</style>
