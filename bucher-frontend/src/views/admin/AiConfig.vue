<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import {
  getAiConfig,
  saveAiConfig,
  getPermissionsPage,
  searchTeachers,
  grantPermission,
  deletePermission,
  type AiConfig,
  type SaveAiConfigRequest,
  type PermissionRecord,
} from '@/api/ai'

const activeTab = ref('config')
const teacherSubTab = ref('granted')

// ---- AI 配置表单 ----
const config = ref<AiConfig | null>(null)
const loading = ref(true)
const saving = ref(false)

const form = ref<SaveAiConfigRequest>({
  projectName: '',
  provider: 'DEEPSEEK',
  apiKey: '',
  apiEndpoint: 'https://api.deepseek.com',
  modelName: 'deepseek-chat',
  embeddingModel: 'deepseek-embedding',
  embeddingApiKey: '',
  embeddingApiEndpoint: '',
  maxTokens: 4096,
  temperature: 0.7,
  status: 1,
})

const providers = [
  { value: 'DEEPSEEK', label: 'DeepSeek' },
  { value: 'OPENAI', label: 'OpenAI' },
  { value: 'AZURE', label: 'Azure OpenAI' },
  { value: 'QWEN', label: '通义千问' },
  { value: 'GEMINI', label: 'Gemini' },
]

// ---- 已授权 ----
const permissions = ref<PermissionRecord[]>([])
const permLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const pages = ref(0)

// ---- 去授权 ----
const searchKeyword = ref('')
const searchResults = ref<{ id: number; userNo: string; realName: string; granted: boolean }[]>([])
const searching = ref(false)

async function fetchConfig() {
  loading.value = true
  try {
    const res = await getAiConfig()
    if (res.code === 200 && res.data) {
      config.value = res.data
      form.value = {
        projectName: res.data.projectName,
        provider: res.data.provider,
        apiKey: '',
        apiEndpoint: res.data.apiEndpoint,
        modelName: res.data.modelName,
        embeddingModel: res.data.embeddingModel,
        embeddingApiKey: '',
        embeddingApiEndpoint: res.data.embeddingApiEndpoint || '',
        maxTokens: res.data.maxTokens,
        temperature: res.data.temperature,
        status: res.data.status,
      }
    } else {
      config.value = null
    }
  } catch {
    config.value = null
  } finally {
    loading.value = false
  }
}

function resetForm() {
  if (config.value) {
    form.value = {
      projectName: config.value.projectName,
      provider: config.value.provider,
      apiKey: '',
      apiEndpoint: config.value.apiEndpoint,
      modelName: config.value.modelName,
      embeddingModel: config.value.embeddingModel,
      embeddingApiKey: '',
      embeddingApiEndpoint: config.value.embeddingApiEndpoint || '',
      maxTokens: config.value.maxTokens,
      temperature: config.value.temperature,
      status: config.value.status,
    }
  }
}

async function handleSave() {
  if (!form.value.projectName.trim()) {
    ElMessage.warning('请输入项目名称')
    return
  }
  if (!form.value.apiKey.trim() && !config.value) {
    ElMessage.warning('请输入 API Key')
    return
  }
  if (!form.value.apiEndpoint.trim()) {
    ElMessage.warning('请输入 API 端点')
    return
  }
  saving.value = true
  try {
    await saveAiConfig(form.value)
    ElMessage.success('保存成功')
    await fetchConfig()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    saving.value = false
  }
}

// ---- 已授权 ----
async function loadPermissions() {
  permLoading.value = true
  try {
    const res = await getPermissionsPage(currentPage.value, pageSize.value)
    permissions.value = res.records
    total.value = res.total
    pages.value = res.pages
  } catch {
    permissions.value = []
  } finally {
    permLoading.value = false
  }
}

function onPageChange(page: number) {
  currentPage.value = page
  loadPermissions()
}

async function handleRevoke(item: PermissionRecord) {
  try {
    await ElMessageBox.confirm(
      `确定撤销教师 ${item.teacherId} 的 AI 访问权限？`,
      '撤销确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deletePermission(item.teacherId)
    ElMessage.success('已撤销')
    if (permissions.value.length === 1 && currentPage.value > 1) {
      currentPage.value--
    }
    await loadPermissions()
  } catch {
    // 错误已在拦截器中处理
  }
}

// ---- 去授权 ----
async function handleSearch() {
  const kw = searchKeyword.value.trim()
  if (!kw) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  searching.value = true
  try {
    searchResults.value = await searchTeachers(kw)
  } catch {
    searchResults.value = []
  } finally {
    searching.value = false
  }
}

async function handleGrant(item: { id: number; userNo: string; realName: string; granted: boolean }) {
  try {
    await grantPermission(item.id)
    ElMessage.success('授权成功')
    item.granted = true
  } catch {
    // 错误已在拦截器中处理
  }
}

async function handleRevokeFromSearch(item: { id: number; userNo: string; realName: string; granted: boolean }) {
  try {
    await ElMessageBox.confirm(
      `确定撤销教师 ${item.realName}（${item.userNo}）的 AI 访问权限？`,
      '撤销确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deletePermission(item.id)
    ElMessage.success('已撤销')
    item.granted = false
  } catch {
    // 错误已在拦截器中处理
  }
}

// 切换到授权教师 tab 时刷新已授权列表
watch(activeTab, (tab) => {
  if (tab === 'teachers') {
    currentPage.value = 1
    loadPermissions()
  }
})

onMounted(fetchConfig)
</script>

<template>
  <AdminLayout>
    <div class="ai-config-page">
      <div class="page-header">
        <h1>AI 配置</h1>
        <p class="subtitle">管理 AI 助手的模型参数与教师访问权限</p>
      </div>

      <el-tabs v-model="activeTab" class="config-tabs">
        <el-tab-pane label="AI 配置" name="config">
          <div v-if="loading" class="loading-state">
            <div class="spinner"></div>
            <p>加载中...</p>
          </div>

          <div v-else class="form-card">
            <h2 class="form-title">{{ config ? '编辑 AI 配置' : '新建 AI 配置' }}</h2>
            <p class="form-desc">
              {{ config ? '修改以下参数后点击保存即可生效' : '请填写以下信息以启用 AI 服务' }}
            </p>

            <el-form label-position="top" class="config-form">
              <div class="form-grid">
                <el-form-item label="项目名称" required>
                  <el-input v-model="form.projectName" placeholder="例如：智学平台-DeepSeek" maxlength="100" />
                </el-form-item>
                <el-form-item label="供应商" required>
                  <el-select v-model="form.provider" style="width:100%">
                    <el-option v-for="p in providers" :key="p.value" :label="p.label" :value="p.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="API 端点" required>
                  <el-input v-model="form.apiEndpoint" placeholder="https://api.deepseek.com" />
                </el-form-item>
                <el-form-item label="模型名称" required>
                  <el-input v-model="form.modelName" placeholder="deepseek-chat" />
                </el-form-item>
                <el-form-item label="Embedding 模型" required>
                  <el-input v-model="form.embeddingModel" placeholder="deepseek-embedding" />
                </el-form-item>
                <el-form-item label="Embedding API Key">
                  <el-input
                    v-model="form.embeddingApiKey"
                    type="password"
                    show-password
                    placeholder="留空则复用上方 API Key"
                  />
                </el-form-item>
                <el-form-item label="Embedding API 端点">
                  <el-input v-model="form.embeddingApiEndpoint" placeholder="留空则复用上方 API 端点" />
                </el-form-item>
                <el-form-item label="API Key" required>
                  <el-input
                    v-model="form.apiKey"
                    type="password"
                    show-password
                    :placeholder="config ? '留空则不修改' : '请输入 API Key'"
                  />
                </el-form-item>
                <el-form-item label="状态">
                  <el-switch
                    v-model="form.status"
                    :active-value="1"
                    :inactive-value="0"
                    active-text="已启用"
                    inactive-text="已停用"
                  />
                </el-form-item>
                <el-form-item label="Max Tokens">
                  <el-input-number v-model="form.maxTokens" :min="1" :max="128000" :step="512" />
                </el-form-item>
                <el-form-item label="Temperature">
                  <el-slider
                    v-model="form.temperature"
                    :min="0"
                    :max="2"
                    :step="0.01"
                    show-input
                    input-size="small"
                  />
                </el-form-item>
              </div>

              <div class="form-actions">
                <el-button v-if="config" @click="resetForm" :disabled="saving">重置</el-button>
                <el-button type="primary" @click="handleSave" :loading="saving">
                  {{ saving ? '保存中...' : '保存配置' }}
                </el-button>
              </div>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane label="授权教师" name="teachers">
          <el-tabs v-model="teacherSubTab" class="inner-tabs">
            <!-- 已授权 -->
            <el-tab-pane label="已授权" name="granted">
              <div class="teachers-card">
                <div class="card-toolbar">
                  <span class="toolbar-hint">共 {{ total }} 条授权记录</span>
                </div>

                <div v-if="permLoading" class="list-state">
                  <div class="spinner"></div>
                  <p>加载中...</p>
                </div>

                <div v-else-if="permissions.length === 0" class="list-state">
                  <div class="empty-icon">
                    <svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5">
                      <circle cx="32" cy="22" r="10" />
                      <path d="M16 52c0-8.84 7.16-16 16-16s16 7.16 16 16" />
                    </svg>
                  </div>
                  <p>暂无已授权的教师</p>
                </div>

                <div v-else class="teacher-list">
                  <div v-for="item in permissions" :key="item.id" class="teacher-row">
                    <div class="teacher-info">
                      <span class="teacher-name">教师 #{{ item.teacherId }}</span>
                      <span class="teacher-no">授权于 {{ item.grantedBy }}</span>
                    </div>
                    <el-button type="danger" plain size="small" @click="handleRevoke(item)">
                      撤销权限
                    </el-button>
                  </div>
                </div>

                <div v-if="pages > 1" class="pagination-wrap">
                  <el-pagination
                    v-model:current-page="currentPage"
                    :page-size="pageSize"
                    :total="total"
                    layout="prev, pager, next"
                    @current-change="onPageChange"
                  />
                </div>
              </div>
            </el-tab-pane>

            <!-- 去授权 -->
            <el-tab-pane label="去授权" name="to-grant">
              <div class="teachers-card">
                <div class="search-section">
                  <el-input
                    v-model="searchKeyword"
                    placeholder="搜索教师姓名或工号"
                    clearable
                    class="search-input"
                    @keyup.enter="handleSearch"
                  >
                    <template #prefix>
                      <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" class="search-icon">
                        <circle cx="7" cy="7" r="5" />
                        <path d="M11 11l3 3" />
                      </svg>
                    </template>
                  </el-input>
                  <el-button type="primary" @click="handleSearch">搜索</el-button>
                </div>

                <div v-if="searching" class="list-state">
                  <div class="spinner"></div>
                  <p>搜索中...</p>
                </div>

                <div v-else-if="searchKeyword && searchResults.length === 0" class="list-state">
                  <div class="empty-icon">
                    <svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5">
                      <circle cx="32" cy="22" r="10" />
                      <path d="M16 52c0-8.84 7.16-16 16-16s16 7.16 16 16" />
                    </svg>
                  </div>
                  <p>未找到匹配的教师</p>
                </div>

                <div v-else-if="!searchKeyword" class="list-state">
                  <div class="empty-icon">
                    <svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5">
                      <circle cx="32" cy="22" r="10" />
                      <path d="M16 52c0-8.84 7.16-16 16-16s16 7.16 16 16" />
                    </svg>
                  </div>
                  <p>输入教师姓名或工号搜索并管理授权</p>
                </div>

                <div v-else class="teacher-list">
                  <div v-for="item in searchResults" :key="item.id" class="teacher-row">
                    <div class="teacher-info">
                      <span class="teacher-name">{{ item.realName }}</span>
                      <span class="teacher-no">{{ item.userNo }}</span>
                    </div>
                    <div class="row-actions">
                      <span v-if="item.granted" class="granted-tag granted">已授权</span>
                      <el-button
                        v-if="!item.granted"
                        type="primary"
                        size="small"
                        @click="handleGrant(item)"
                      >
                        授权
                      </el-button>
                      <el-button
                        v-if="item.granted"
                        type="danger"
                        plain
                        size="small"
                        @click="handleRevokeFromSearch(item)"
                      >
                        撤销权限
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-tab-pane>
      </el-tabs>
    </div>
  </AdminLayout>
</template>

<style scoped>
.ai-config-page {
  padding: 40px;
  max-width: 900px;
}

.page-header {
  margin-bottom: 28px;
}

.page-header h1 {
  font-size: 26px;
  font-weight: 700;
  color: #1a2b3e;
  margin: 0 0 6px;
}

.subtitle {
  color: #7a9bb5;
  font-size: 15px;
  margin: 0;
}

/* 外层 Tabs */
.config-tabs {
  --el-tabs-header-height: 48px;
}

.config-tabs :deep(.el-tabs__header) {
  margin-bottom: 28px;
}

.config-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: #e8f0f8;
}

.config-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 600;
  color: #7a9bb5;
  padding: 0 24px;
  height: 48px;
  line-height: 48px;
  transition: color 0.2s;
}

.config-tabs :deep(.el-tabs__item:hover) {
  color: #0066ff;
}

.config-tabs :deep(.el-tabs__item.is-active) {
  color: #0066ff;
}

.config-tabs :deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #0066ff, #00d4ff);
  height: 2.5px;
  border-radius: 2px;
}

/* 内层 Tabs */
.inner-tabs {
  --el-tabs-header-height: 40px;
}

.inner-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.inner-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.inner-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  font-weight: 500;
  color: #94b4cc;
  padding: 0 20px;
  height: 40px;
  line-height: 40px;
}

.inner-tabs :deep(.el-tabs__item:hover) {
  color: #0066ff;
}

.inner-tabs :deep(.el-tabs__item.is-active) {
  color: #0066ff;
  font-weight: 600;
}

.inner-tabs :deep(.el-tabs__active-bar) {
  background: #0066ff;
  height: 2px;
  border-radius: 2px;
}

/* 加载 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: #7a9bb5;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #e8f0fe;
  border-top-color: #0066ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-state p {
  margin: 0;
  font-size: 14px;
}

/* 表单卡片 */
.form-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 4px 16px rgba(0, 102, 255, 0.06);
  padding: 32px;
}

.form-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a2b3e;
  margin: 0 0 4px;
}

.form-desc {
  color: #94b4cc;
  font-size: 14px;
  margin: 0 0 28px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 24px;
}

.form-grid > :nth-child(8) {
  grid-column: 1 / -1;
}

.form-grid > :nth-child(9) {
  grid-column: 1 / -1;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 32px;
  padding-top: 20px;
  border-top: 1px solid #f0f5fa;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  font-size: 13px;
  color: #5a7a9a;
  padding-bottom: 6px;
}

:deep(.el-input__wrapper),
:deep(.el-select .el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e4edf5 inset;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c8dce8 inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #0066ff inset;
}

:deep(.el-switch__label) {
  font-size: 13px;
  color: #5a7a9a;
}

:deep(.el-switch__label.is-active) {
  color: #0066ff;
}

/* 教师卡片通用 */
.teachers-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 4px 16px rgba(0, 102, 255, 0.06);
  overflow: hidden;
}

.card-toolbar {
  display: flex;
  align-items: center;
  padding: 16px 28px;
  border-bottom: 1px solid #f0f5fa;
}

.toolbar-hint {
  font-size: 13px;
  color: #94b4cc;
}

.search-section {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 28px;
  border-bottom: 1px solid #f0f5fa;
}

.search-input {
  max-width: 360px;
}

.search-icon {
  width: 16px;
  height: 16px;
  color: #94b4cc;
}

/* 列表状态 */
.list-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 0;
  text-align: center;
  color: #94b4cc;
}

.list-state .empty-icon {
  width: 56px;
  height: 56px;
  color: #c8dce8;
  margin-bottom: 16px;
}

.list-state p {
  font-size: 14px;
  margin: 0;
}

/* 教师结果列表 */
.teacher-list {
  padding: 4px 0;
}

.teacher-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 28px;
  transition: background 0.15s;
}

.teacher-row:hover {
  background: #f8faff;
}

.teacher-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.teacher-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a2b3e;
}

.teacher-no {
  font-size: 13px;
  color: #94b4cc;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.granted-tag {
  font-size: 13px;
  font-weight: 600;
  padding: 5px 14px;
  border-radius: 20px;
}

.granted-tag.granted {
  background: #e8faf0;
  color: #0d9f6e;
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 16px 0 20px;
  border-top: 1px solid #f0f5fa;
}

.pagination-wrap :deep(.el-pager li) {
  border-radius: 8px;
  margin: 0 2px;
}

.pagination-wrap :deep(.el-pager li.is-active) {
  background: #0066ff;
  color: #fff;
}

/* 响应式 */
@media (max-width: 640px) {
  .ai-config-page {
    padding: 20px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-grid > :nth-child(8) {
    grid-column: 1;
  }

  .form-grid > :nth-child(9) {
    grid-column: 1;
  }

  .search-section {
    flex-direction: column;
    align-items: stretch;
  }

  .teacher-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
