<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import TeacherLayout from '@/layouts/TeacherLayout.vue'

const router = useRouter()
import {
  getHomeworkList,
  getUnpublishedHomeworkList,
  type HomeworkListItem,
  type UnpublishedHomeworkListItem,
} from '@/api/homework'

interface HomeworkDisplay extends HomeworkListItem {
  displayStatus: 'collecting' | 'pending' | 'ended' | 'unpublished'
}

const homeworkList = ref<HomeworkDisplay[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const filters = [
  { key: 'all', label: '全部' },
  { key: 'unpublished', label: '未发布' },
  { key: 'collecting', label: '收集中' },
  { key: 'pending', label: '待批改' },
  { key: 'ended', label: '已结束' },
]

const activeFilter = ref('all')

const statusConfig: Record<string, { label: string; color: string; bg: string }> = {
  unpublished: { label: '未发布', color: '#8c8c8c', bg: 'rgba(140, 140, 140, 0.1)' },
  collecting: { label: '收集中', color: '#0066ff', bg: 'rgba(0, 102, 255, 0.1)' },
  pending: { label: '待批改', color: '#ff8c00', bg: 'rgba(255, 140, 0, 0.1)' },
  ended: { label: '已结束', color: '#00b300', bg: 'rgba(0, 179, 0, 0.1)' },
}

function getDisplayStatus(status: number, gradingStatus: number): 'collecting' | 'pending' | 'ended' {
  if (status === 1 && gradingStatus === 0) return 'collecting'
  if (gradingStatus === 1) return 'pending'
  return 'ended'
}

async function fetchHomeworks() {
  loading.value = true
  try {
    if (activeFilter.value === 'unpublished') {
      const data = await getUnpublishedHomeworkList({
        pageNum: pageNum.value,
        pageSize: pageSize.value,
      })
      homeworkList.value = data.records.map((item) => ({
        ...item,
        submitCount: 0,
        gradedCount: 0,
        status: 0,
        gradingStatus: 0,
        displayStatus: 'unpublished' as const,
      }))
      total.value = data.total
    } else {
      const data = await getHomeworkList({
        filterMode: activeFilter.value,
        pageNum: pageNum.value,
        pageSize: pageSize.value,
      })
      homeworkList.value = data.records.map((item) => ({
        ...item,
        displayStatus: getDisplayStatus(item.status, item.gradingStatus),
      }))
      total.value = data.total
    }
  } catch {
    // 错误已由拦截器统一处理
  } finally {
    loading.value = false
  }
}

function onFilterChange(key: string) {
  activeFilter.value = key
  pageNum.value = 1
  fetchHomeworks()
}

function onPageChange(page: number) {
  pageNum.value = page
  fetchHomeworks()
}

function calcProgress(hw: HomeworkDisplay): number {
  if (hw.displayStatus === 'unpublished') return 0
  if (hw.displayStatus === 'pending') {
    return hw.submitCount > 0 ? Math.round((hw.gradedCount / hw.submitCount) * 100) : 0
  }
  if (hw.displayStatus === 'ended') {
    return 100
  }
  return 60
}

function progressLabel(hw: HomeworkDisplay): string {
  if (hw.displayStatus === 'unpublished') return ''
  if (hw.displayStatus === 'collecting') {
    return `已提交 ${hw.submitCount} 人`
  }
  if (hw.displayStatus === 'pending') {
    return `批改进度 ${hw.gradedCount}/${hw.submitCount} 份`
  }
  return `共提交 ${hw.submitCount} 人`
}

function handleCreate() {
  router.push('/teacher/homework/assign')
}

function handleView(item: HomeworkDisplay) {
  ElMessage.info(`查看作业：${item.title}`)
}

function handleGrade(item: HomeworkDisplay) {
  ElMessage.info(`去批改：${item.title}`)
}

function handleAnalysis(item: HomeworkDisplay) {
  ElMessage.info(`成绩分析：${item.title}`)
}

function handleEdit(item: HomeworkDisplay) {
  ElMessage.info(`编辑作业：${item.title}`)
}

function handlePublish(item: HomeworkDisplay) {
  ElMessage.info(`发布作业：${item.title}`)
}

onMounted(() => {
  fetchHomeworks()
})
</script>

<template>
  <TeacherLayout>
    <div class="page-container">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="filter-tabs">
          <button
            v-for="f in filters"
            :key="f.key"
            class="filter-tab"
            :class="{ active: activeFilter === f.key }"
            @click="onFilterChange(f.key)"
          >
            {{ f.label }}
          </button>
        </div>
        <button class="create-btn" @click="handleCreate">
          <svg viewBox="0 0 20 20" width="16" height="16" class="btn-icon">
            <line x1="10" y1="3" x2="10" y2="17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            <line x1="3" y1="10" x2="17" y2="10" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <span>布置作业</span>
        </button>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="empty-state">
        <p class="empty-text">加载中...</p>
      </div>

      <!-- 作业卡片列表 -->
      <template v-else>
        <div v-if="homeworkList.length > 0" class="homework-list">
          <div
            v-for="hw in homeworkList"
            :key="hw.id"
            class="homework-card"
            :style="{
              '--status-color': statusConfig[hw.displayStatus].color,
              '--status-bg': statusConfig[hw.displayStatus].bg,
            }"
          >
            <div class="card-body">
              <!-- 左侧信息 -->
              <div class="card-info">
                <div class="card-header-row">
                  <h3 class="hw-name">{{ hw.title }}</h3>
                  <span
                    class="status-tag"
                    :style="{
                      color: statusConfig[hw.displayStatus].color,
                      background: statusConfig[hw.displayStatus].bg,
                    }"
                  >
                    {{ statusConfig[hw.displayStatus].label }}
                  </span>
                </div>
                <div class="hw-meta">
                  <span class="meta-item">{{ hw.courseName }}</span>
                  <span class="meta-divider">|</span>
                  <span class="meta-item">截止 {{ hw.deadline }}</span>
                </div>
                <div
                  v-if="hw.displayStatus !== 'unpublished'"
                  class="progress-section"
                >
                  <div class="progress-bar-track">
                    <div
                      class="progress-bar-fill"
                      :style="{
                        width: calcProgress(hw) + '%',
                        background: statusConfig[hw.displayStatus].color,
                      }"
                    ></div>
                  </div>
                  <span class="progress-label" :style="{ color: statusConfig[hw.displayStatus].color }">
                    {{ progressLabel(hw) }}
                  </span>
                </div>
                <div v-else class="unpublished-hint">
                  <svg viewBox="0 0 16 16" width="14" height="14" class="hint-icon">
                    <circle cx="8" cy="8" r="7" fill="none" stroke="currentColor" stroke-width="1.5"/>
                    <line x1="8" y1="5" x2="8" y2="9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                    <circle cx="8" cy="11.5" r="0.8" fill="currentColor"/>
                  </svg>
                  <span>待发布，学生不可见</span>
                </div>
              </div>

              <!-- 右侧操作按钮 -->
              <div class="card-actions">
                <button class="action-btn action-view" @click="handleView(hw)">查看</button>
                <button
                  v-if="hw.displayStatus === 'unpublished'"
                  class="action-btn action-edit"
                  @click="handleEdit(hw)"
                >
                  编辑
                </button>
                <button
                  v-if="hw.displayStatus === 'unpublished'"
                  class="action-btn action-publish"
                  @click="handlePublish(hw)"
                >
                  发布
                </button>
                <button
                  v-if="hw.displayStatus === 'pending'"
                  class="action-btn action-grade"
                  @click="handleGrade(hw)"
                >
                  去批改
                </button>
                <button
                  v-if="hw.displayStatus === 'ended'"
                  class="action-btn action-analysis"
                  @click="handleAnalysis(hw)"
                >
                  成绩分析
                </button>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="pageNum"
              v-model:page-size="pageSize"
              :total="total"
              :page-size="10"
              layout="total, prev, pager, next"
              background
              small
              @current-change="onPageChange"
            />
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-state">
          <svg viewBox="0 0 80 80" width="80" height="80" class="empty-icon">
            <rect x="10" y="15" width="60" height="50" rx="4" fill="none" stroke="#b0c8e0" stroke-width="2"/>
            <line x1="10" y1="28" x2="70" y2="28" stroke="#b0c8e0" stroke-width="1.5"/>
            <line x1="26" y1="38" x2="60" y2="38" stroke="#b0c8e0" stroke-width="1.5" opacity="0.5"/>
            <line x1="26" y1="48" x2="50" y2="48" stroke="#b0c8e0" stroke-width="1.5" opacity="0.5"/>
          </svg>
          <p class="empty-text">暂无作业</p>
        </div>
      </template>
    </div>
  </TeacherLayout>
</template>

<style scoped>
.page-container {
  max-width: 1200px;
  margin: 0 auto;
}

/* ===== 工具栏 ===== */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
}

.filter-tabs {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 12px;
  border: 1px solid rgba(0, 102, 255, 0.08);
}

.filter-tab {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #5a7a9a;
  background: transparent;
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
}

.filter-tab:hover {
  color: #0066ff;
  background: rgba(0, 102, 255, 0.06);
}

.filter-tab.active {
  color: #fff;
  background: linear-gradient(135deg, #0066ff 0%, #00d4ff 100%);
  box-shadow: 0 2px 8px rgba(0, 102, 255, 0.2);
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

/* ===== 作业卡片列表 ===== */
.homework-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.homework-card {
  background: rgba(255, 255, 255, 0.75);
  border-radius: 20px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow:
    0 8px 24px rgba(0, 102, 255, 0.06),
    0 2px 8px rgba(0, 102, 255, 0.03);
  overflow: hidden;
  transition: all 0.3s ease;
}

.homework-card:hover {
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(0, 102, 255, 0.15);
  transform: translateY(-4px);
  box-shadow:
    0 16px 40px rgba(0, 102, 255, 0.12),
    0 4px 16px rgba(0, 102, 255, 0.06);
}

.card-body {
  display: flex;
  align-items: stretch;
  padding: 24px 28px;
  gap: 24px;
}

/* ===== 左侧信息 ===== */
.card-info {
  flex: 1;
  min-width: 0;
}

.card-header-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.hw-name {
  font-size: 17px;
  font-weight: 600;
  color: #1a3a5a;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-tag {
  flex-shrink: 0;
  display: inline-block;
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  line-height: 22px;
  white-space: nowrap;
}

.hw-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 16px;
}

.meta-item {
  font-size: 13px;
  color: #7a9aba;
}

.meta-divider {
  font-size: 12px;
  color: #c0d0e0;
  margin: 0 4px;
}

/* ===== 进度条 ===== */
.progress-section {
  display: flex;
  align-items: center;
  gap: 14px;
}

.progress-bar-track {
  flex: 1;
  max-width: 300px;
  height: 8px;
  background: rgba(0, 102, 255, 0.08);
  border-radius: 4px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.progress-label {
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  flex-shrink: 0;
}

/* ===== 未发布提示 ===== */
.unpublished-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #a0b8d0;
}

.hint-icon {
  flex-shrink: 0;
}

/* ===== 右侧操作按钮 ===== */
.card-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.action-btn {
  padding: 8px 18px;
  border: 1px solid rgba(0, 102, 255, 0.15);
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
}

.action-view {
  background: rgba(0, 102, 255, 0.06);
  color: #0066ff;
}

.action-view:hover {
  background: rgba(0, 102, 255, 0.12);
  transform: translateY(-1px);
}

.action-grade {
  background: rgba(255, 140, 0, 0.08);
  color: #ff8c00;
  border-color: rgba(255, 140, 0, 0.2);
}

.action-grade:hover {
  background: rgba(255, 140, 0, 0.16);
  transform: translateY(-1px);
}

.action-analysis {
  background: rgba(0, 179, 0, 0.08);
  color: #00b300;
  border-color: rgba(0, 179, 0, 0.2);
}

.action-analysis:hover {
  background: rgba(0, 179, 0, 0.16);
  transform: translateY(-1px);
}

.action-edit {
  background: rgba(140, 140, 140, 0.08);
  color: #8c8c8c;
  border-color: rgba(140, 140, 140, 0.2);
}

.action-edit:hover {
  background: rgba(140, 140, 140, 0.16);
  transform: translateY(-1px);
}

.action-publish {
  background: rgba(0, 102, 255, 0.08);
  color: #0066ff;
  border-color: rgba(0, 102, 255, 0.2);
}

.action-publish:hover {
  background: rgba(0, 102, 255, 0.16);
  transform: translateY(-1px);
}

/* ===== 分页 ===== */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 8px 0 4px;
}

/* ===== 空状态/加载中 ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-icon {
  margin-bottom: 20px;
}

.empty-text {
  font-size: 15px;
  color: #7a9aba;
  margin: 0;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .filter-tabs {
    justify-content: center;
  }

  .create-btn {
    justify-content: center;
  }

  .card-body {
    flex-direction: column;
    gap: 16px;
  }

  .card-actions {
    justify-content: flex-end;
  }

  .progress-bar-track {
    max-width: 100%;
  }
}

@media (max-width: 480px) {
  .homework-card {
    border-radius: 16px;
  }

  .card-body {
    padding: 20px 18px;
  }

  .hw-name {
    font-size: 15px;
  }

  .filter-tab {
    padding: 6px 14px;
    font-size: 13px;
  }
}
</style>
