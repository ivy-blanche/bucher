<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import TeacherLayout from '@/layouts/TeacherLayout.vue'
import {
  getExamList,
  getUnpublishedExamList,
  type ExamListItem,
  type UnpublishedExamItem,
} from '@/api/exam'

interface ExamDisplay extends ExamListItem {
  displayStatus: 'unpublished' | 'ongoing' | 'ended'
}

const router = useRouter()

const examList = ref<ExamDisplay[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const filters = [
  { key: 'all', label: '全部' },
  { key: 'unpublished', label: '未发布' },
  { key: '1', label: '进行中' },
  { key: '2', label: '已结束' },
]

const activeFilter = ref('all')

const statusConfig: Record<string, { label: string; color: string; bg: string }> = {
  unpublished: { label: '未发布', color: '#8c8c8c', bg: 'rgba(140, 140, 140, 0.1)' },
  ongoing: { label: '进行中', color: '#0066ff', bg: 'rgba(0, 102, 255, 0.1)' },
  ended: { label: '已结束', color: '#00b300', bg: 'rgba(0, 179, 0, 0.1)' },
}

function formatTime(dt: string): string {
  return dt.replace('T', ' ').slice(0, 16)
}

async function fetchExams() {
  loading.value = true
  try {
    if (activeFilter.value === 'unpublished') {
      const data = await getUnpublishedExamList({
        pageNum: pageNum.value,
        pageSize: pageSize.value,
      })
      examList.value = data.records.map((item: UnpublishedExamItem) => ({
        ...item,
        startTime: '',
        endTime: '',
        duration: 0,
        totalScore: 0,
        submitCount: 0,
        totalCount: 0,
        status: 0,
        displayStatus: 'unpublished' as const,
      }))
      total.value = data.total
    } else {
      const data = await getExamList({
        filterMode: activeFilter.value === 'all' ? undefined : activeFilter.value,
        pageNum: pageNum.value,
        pageSize: pageSize.value,
      })
      examList.value = data.records.map((item) => ({
        ...item,
        displayStatus: item.status === 1 ? 'ongoing' : 'ended',
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
  fetchExams()
}

function onPageChange(page: number) {
  pageNum.value = page
  fetchExams()
}

function handleCreate() {
  router.push('/teacher/exams/assign')
}

function handleView(item: ExamDisplay) {
  ElMessage.info(`查看考试：${item.title}`)
}

function handleEdit(item: ExamDisplay) {
  ElMessage.info(`编辑考试：${item.title}`)
}

function handlePublish(item: ExamDisplay) {
  ElMessage.info(`发布考试：${item.title}`)
}

function handleAnalysis(item: ExamDisplay) {
  ElMessage.info(`成绩分析：${item.title}`)
}

onMounted(() => {
  fetchExams()
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
          <span>创建考试</span>
        </button>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="empty-state">
        <p class="empty-text">加载中...</p>
      </div>

      <!-- 考试卡片列表 -->
      <template v-else>
        <div v-if="examList.length > 0" class="exam-list">
          <div
            v-for="exam in examList"
            :key="exam.id"
            class="exam-card"
            :style="{
              '--status-color': statusConfig[exam.displayStatus].color,
              '--status-bg': statusConfig[exam.displayStatus].bg,
            }"
          >
            <div class="card-body">
              <!-- 左侧信息 -->
              <div class="card-info">
                <div class="card-header-row">
                  <h3 class="exam-name">{{ exam.title }}</h3>
                  <span
                    class="status-tag"
                    :style="{
                      color: statusConfig[exam.displayStatus].color,
                      background: statusConfig[exam.displayStatus].bg,
                    }"
                  >
                    {{ statusConfig[exam.displayStatus].label }}
                  </span>
                </div>
                <div class="exam-meta">
                  <span class="meta-item">{{ exam.courseName }}</span>
                  <span class="meta-divider">|</span>
                  <span v-if="exam.displayStatus !== 'unpublished'" class="meta-item">
                    {{ formatTime(exam.startTime) }} ~ {{ formatTime(exam.endTime) }}
                  </span>
                  <span v-if="exam.duration > 0" class="meta-divider">|</span>
                  <span v-if="exam.duration > 0" class="meta-item">{{ exam.duration }} 分钟</span>
                  <span v-if="exam.totalScore > 0" class="meta-divider">|</span>
                  <span v-if="exam.totalScore > 0" class="meta-item">{{ exam.totalScore }} 分</span>
                </div>
                <div v-if="exam.displayStatus === 'unpublished'" class="unpublished-hint">
                  <svg viewBox="0 0 16 16" width="14" height="14" class="hint-icon">
                    <circle cx="8" cy="8" r="7" fill="none" stroke="currentColor" stroke-width="1.5"/>
                    <line x1="8" y1="5" x2="8" y2="9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                    <circle cx="8" cy="11.5" r="0.8" fill="currentColor"/>
                  </svg>
                  <span>待发布，学生不可见</span>
                </div>
                <div v-else class="submit-info">
                  <span class="submit-label">已提交 {{ exam.submitCount }}/{{ exam.totalCount }} 人</span>
                </div>
              </div>

              <!-- 右侧操作按钮 -->
              <div class="card-actions">
                <button class="action-btn action-view" @click="handleView(exam)">查看</button>
                <button
                  v-if="exam.displayStatus === 'unpublished'"
                  class="action-btn action-edit"
                  @click="handleEdit(exam)"
                >
                  编辑
                </button>
                <button
                  v-if="exam.displayStatus === 'unpublished'"
                  class="action-btn action-publish"
                  @click="handlePublish(exam)"
                >
                  发布
                </button>
                <button
                  v-if="exam.displayStatus === 'ended'"
                  class="action-btn action-analysis"
                  @click="handleAnalysis(exam)"
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
          <p class="empty-text">暂无考试</p>
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

/* ===== 考试卡片列表 ===== */
.exam-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.exam-card {
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

.exam-card:hover {
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

.exam-name {
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

.exam-meta {
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

.submit-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.submit-label {
  font-size: 13px;
  color: #7a9aba;
  font-weight: 500;
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
}

@media (max-width: 480px) {
  .exam-card {
    border-radius: 16px;
  }

  .card-body {
    padding: 20px 18px;
  }

  .exam-name {
    font-size: 15px;
  }

  .filter-tab {
    padding: 6px 14px;
    font-size: 13px;
  }
}
</style>
