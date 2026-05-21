<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import TeacherLayout from '@/layouts/TeacherLayout.vue'

const router = useRouter()
import { getQuestionBankList, createQuestionBank, updateQuestionBank, deleteQuestionBank, type QuestionBankItem } from '@/api/question'

const loading = ref(false)
const bankList = ref<QuestionBankItem[]>([])

// 新建 / 编辑题库弹窗
const showDialog = ref(false)
const submitting = ref(false)
const editingId = ref<string | null>(null)
const dialogForm = ref({ name: '' })

function openCreateDialog() {
  editingId.value = null
  dialogForm.value = { name: '' }
  showDialog.value = true
}

function openEditDialog(row: QuestionBankItem) {
  editingId.value = row.id
  dialogForm.value = { name: row.name }
  showDialog.value = true
}

function closeCreateDialog() {
  showDialog.value = false
  editingId.value = null
  dialogForm.value = { name: '' }
}

// ---------- API 占位符 ----------

async function fetchBankList() {
  loading.value = true
  try {
    bankList.value = await getQuestionBankList()
  } catch {
    // 错误由拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  const name = dialogForm.value.name.trim()
  if (!name) {
    ElMessage.warning('请输入题库名称')
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateQuestionBank(editingId.value, { name })
      const item = bankList.value.find((item) => item.id === editingId.value)
      if (item) item.name = name
      ElMessage.success('题库名称已更新')
    } else {
      const id = await createQuestionBank({ name })
      bankList.value.unshift({
        id,
        name,
        questionCount: 0,
        createTime: new Date().toLocaleString('zh-CN', { hour12: false }),
      })
      ElMessage.success('题库创建成功')
    }
    closeCreateDialog()
  } catch {
    // 错误由拦截器统一处理
  } finally {
    submitting.value = false
  }
}

function handleEdit(row: QuestionBankItem) {
  openEditDialog(row)
}

function handleManageQuestions(row: QuestionBankItem) {
  router.push({
    name: 'teacherQuestionManage',
    params: { bankId: row.id },
    query: { name: row.name },
  })
}

function handleGenerateExam(row: QuestionBankItem) {
  // TODO: 跳转到组卷页面
  ElMessage.info('组卷 — 待实现')
}

function handleDelete(row: QuestionBankItem) {
  ElMessageBox.confirm(`确认删除题库「${row.name}」吗？删除后不可恢复。`, '删除确认', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await deleteQuestionBank(row.id)
      bankList.value = bankList.value.filter((item) => item.id !== row.id)
      ElMessage.success('删除成功')
    } catch {
      // 错误由拦截器统一处理
    }
  }).catch(() => {
    // 取消删除
  })
}

onMounted(() => {
  fetchBankList()
})
</script>

<template>
  <TeacherLayout>
    <div class="page-container">
      <!-- 工具栏 -->
      <div class="toolbar">
        <h1 class="page-title">题库管理</h1>
        <button class="create-btn" @click="openCreateDialog">
          <svg viewBox="0 0 20 20" width="16" height="16" class="btn-icon">
            <line x1="10" y1="3" x2="10" y2="17" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
            <line x1="3" y1="10" x2="17" y2="10" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
          </svg>
          <span>新建题库</span>
        </button>
      </div>

      <!-- 表格 -->
      <div class="table-wrapper">
        <div v-if="loading" class="empty-state">
          <p class="empty-text">加载中...</p>
        </div>
        <table v-else class="bank-table">
          <thead>
            <tr>
              <th class="col-index">序号</th>
              <th class="col-name">题库名称</th>
              <th class="col-count">题量</th>
              <th class="col-time">创建时间</th>
              <th class="col-actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in bankList" :key="row.id">
              <td class="col-index">{{ index + 1 }}</td>
              <td class="col-name">{{ row.name }}</td>
              <td class="col-count">{{ row.questionCount }}</td>
              <td class="col-time">{{ row.createTime }}</td>
              <td class="col-actions">
                <button class="action-link" @click="handleEdit(row)">编辑题库</button>
                <button class="action-link" @click="handleManageQuestions(row)">管理题目</button>
                <button class="action-link" @click="handleGenerateExam(row)">组卷</button>
                <button class="action-link action-link-danger" @click="handleDelete(row)">删除</button>
              </td>
            </tr>
            <tr v-if="bankList.length === 0">
              <td colspan="5" class="empty-cell">暂无题库，点击上方按钮创建第一个题库</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 新建 / 编辑题库弹窗 -->
    <teleport to="body">
      <transition name="dialog">
        <div v-if="showDialog" class="dialog-overlay" @click.self="closeCreateDialog">
          <div class="dialog-card">
            <h3 class="dialog-title">{{ editingId ? '编辑题库' : '新建题库' }}</h3>
            <div class="form-group">
              <label class="form-label">
                题库名称 <span class="required">*</span>
              </label>
              <input
                v-model="dialogForm.name"
                type="text"
                class="form-input"
                placeholder="例：Java 基础题库"
                maxlength="100"
              />
            </div>
            <div class="dialog-actions">
              <button class="dialog-btn dialog-btn-cancel" @click="closeCreateDialog">取消</button>
              <button
                class="dialog-btn dialog-btn-confirm"
                :disabled="submitting"
                @click="handleSubmit"
              >
                {{ submitting ? '保存中...' : (editingId ? '确认修改' : '确认创建') }}
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

/* 表格容器 */
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

.bank-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.bank-table thead {
  background: linear-gradient(135deg, rgba(0, 102, 255, 0.06) 0%, rgba(0, 212, 255, 0.04) 100%);
}

.bank-table th {
  text-align: left;
  padding: 16px 20px;
  font-weight: 600;
  color: #3a5a7a;
  font-size: 13px;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 102, 255, 0.08);
}

.bank-table td {
  padding: 16px 20px;
  color: #1a3a5a;
  border-bottom: 1px solid rgba(0, 102, 255, 0.05);
  white-space: nowrap;
}

.bank-table tbody tr:last-child td {
  border-bottom: none;
}

.bank-table tbody tr:hover {
  background: rgba(0, 102, 255, 0.03);
}

/* 列宽控制 */
.col-index {
  width: 60px;
  text-align: center;
}

.col-name {
  min-width: 200px;
}

.col-count {
  width: 80px;
  text-align: center;
}

.col-time {
  width: 180px;
}

.col-actions {
  width: 320px;
}

.col-index,
.col-count {
  text-align: center;
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

/* ===== 新建题库弹窗 ===== */
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

/* 响应式 */
@media (max-width: 768px) {
  .col-actions {
    width: auto;
  }

  .col-time {
    display: none;
  }

  .bank-table th,
  .bank-table td {
    padding: 12px 14px;
    font-size: 13px;
  }
}
</style>
