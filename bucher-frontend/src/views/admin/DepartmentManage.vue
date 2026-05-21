<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Edit, Delete, Download, ArrowLeft } from '@element-plus/icons-vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import {
  getDepartments,
  getClasses,
  getUsers,
  createDepartment,
  updateDepartment,
  deleteDepartment,
  createClass,
  resetUserPassword,
  importStudents,
  importTeachers,
  type Department,
  type AdminClass,
  type UserItem,
  type PaginatedResult
} from '@/api/admin'

// ---- 院系数据（分页） ----
const deptPage = ref(1)
const deptPageSize = ref(6)
const deptTotal = ref(0)
const departments = ref<Department[]>([])

const deptClasses = ref<AdminClass[]>([])
const classLoading = ref(false)

const loading = ref(false)

// ---- 展开班级成员 ----
const selectedDeptId = ref<number | null>(null)
const expandedDeptId = ref<number | null>(null)
const expandedClassId = ref<number | null>(null)
const expandedClassName = ref('')
const expandedMembers = ref<UserItem[]>([])
const memberTotal = ref(0)
const memberPage = ref(1)
const memberPageSize = ref(10)

// ---- 弹窗可见性 ----
const showCreateDeptDialog = ref(false)
const showEditDeptDialog = ref(false)
const showCreateClassDialog = ref(false)
const showBatchImportDialog = ref(false)

// ---- 弹窗表单数据 ----
const newDeptName = ref('')
const newDeptType = ref<1 | 2>(1)
const newDeptDesc = ref('')
const editDeptId = ref(0)
const editDeptName = ref('')
const editDeptType = ref<1 | 2>(1)
const editDeptDesc = ref('')
const newClassName = ref('')
const newClassYear = ref(new Date().getFullYear())
const currentDeptId = ref(0)

// ---- 批量导入 ----
type ImportTarget =
  | { kind: 'students'; adminClassId: number }
  | { kind: 'teachers'; deptId: number }
  | null

const batchImportStep = ref(0)
const batchImportTarget = ref<ImportTarget>(null)
const batchImportPreview = ref<Array<{ name: string; account: string; role: string; error?: string }>>([])
const batchImportFile = ref<File | null>(null)

const batchImportValid = computed(() =>
  batchImportPreview.value.length > 0 && batchImportPreview.value.every((row) => !row.error)
)

const batchImportErrorCount = computed(() =>
  batchImportPreview.value.filter((row) => row.error).length
)

// ---- 数据加载 ----
async function loadDepartments() {
  loading.value = true
  try {
    const res: PaginatedResult<Department> = await getDepartments({
      page: deptPage.value,
      size: deptPageSize.value
    })
    departments.value = res.records
    deptTotal.value = res.total
  } catch {
    // 后端未就绪时静默失败
  } finally {
    loading.value = false
  }
}

async function loadDeptClasses(deptId: number) {
  classLoading.value = true
  try {
    const res = await getClasses({ deptId, size: 100 })
    deptClasses.value = res.records
  } catch {
    deptClasses.value = []
  } finally {
    classLoading.value = false
  }
}

function onDeptPageChange(page: number) {
  deptPage.value = page
  loadDepartments()
}

onMounted(() => {
  loadDepartments()
})

// ---- 展开班级成员 ----
async function handleClassClick(deptId: number, cls: AdminClass) {
  if (expandedClassId.value === cls.id) {
    expandedDeptId.value = null
    expandedClassId.value = null
    expandedClassName.value = ''
    expandedMembers.value = []
    memberTotal.value = 0
    memberPage.value = 1
    return
  }
  expandedDeptId.value = deptId
  expandedClassId.value = cls.id
  expandedClassName.value = cls.name
  memberPage.value = 1
  await loadMembers(cls.id)
}

async function loadMembers(adminClassId: number) {
  try {
    const res = await getUsers({
      adminClassId,
      page: memberPage.value,
      size: memberPageSize.value
    })
    expandedMembers.value = res.records
    memberTotal.value = res.total
  } catch {
    expandedMembers.value = []
    memberTotal.value = 0
  }
}

function onMemberPageChange(page: number) {
  memberPage.value = page
  if (expandedClassId.value) {
    loadMembers(expandedClassId.value)
  }
}

function onMemberPageSizeChange(size: number) {
  memberPageSize.value = size
  memberPage.value = 1
  if (expandedClassId.value) {
    loadMembers(expandedClassId.value)
  }
}

function handleDeptRowClick(dept: Department) {
  selectedDeptId.value = dept.id
  deptClasses.value = []
  loadDeptClasses(dept.id)
}

function handleBackToDeptList() {
  selectedDeptId.value = null
  deptClasses.value = []
  expandedDeptId.value = null
  expandedClassId.value = null
  expandedClassName.value = ''
  expandedMembers.value = []
  memberTotal.value = 0
}

function handleCloseTable() {
  expandedDeptId.value = null
  expandedClassId.value = null
  expandedClassName.value = ''
  expandedMembers.value = []
  memberTotal.value = 0
}

// ---- 角色 / 状态 映射 ----
function getRoleType(role: number) {
  return role === 3 ? 'success' : role === 2 ? 'warning' : ''
}
function getRoleText(role: number) {
  return role === 3 ? '学生' : role === 2 ? '教师' : '管理员'
}
function getStatusType(status: number) {
  return status === 1 ? 'success' : 'danger'
}
function getStatusText(status: number) {
  return status === 1 ? '正常' : '已禁用'
}

// ---- 新建院系 ----
async function handleCreateDepartment() {
  const name = newDeptName.value.trim()
  if (!name) {
    ElMessage.warning('请输入院系名称')
    return
  }
  try {
    await createDepartment({ name, type: newDeptType.value, description: newDeptDesc.value || undefined })
    ElMessage.success('院系创建成功')
    showCreateDeptDialog.value = false
    newDeptName.value = ''
    newDeptType.value = 1
    newDeptDesc.value = ''
    loadDepartments()
  } catch {
    ElMessage.error('创建失败')
  }
}

// ---- 编辑院系 ----
function openEditDeptDialog(dept: Department) {
  editDeptId.value = dept.id
  editDeptName.value = dept.name
  editDeptType.value = dept.type
  editDeptDesc.value = dept.description || ''
  showEditDeptDialog.value = true
}

async function handleEditDepartment() {
  const name = editDeptName.value.trim()
  if (!name) {
    ElMessage.warning('请输入院系名称')
    return
  }
  try {
    await updateDepartment(editDeptId.value, {
      name,
      type: editDeptType.value,
      description: editDeptDesc.value || undefined
    })
    ElMessage.success('院系编辑成功')
    showEditDeptDialog.value = false
    loadDepartments()
  } catch {
    ElMessage.error('编辑失败')
  }
}

// ---- 删除院系 ----
function handleDeleteDepartment(dept: Department) {
  ElMessageBox.confirm(
    `确定要删除院系「${dept.name}」吗？删除后不可恢复。`,
    '确认删除',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      await deleteDepartment(dept.id)
      ElMessage.success('院系已删除')
      loadDepartments()
    } catch {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// ---- 新建班级 ----
function openCreateClassDialog(deptId: number) {
  currentDeptId.value = deptId
  newClassName.value = ''
  newClassYear.value = new Date().getFullYear()
  showCreateClassDialog.value = true
}

async function handleCreateClass() {
  const name = newClassName.value.trim()
  if (!name) {
    ElMessage.warning('请输入班级名称')
    return
  }
  try {
    await createClass({ name, deptId: currentDeptId.value, year: newClassYear.value })
    ElMessage.success('班级创建成功')
    showCreateClassDialog.value = false
    loadDepartments()
  } catch {
    ElMessage.error('创建失败')
  }
}

// ---- 重置密码 ----
function handleResetPassword(user: UserItem) {
  ElMessageBox.confirm(
    '确认重置密码？重置后密码为 Zx@123456，用户下次登录需修改密码。',
    '重置密码',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      await resetUserPassword(user.id)
      ElMessage.success(`已重置用户「${user.realName}」的密码为 Zx@123456`)
    } catch {
      ElMessage.error('重置失败')
    }
  }).catch(() => {})
}

// ---- 批量导入 ----
function openBatchImportDialog(target?: ImportTarget) {
  batchImportStep.value = 0
  batchImportPreview.value = []
  batchImportFile.value = null
  batchImportTarget.value = target || null
  showBatchImportDialog.value = true
}

function downloadTemplate() {
  const isTeacher = batchImportTarget.value?.kind === 'teachers'
  const header = isTeacher ? '工号,姓名' : '姓名,账号,角色'
  const examples = isTeacher
    ? 'T001,张三\nT002,李四'
    : '张三,2021001,学生\n李四,2021002,学生'
  const csvContent = `﻿${header}\n${examples}`
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = isTeacher ? '教师导入模板.csv' : '学生导入模板.csv'
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('模板下载成功')
}

function handleFileChange(uploadFile: any) {
  const raw: File = uploadFile.raw
  if (!raw) return

  batchImportFile.value = raw

  const reader = new FileReader()
  reader.onload = (e) => {
    const text = e.target?.result as string
    const lines = text.split(/\r?\n/).filter((l) => l.trim())
    if (lines.length < 2) {
      ElMessage.error('文件内容为空或格式不正确')
      return
    }
    const isTeacher = batchImportTarget.value?.kind === 'teachers'
    const rows: Array<{ name: string; account: string; role: string; error?: string }> = []
    for (let i = 1; i < lines.length; i++) {
      const cols = lines[i].split(',')
      let name: string, account: string, role: string
      if (isTeacher) {
        account = (cols[0] || '').trim()
        name = (cols[1] || '').trim()
        role = '教师'
      } else {
        name = (cols[0] || '').trim()
        account = (cols[1] || '').trim()
        role = (cols[2] || '').trim()
      }
      const row = { name, account, role }
      const errors: string[] = []
      if (!row.name) errors.push('姓名为空')
      if (!row.account) errors.push(isTeacher ? '工号为空' : '账号为空')
      if (!isTeacher && !['学生', '教师'].includes(row.role)) errors.push('角色必须为"学生"或"教师"')
      if (errors.length > 0) {
        rows.push({ ...row, error: errors.join('；') })
      } else {
        rows.push(row)
      }
    }
    batchImportPreview.value = rows
    batchImportStep.value = 2
  }
  reader.readAsText(raw, 'UTF-8')
}

async function confirmBatchImport() {
  if (!batchImportValid.value || !batchImportFile.value || !batchImportTarget.value) return

  try {
    const target = batchImportTarget.value
    if (target.kind === 'students') {
      await importStudents(target.adminClassId, batchImportFile.value)
    } else {
      await importTeachers(target.deptId, batchImportFile.value)
    }
    ElMessage.success(`成功导入 ${batchImportPreview.value.length} 条数据`)
    showBatchImportDialog.value = false
    loadDepartments()
  } catch {
    ElMessage.error('导入失败')
  }
}

function getImportRowStyle(row: { error?: string }) {
  return row.error ? { color: '#f56c6c' } : {}
}
</script>

<template>
  <AdminLayout>
    <div class="department-manage">
      <!-- 背景装饰 -->
      <div class="bg-grid"></div>
      <div class="floating-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
      </div>

      <div class="content-container">
        <!-- 页面标题 -->
        <div class="page-header">
          <div class="header-left">
            <h1 class="page-title">院系管理</h1>
            <p class="page-subtitle">共 {{ deptTotal }} 个院系</p>
          </div>
          <div v-if="!selectedDeptId && !expandedClassId" class="header-actions">
            <button class="btn-secondary" @click="openBatchImportDialog()">
              <el-icon><Upload /></el-icon>
              <span>批量导入</span>
            </button>
            <button class="btn-primary" @click="newDeptName = ''; newDeptType = 1; newDeptDesc = ''; showCreateDeptDialog = true">
              <el-icon><Plus /></el-icon>
              <span>新建院系</span>
            </button>
          </div>
        </div>

        <!-- 面包屑 -->
        <div v-if="selectedDeptId || expandedClassId" class="breadcrumb-bar">
          <span class="breadcrumb-link" @click="handleBackToDeptList">院系管理</span>
          <span class="breadcrumb-sep">›</span>
          <span
            v-if="expandedClassId"
            class="breadcrumb-link"
            @click="handleCloseTable()"
          >{{ departments.find(d => d.id === selectedDeptId)?.name }}</span>
          <span v-else class="breadcrumb-current">{{ departments.find(d => d.id === selectedDeptId)?.name }}</span>
          <template v-if="expandedClassId">
            <span class="breadcrumb-sep">›</span>
            <span class="breadcrumb-current">{{ expandedClassName }}</span>
          </template>
          <span class="breadcrumb-close" @click="expandedClassId ? handleCloseTable() : handleBackToDeptList()">
            <el-icon><ArrowLeft /></el-icon>
          </span>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="loading-state">
          <p>加载中...</p>
        </div>

        <!-- 院系列表 -->
        <div v-if="!selectedDeptId && !expandedClassId && !loading && departments.length > 0" class="dept-table-section">
          <el-table
            :data="departments"
            stripe
            style="width: 100%"
            empty-text="暂无院系数据"
            @row-click="handleDeptRowClick"
          >
            <el-table-column prop="name" label="名称" min-width="200">
              <template #default="{ row }">
                <span class="dept-name-link">{{ row.name }}</span>
              </template>
            </el-table-column>
            <el-table-column label="班级数" width="120" align="center">
              <template #default="{ row }">
                <span class="stat-cell">{{ row.classCount ?? 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="学生数" width="120" align="center">
              <template #default="{ row }">
                <span class="stat-cell">{{ row.studentCount ?? 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="教师数" width="120" align="center">
              <template #default="{ row }">
                <span class="stat-cell">{{ row.teacherCount ?? 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="240" fixed="right">
              <template #default="{ row }">
                <div class="table-row-actions">
                  <button class="table-action-btn" @click.stop="openEditDeptDialog(row)">
                    <el-icon><Edit /></el-icon>
                    <span>编辑</span>
                  </button>
                  <button class="table-action-btn danger" @click.stop="handleDeleteDepartment(row)">
                    <el-icon><Delete /></el-icon>
                    <span>删除</span>
                  </button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="deptTotal > deptPageSize" class="dept-pagination">
            <el-pagination
              v-model:current-page="deptPage"
              :page-size="deptPageSize"
              :total="deptTotal"
              layout="total, prev, pager, next, jumper"
              @current-change="onDeptPageChange"
            />
          </div>
        </div>

        <!-- 院系详情：班级列表 -->
        <div v-if="selectedDeptId && !expandedClassId" class="dept-detail-section">
          <div class="dept-detail-header">
            <div class="dept-detail-info">
              <h3>{{ departments.find(d => d.id === selectedDeptId)?.name }}</h3>
              <div class="dept-detail-stats">
                <span>班级：{{ departments.find(d => d.id === selectedDeptId)?.classCount ?? 0 }}</span>
                <span class="stat-sep">|</span>
                <span>学生：{{ departments.find(d => d.id === selectedDeptId)?.studentCount ?? 0 }}</span>
                <span class="stat-sep">|</span>
                <span>教师：{{ departments.find(d => d.id === selectedDeptId)?.teacherCount ?? 0 }}</span>
              </div>
            </div>
            <div class="dept-detail-actions">
              <button class="btn-secondary" @click="openBatchImportDialog({ kind: 'teachers', deptId: selectedDeptId! })">
                <el-icon><Upload /></el-icon>
                <span>批量导入教师</span>
              </button>
              <button class="btn-primary" @click="openCreateClassDialog(selectedDeptId!)">
                <el-icon><Plus /></el-icon>
                <span>新建班级</span>
              </button>
            </div>
          </div>

          <div v-if="classLoading" class="dept-detail-empty">
            <p>加载中...</p>
          </div>

          <div v-else-if="deptClasses.length > 0" class="class-card-grid">
            <div
              v-for="cls in deptClasses"
              :key="cls.id"
              class="class-card"
              @click="handleClassClick(selectedDeptId!, cls)"
            >
              <div class="class-card-icon">
                <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
                  <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
                </svg>
              </div>
              <span class="class-card-name">{{ cls.name }}</span>
              <span class="class-card-year" v-if="cls.year">{{ cls.year }}级</span>
            </div>
          </div>

          <div v-else class="dept-detail-empty">
            <p>该院系暂无班级，请新建班级</p>
          </div>
        </div>

        <!-- 加载中 / 空状态 -->
        <div v-if="!selectedDeptId && !expandedClassId && departments.length === 0 && !loading" class="empty-state">
          <p>暂无院系数据</p>
        </div>

        <!-- 班级成员表格 -->
        <div v-if="expandedClassId" class="member-section">
          <div class="member-section-header">
            <div class="member-section-title">
              <h3>{{ expandedClassName }} — 成员列表</h3>
              <span class="member-count">共 {{ memberTotal }} 人</span>
            </div>
            <div class="member-section-actions">
              <button
                class="btn-secondary"
                @click="openBatchImportDialog({ kind: 'students', adminClassId: expandedClassId! })"
              >
                <el-icon><Upload /></el-icon>
                <span>批量导入学生</span>
              </button>
              <button class="btn-primary">
                <el-icon><Plus /></el-icon>
                <span>手动添加</span>
              </button>
            </div>
          </div>

          <el-table
            :data="expandedMembers"
            stripe
            style="width: 100%"
            empty-text="暂无成员"
          >
            <el-table-column prop="realName" label="姓名" min-width="120" />
            <el-table-column prop="userNo" label="账号" min-width="140" />
            <el-table-column prop="role" label="角色" width="100">
              <template #default="{ row }">
                <el-tag :type="getRoleType(row.role)" size="small" effect="light">
                  {{ getRoleText(row.role) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small" effect="light">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <button class="table-action-btn" @click="handleResetPassword(row)">重置密码</button>
                <button
                  class="table-action-btn"
                  :class="row.status === 1 ? 'danger' : 'success'"
                >
                  {{ row.status === 1 ? '禁用' : '启用' }}
                </button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 成员分页 -->
          <div class="member-pagination">
            <el-pagination
              v-model:current-page="memberPage"
              v-model:page-size="memberPageSize"
              :total="memberTotal"
              :page-sizes="[5, 10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              small
              @current-change="onMemberPageChange"
              @size-change="onMemberPageSizeChange"
            />
          </div>
        </div>
      </div>

      <!-- ======================== 新建院系弹窗 ======================== -->
      <el-dialog v-model="showCreateDeptDialog" title="新建院系" width="480px" :close-on-click-modal="false">
        <div class="dialog-form">
          <div class="form-item">
            <label class="form-label">院系名称</label>
            <el-input
              v-model="newDeptName"
              placeholder="请输入院系名称"
              maxlength="50"
              show-word-limit
              @keyup.enter="handleCreateDepartment"
            />
          </div>
          <div class="form-item">
            <label class="form-label">院系类型</label>
            <div class="type-selector">
              <label
                class="type-option"
                :class="{ active: newDeptType === 1 }"
                @click="newDeptType = 1"
              >
                学校院系
              </label>
              <label
                class="type-option"
                :class="{ active: newDeptType === 2 }"
                @click="newDeptType = 2"
              >
                企业部门
              </label>
            </div>
          </div>
          <div class="form-item">
            <label class="form-label">描述（选填）</label>
            <el-input
              v-model="newDeptDesc"
              type="textarea"
              placeholder="请输入院系描述"
              maxlength="200"
              show-word-limit
              :rows="3"
            />
          </div>
        </div>
        <template #footer>
          <div class="dialog-footer">
            <button class="btn-cancel" @click="showCreateDeptDialog = false">取消</button>
            <button class="btn-primary" :disabled="!newDeptName.trim()" @click="handleCreateDepartment">确定</button>
          </div>
        </template>
      </el-dialog>

      <!-- ======================== 编辑院系弹窗 ======================== -->
      <el-dialog v-model="showEditDeptDialog" title="编辑院系" width="480px" :close-on-click-modal="false">
        <div class="dialog-form">
          <div class="form-item">
            <label class="form-label">院系名称</label>
            <el-input
              v-model="editDeptName"
              placeholder="请输入院系名称"
              maxlength="50"
              show-word-limit
              @keyup.enter="handleEditDepartment"
            />
          </div>
          <div class="form-item">
            <label class="form-label">院系类型</label>
            <div class="type-selector">
              <label
                class="type-option"
                :class="{ active: editDeptType === 1 }"
                @click="editDeptType = 1"
              >
                学校院系
              </label>
              <label
                class="type-option"
                :class="{ active: editDeptType === 2 }"
                @click="editDeptType = 2"
              >
                企业部门
              </label>
            </div>
          </div>
          <div class="form-item">
            <label class="form-label">描述（选填）</label>
            <el-input
              v-model="editDeptDesc"
              type="textarea"
              placeholder="请输入院系描述"
              maxlength="200"
              show-word-limit
              :rows="3"
            />
          </div>
        </div>
        <template #footer>
          <div class="dialog-footer">
            <button class="btn-cancel" @click="showEditDeptDialog = false">取消</button>
            <button class="btn-primary" :disabled="!editDeptName.trim()" @click="handleEditDepartment">确定</button>
          </div>
        </template>
      </el-dialog>

      <!-- ======================== 新建班级弹窗 ======================== -->
      <el-dialog v-model="showCreateClassDialog" title="新建班级" width="440px" :close-on-click-modal="false">
        <div class="dialog-form">
          <div class="form-item">
            <label class="form-label">班级名称</label>
            <el-input
              v-model="newClassName"
              placeholder="请输入班级名称"
              maxlength="50"
              show-word-limit
              @keyup.enter="handleCreateClass"
            />
          </div>
          <div class="form-item">
            <label class="form-label">入学年份</label>
            <el-input-number
              v-model="newClassYear"
              :min="2000"
              :max="2099"
              style="width: 100%"
            />
          </div>
        </div>
        <template #footer>
          <div class="dialog-footer">
            <button class="btn-cancel" @click="showCreateClassDialog = false">取消</button>
            <button class="btn-primary" :disabled="!newClassName.trim()" @click="handleCreateClass">确定</button>
          </div>
        </template>
      </el-dialog>

      <!-- ======================== 批量导入弹窗 ======================== -->
      <el-dialog
        v-model="showBatchImportDialog"
        title="批量导入"
        width="720px"
        :close-on-click-modal="false"
        @closed="batchImportStep = 0; batchImportPreview = []; batchImportFile = null"
      >
        <!-- 步骤条 -->
        <el-steps :active="batchImportStep" align-center class="import-steps">
          <el-step title="下载模板" />
          <el-step title="上传文件" />
          <el-step title="预览确认" />
        </el-steps>

        <!-- 步骤 0：下载模板 -->
        <div v-if="batchImportStep === 0" class="step-content">
          <div class="step-icon-wrapper">
            <svg viewBox="0 0 64 64" width="64" height="64" fill="none" stroke="#0066ff" stroke-width="1.5">
              <rect x="8" y="8" width="48" height="48" rx="6" />
              <line x1="20" y1="20" x2="44" y2="20" />
              <line x1="20" y1="28" x2="44" y2="28" />
              <line x1="20" y1="36" x2="36" y2="36" />
              <line x1="20" y1="44" x2="40" y2="44" />
            </svg>
          </div>
          <p class="step-desc">请先下载导入模板，按照模板格式填写数据后上传。</p>
          <button class="btn-primary" @click="downloadTemplate">
            <el-icon><Download /></el-icon>
            <span>下载模板（CSV）</span>
          </button>
          <p class="step-hint">模板包含：姓名、账号、角色（学生/教师）三列</p>
        </div>

        <!-- 步骤 1：上传文件 -->
        <div v-if="batchImportStep === 1" class="step-content">
          <el-upload
            class="upload-zone"
            drag
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls,.csv"
            :on-change="handleFileChange"
          >
            <div class="upload-placeholder">
              <svg viewBox="0 0 48 48" width="48" height="48" fill="none" stroke="#a0b8d0" stroke-width="1.5">
                <path d="M24 32V16M18 22l6-6 6 6" />
                <path d="M8 34v4a4 4 0 0 0 4 4h24a4 4 0 0 0 4-4v-4" />
              </svg>
              <p class="upload-text">将文件拖到此处，或 <em>点击上传</em></p>
              <p class="upload-hint">支持 .xlsx、.xls、.csv 格式</p>
            </div>
          </el-upload>
        </div>

        <!-- 步骤 2：预览确认 -->
        <div v-if="batchImportStep === 2" class="step-content">
          <div v-if="batchImportValid" class="preview-badge success">
            <span>共 {{ batchImportPreview.length }} 条数据，验证通过，可以导入</span>
          </div>
          <div v-else class="preview-badge error">
            <span>共 {{ batchImportErrorCount }} 条错误数据，请修正后重新上传</span>
          </div>

          <el-table :data="batchImportPreview" max-height="320" stripe class="preview-table">
            <template v-if="batchImportTarget?.kind === 'teachers'">
              <el-table-column prop="account" label="工号" min-width="120" />
              <el-table-column prop="name" label="姓名" min-width="100" />
            </template>
            <template v-else>
              <el-table-column prop="name" label="姓名" min-width="100" />
              <el-table-column prop="account" label="账号" min-width="120" />
              <el-table-column prop="role" label="角色" width="80" />
            </template>
            <el-table-column prop="error" label="错误原因" min-width="180">
              <template #default="{ row }">
                <span v-if="row.error" class="error-text">{{ row.error }}</span>
                <span v-else class="ok-text">--</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <template #footer>
          <div class="dialog-footer">
            <button
              v-if="batchImportStep > 0"
              class="btn-cancel"
              @click="batchImportStep--"
            >
              上一步
            </button>
            <button
              v-if="batchImportStep < 2"
              class="btn-primary"
              :disabled="batchImportStep === 1 && batchImportPreview.length === 0"
              @click="batchImportStep++"
            >
              下一步
            </button>
            <button
              v-if="batchImportStep === 2"
              class="btn-primary"
              :disabled="!batchImportValid || !batchImportTarget"
              @click="confirmBatchImport"
            >
              确认导入
            </button>
          </div>
        </template>
      </el-dialog>
    </div>
  </AdminLayout>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap');

/* ======================== 基础布局 ======================== */
.department-manage {
  position: relative;
  min-height: calc(100vh - 64px);
  padding: 36px 32px;
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  overflow: hidden;
}

/* 背景装饰 */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 102, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 102, 255, 0.02) 1px, transparent 1px);
  background-size: 60px 60px;
  pointer-events: none;
}

.floating-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.shape {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.06) 0%, rgba(0, 102, 255, 0.03) 100%);
  backdrop-filter: blur(10px);
}

.shape-1 {
  width: 300px; height: 300px;
  top: -100px; right: -60px;
  animation: shapeFloat 18s ease-in-out infinite;
}
.shape-2 {
  width: 220px; height: 220px;
  bottom: -60px; left: -40px;
  animation: shapeFloat 22s ease-in-out infinite reverse;
}
.shape-3 {
  width: 160px; height: 160px;
  top: 50%; right: 10%;
  animation: shapeFloat 16s ease-in-out infinite;
}

@keyframes shapeFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(20px, -20px) scale(1.05); }
  66% { transform: translate(-12px, 12px) scale(0.95); }
}

.content-container {
  position: relative;
  z-index: 10;
  max-width: 1280px;
  margin: 0 auto;
}

/* ======================== 加载 / 空状态 ======================== */
.loading-state,
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 20px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.loading-state p,
.empty-state p {
  font-size: 16px;
  color: #7a9aba;
}

/* ======================== 页面标题 ======================== */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
  flex-wrap: wrap;
  gap: 16px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a3a5a;
  margin: 0 0 4px;
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.page-subtitle {
  font-size: 14px;
  color: #7a9aba;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* ======================== 通用按钮 ======================== */
.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #0066ff, #00d4ff);
  color: #fff;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 4px 14px rgba(0, 102, 255, 0.25);
  font-family: inherit;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 102, 255, 0.35);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.btn-secondary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.75);
  color: #0066ff;
  border: 1px solid rgba(0, 102, 255, 0.2);
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  backdrop-filter: blur(8px);
  font-family: inherit;
}

.btn-secondary:hover {
  background: rgba(0, 102, 255, 0.06);
  border-color: rgba(0, 102, 255, 0.35);
}

.btn-cancel {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.6);
  color: #5a7a9a;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  font-family: inherit;
}

.btn-cancel:hover {
  background: rgba(0, 0, 0, 0.04);
}

/* ======================== 面包屑 ======================== */
.breadcrumb-bar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 10px 18px;
  margin-bottom: 24px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 14px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  font-size: 14px;
}

.breadcrumb-link {
  color: #0066ff;
  cursor: pointer;
  font-weight: 500;
  padding: 3px 8px;
  border-radius: 7px;
  transition: all 0.2s ease;
}
.breadcrumb-link:hover {
  background: rgba(0, 102, 255, 0.08);
  color: #0052cc;
}

.breadcrumb-sep {
  color: #c4d4e4;
  font-size: 16px;
  font-weight: 300;
  margin: 0 2px;
  user-select: none;
}

.breadcrumb-current {
  color: #1a3a5a;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 7px;
  background: rgba(0, 0, 0, 0.03);
}

.breadcrumb-close {
  margin-left: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  color: #a0b8d0;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s ease;
  font-size: 16px;
}
.breadcrumb-close:hover {
  color: #0066ff;
  background: rgba(0, 102, 255, 0.08);
}

/* ======================== 院系列表 ======================== */
.dept-table-section {
  background: rgba(255, 255, 255, 0.75);
  border-radius: 20px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 24px rgba(0, 102, 255, 0.05);
  overflow: hidden;
}

.dept-name-link {
  color: #0066ff;
  font-weight: 500;
  cursor: pointer;
}

.stat-cell {
  font-size: 16px;
  font-weight: 600;
  color: #1a3a5a;
}

.table-row-actions {
  display: flex;
  gap: 4px;
  flex-wrap: nowrap;
}

/* ======================== 院系分页 ======================== */
.dept-pagination {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

/* ======================== 院系详情：班级列表 ======================== */
.dept-detail-section {
  background: rgba(255, 255, 255, 0.75);
  border-radius: 20px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 24px rgba(0, 102, 255, 0.05);
  padding: 28px;
}

.dept-detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 28px;
  flex-wrap: wrap;
  gap: 16px;
}

.dept-detail-info h3 {
  font-size: 22px;
  font-weight: 700;
  color: #1a3a5a;
  margin: 0 0 8px;
}

.dept-detail-stats {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: #7a9aba;
}

.stat-sep {
  color: rgba(0, 102, 255, 0.15);
}

.dept-detail-actions {
  display: flex;
  gap: 10px;
}

.dept-detail-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 160px;
  color: #a0b8d0;
  font-size: 14px;
}

/* 班级卡片网格 */
.class-card-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.class-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 24px 16px;
  background: rgba(0, 102, 255, 0.03);
  border: 1px solid rgba(0, 102, 255, 0.1);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.class-card:hover {
  background: rgba(0, 102, 255, 0.08);
  border-color: rgba(0, 102, 255, 0.25);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 102, 255, 0.08);
}

.class-card-icon {
  color: #0066ff;
  opacity: 0.7;
}

.class-card-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a3a5a;
}

.class-card-year {
  font-size: 12px;
  color: #7a9aba;
  background: rgba(0, 102, 255, 0.06);
  padding: 2px 8px;
  border-radius: 4px;
}

/* ======================== 成员表格区域 ======================== */
.member-section {
  background: rgba(255, 255, 255, 0.75);
  border-radius: 20px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 24px rgba(0, 102, 255, 0.05);
  overflow: hidden;
}

.member-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.member-section-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.member-section-title h3 {
  font-size: 17px;
  font-weight: 600;
  color: #1a3a5a;
  margin: 0;
}

.member-count {
  font-size: 13px;
  color: #7a9aba;
}

.member-section-actions {
  display: flex;
  gap: 8px;
}

.member-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 24px 20px;
}

/* 表格内操作按钮 */
.table-action-btn {
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid rgba(0, 102, 255, 0.2);
  border-radius: 6px;
  background: transparent;
  color: #0066ff;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.table-action-btn:hover {
  background: rgba(0, 102, 255, 0.06);
}

.table-action-btn.danger {
  color: #ff6b6b;
  border-color: rgba(255, 107, 107, 0.25);
}

.table-action-btn.danger:hover {
  background: rgba(255, 107, 107, 0.06);
}

.table-action-btn.success {
  color: #10b981;
  border-color: rgba(16, 185, 129, 0.25);
}

.table-action-btn.success:hover {
  background: rgba(16, 185, 129, 0.06);
}

/* ======================== 弹窗通用 ======================== */
.dialog-form {
  padding: 8px 0;
}

.form-item {
  margin-bottom: 16px;
}

.form-item:last-child {
  margin-bottom: 0;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #1a3a5a;
  margin-bottom: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 类型选择器 */
.type-selector {
  display: flex;
  gap: 12px;
}

.type-option {
  flex: 1;
  padding: 10px 16px;
  text-align: center;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(0, 102, 255, 0.12);
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: #5a7a9a;
  cursor: pointer;
  transition: all 0.2s ease;
}

.type-option:hover {
  border-color: rgba(0, 102, 255, 0.3);
  background: rgba(0, 102, 255, 0.04);
}

.type-option.active {
  background: linear-gradient(135deg, rgba(0, 102, 255, 0.1) 0%, rgba(0, 212, 255, 0.08) 100%);
  border-color: #0066ff;
  color: #0066ff;
  font-weight: 600;
}

/* ======================== 批量导入弹窗 ======================== */
.import-steps {
  margin-bottom: 32px;
  padding: 0 20px;
}

.step-content {
  padding: 20px 0;
  min-height: 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.step-icon-wrapper {
  margin-bottom: 8px;
  opacity: 0.8;
}

.step-desc {
  font-size: 15px;
  color: #5a7a9a;
  margin: 0;
  text-align: center;
}

.step-hint {
  font-size: 13px;
  color: #a0b8d0;
  margin: 0;
}

/* 上传区域 */
.upload-zone {
  width: 100%;
}

.upload-placeholder {
  text-align: center;
  padding: 10px 0;
}

.upload-text {
  font-size: 14px;
  color: #5a7a9a;
  margin: 12px 0 4px;
}

.upload-text em {
  color: #0066ff;
  font-style: normal;
  cursor: pointer;
}

.upload-hint {
  font-size: 12px;
  color: #a0b8d0;
  margin: 0;
}

/* 预览结果提示 */
.preview-badge {
  padding: 10px 20px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  text-align: center;
  width: 100%;
}

.preview-badge.success {
  background: rgba(16, 185, 129, 0.08);
  color: #059669;
  border: 1px solid rgba(16, 185, 129, 0.2);
}

.preview-badge.error {
  background: rgba(245, 108, 108, 0.08);
  color: #f56c6c;
  border: 1px solid rgba(245, 108, 108, 0.2);
}

.preview-table {
  width: 100%;
}

.error-text {
  color: #f56c6c;
  font-size: 13px;
}

.ok-text {
  color: #10b981;
  font-size: 13px;
}

/* ======================== 深色 Element Plus 弹窗覆盖 ======================== */
:deep(.el-dialog) {
  border-radius: 20px;
  overflow: hidden;
}

:deep(.el-dialog__header) {
  padding: 20px 24px 0;
}

:deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
  color: #1a3a5a;
}

:deep(.el-dialog__body) {
  padding: 20px 24px;
}

:deep(.el-dialog__footer) {
  padding: 0 24px 20px;
}

:deep(.el-steps) {
  justify-content: center;
}

:deep(.el-upload-dragger) {
  border-radius: 16px;
  border: 2px dashed rgba(0, 102, 255, 0.15);
  background: rgba(255, 255, 255, 0.5);
  transition: all 0.25s ease;
}

:deep(.el-upload-dragger:hover) {
  border-color: rgba(0, 102, 255, 0.35);
  background: rgba(0, 102, 255, 0.03);
}

:deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(0, 102, 255, 0.03);
  border-radius: 12px;
  overflow: hidden;
}

:deep(.el-table th.el-table__cell) {
  color: #1a3a5a;
  font-weight: 600;
  font-size: 13px;
}

:deep(.el-table .el-table__row:hover > td) {
  background-color: rgba(0, 102, 255, 0.03) !important;
}

:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
}

:deep(.el-tag) {
  border-radius: 6px;
  font-weight: 500;
}

:deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px rgba(0, 102, 255, 0.12) inset;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(0, 102, 255, 0.25) inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #0066ff inset;
}

/* ======================== 响应式 ======================== */
@media (max-width: 1100px) {
  .class-card-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .department-manage {
    padding: 24px 16px;
  }

  .page-title {
    font-size: 24px;
  }

  .class-card-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .dept-detail-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .dept-detail-actions {
    flex-wrap: wrap;
  }

  .member-section-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .member-section-actions {
    flex-wrap: wrap;
  }
}

@media (max-width: 480px) {
  .department-manage {
    padding: 16px 12px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions .btn-primary,
  .header-actions .btn-secondary {
    flex: 1;
    justify-content: center;
  }

  .class-card-grid {
    grid-template-columns: 1fr;
  }

  .type-selector {
    flex-direction: column;
  }
}
</style>
