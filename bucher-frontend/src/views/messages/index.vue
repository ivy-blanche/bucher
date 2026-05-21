<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import MainLayout from '@/layouts/MainLayout.vue'
import { getNotifications, markNotificationsRead, type NotificationVO } from '@/api/message'

const router = useRouter()
const activeTab = ref<1 | 2>(1)
const notifications = ref<NotificationVO[]>([])
const loading = ref(true)

onMounted(async () => {
  loading.value = true
  try {
    notifications.value = await getNotifications()
    // 标记已读，下次拉取时角标会更新为 0
    markNotificationsRead().catch(() => {})
  } finally {
    loading.value = false
  }
})

const filteredList = computed(() =>
  notifications.value.filter((n) => n.type === activeTab.value)
)

interface DateGroup {
  date: string
  items: NotificationVO[]
}

const groupedList = computed(() => {
  const groups: DateGroup[] = []
  const map = new Map<string, NotificationVO[]>()
  for (const item of filteredList.value) {
    const day = dayjs(item.createTime).format('YYYY-MM-DD')
    if (!map.has(day)) map.set(day, [])
    map.get(day)!.push(item)
  }
  const sortedDays = Array.from(map.keys()).sort((a, b) => (a > b ? -1 : 1))
  for (const day of sortedDays) {
    groups.push({ date: day, items: map.get(day)! })
  }
  return groups
})

function formatTime(iso: string) {
  return dayjs(iso).format('HH:mm')
}

function formatDatetime(iso: string) {
  return dayjs(iso).format('YYYY-MM-DD HH:mm')
}

/** 根据 type 组装模板文本 */
function assembleContent(item: NotificationVO): string {
  if (item.type === 1) {
    return `${item.teacherName}教师 在 ${item.courseName} 发布了新的作业【${item.bizTitle}】，提交截止时间 ${formatDatetime(item.deadline)}，请按时提交作业`
  }
  // type === 2
  const start = item.startTime ? formatDatetime(item.startTime) : ''
  return `${item.teacherName}教师 在 ${item.courseName} 发布了新的考试【${item.bizTitle}】，考试时间 ${start} - ${formatDatetime(item.deadline)}，请按时参加考试`
}

function getTitle(item: NotificationVO): string {
  return `【${item.courseName}】${item.bizTitle}`
}

function handleClick(item: NotificationVO) {
  if (item.type === 1) {
    router.push(`/homework/${item.relatedId}`)
  } else {
    router.push(`/exam/${item.relatedId}`)
  }
}
</script>

<template>
  <MainLayout>
    <div class="messages-page">
      <div class="messages-card">
        <!-- 标题 -->
        <div class="page-header">
          <h2 class="page-title">消息通知</h2>
        </div>

        <!-- Tab 切换 -->
        <div class="tab-bar">
          <button
            :class="['tab-btn', { active: activeTab === 1 }]"
            @click="activeTab = 1"
          >
            <svg class="tab-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
            </svg>
            作业通知
          </button>
          <button
            :class="['tab-btn', { active: activeTab === 2 }]"
            @click="activeTab = 2"
          >
            <svg class="tab-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 11a3 3 0 1 0 6 0a3 3 0 0 0 -6 0"/>
              <path d="M17.657 16.657l-4.243 4.243a2 2 0 0 1-2.827 0l-4.244-4.243a8 8 0 1 1 11.314 0z"/>
            </svg>
            考试通知
          </button>
        </div>

        <!-- 通知列表 -->
        <div class="notification-list">
          <div v-if="loading" class="empty-state">
            <div class="loading-spinner"></div>
            <p>加载中...</p>
          </div>

          <template v-else-if="groupedList.length === 0">
            <div class="empty-state">
              <svg class="empty-icon" viewBox="0 0 80 80" fill="none">
                <circle cx="40" cy="40" r="32" stroke="#d0dce8" stroke-width="2" />
                <path d="M26 28h28v24H26V28z" stroke="#d0dce8" stroke-width="1.5" rx="2" />
                <line x1="32" y1="36" x2="48" y2="36" stroke="#d0dce8" stroke-width="1.5" stroke-linecap="round" />
                <line x1="32" y1="42" x2="44" y2="42" stroke="#d0dce8" stroke-width="1.5" stroke-linecap="round" />
                <line x1="32" y1="48" x2="40" y2="48" stroke="#d0dce8" stroke-width="1.5" stroke-linecap="round" />
              </svg>
              <p class="empty-text">暂无通知</p>
            </div>
          </template>

          <template v-else>
            <div
              v-for="group in groupedList"
              :key="group.date"
              class="date-group"
            >
              <!-- 日期分隔线 -->
              <div class="date-separator">
                <span class="date-label">{{ group.date }}</span>
                <span class="date-line"></span>
              </div>

              <div
                v-for="item in group.items"
                :key="item.id"
                :class="['notification-item', { unread: !item.isRead }]"
                @click="handleClick(item)"
              >
                <div class="item-left">
                  <!-- 未读圆点 -->
                  <span v-if="!item.isRead" class="unread-dot"></span>
                  <div :class="['item-icon', item.type === 1 ? 'homework' : 'exam']">
                    <svg v-if="item.type === 1" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/>
                      <polyline points="14 2 14 8 20 8"/>
                      <line x1="16" y1="13" x2="8" y2="13"/>
                      <line x1="16" y1="17" x2="8" y2="17"/>
                    </svg>
                    <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <path d="M9 11a3 3 0 1 0 6 0a3 3 0 0 0 -6 0"/>
                      <path d="M17.657 16.657l-4.243 4.243a2 2 0 0 1-2.827 0l-4.244-4.243a8 8 0 1 1 11.314 0z"/>
                    </svg>
                  </div>
                </div>
                <div class="item-body">
                  <div class="item-header">
                    <span class="item-title">{{ getTitle(item) }}</span>
                    <span class="item-time">{{ formatTime(item.createTime) }}</span>
                  </div>
                  <p class="item-content">{{ assembleContent(item) }}</p>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap');

.messages-page {
  min-height: calc(100vh - 64px);
  display: flex;
  justify-content: center;
  padding: 32px 20px;
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.messages-card {
  width: 100%;
  max-width: 720px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow:
    0 15px 50px rgba(0, 102, 255, 0.08),
    0 5px 20px rgba(0, 102, 255, 0.04);
  overflow: hidden;
  align-self: flex-start;
}

.page-header {
  padding: 28px 32px 0;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a3a5a;
  margin: 0;
  letter-spacing: 0.5px;
}

.tab-bar {
  display: flex;
  gap: 4px;
  padding: 20px 32px 0;
  border-bottom: 1px solid rgba(0, 102, 255, 0.06);
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border: none;
  background: transparent;
  font-size: 14px;
  font-weight: 500;
  color: #7a9aba;
  cursor: pointer;
  position: relative;
  transition: all 0.3s ease;
  font-family: inherit;
}

.tab-btn:hover {
  color: #3a5a7a;
}

.tab-btn.active {
  color: #0066ff;
}

.tab-btn.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, #0066ff, #00d4ff);
  border-radius: 2px 2px 0 0;
}

.tab-icon {
  width: 18px;
  height: 18px;
}

.notification-list {
  padding: 8px 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 32px;
  color: #a0b8d0;
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

.empty-icon {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 15px;
  color: #a0b8d0;
  margin: 0;
}

.date-group {
  padding: 0;
}

.date-separator {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 32px 8px;
}

.date-label {
  font-size: 12px;
  font-weight: 600;
  color: #a0b8d0;
  white-space: nowrap;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.date-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, rgba(0, 102, 255, 0.08), transparent);
}

.notification-item {
  display: flex;
  gap: 16px;
  padding: 16px 32px;
  transition: background 0.2s ease;
  cursor: pointer;
  position: relative;
}

.notification-item:hover {
  background: rgba(0, 102, 255, 0.02);
}

.notification-item + .notification-item {
  border-top: 1px solid rgba(0, 102, 255, 0.04);
}

.item-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  padding-top: 2px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0066ff, #00d4ff);
  flex-shrink: 0;
  box-shadow: 0 0 6px rgba(0, 102, 255, 0.4);
}

.notification-item:not(.unread) .unread-dot {
  display: none;
}

.item-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.item-icon.homework {
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.12), rgba(0, 102, 255, 0.08));
  color: #0066ff;
}

.item-icon.exam {
  background: linear-gradient(135deg, rgba(255, 180, 0, 0.12), rgba(255, 140, 0, 0.08));
  color: #e68a00;
}

.item-icon svg {
  width: 20px;
  height: 20px;
}

.item-body {
  flex: 1;
  min-width: 0;
}

.item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}

.item-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a3a5a;
  line-height: 1.4;
}

.notification-item.unread .item-title {
  color: #0a2540;
}

.item-time {
  font-size: 12px;
  color: #b0c4d8;
  white-space: nowrap;
  flex-shrink: 0;
}

.item-content {
  font-size: 14px;
  color: #5a7a9a;
  margin: 0;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

@media (max-width: 768px) {
  .messages-page {
    padding: 16px 12px;
  }

  .messages-card {
    border-radius: 16px;
  }

  .page-header {
    padding: 20px 20px 0;
  }

  .page-title {
    font-size: 20px;
  }

  .tab-bar {
    padding: 16px 20px 0;
  }

  .tab-btn {
    padding: 10px 16px;
    font-size: 13px;
  }

  .notification-item {
    padding: 14px 20px;
    gap: 12px;
  }

  .date-separator {
    padding: 16px 20px 4px;
  }

  .item-title {
    font-size: 14px;
  }

  .item-content {
    font-size: 13px;
  }

  .item-icon {
    width: 36px;
    height: 36px;
  }

  .item-icon svg {
    width: 18px;
    height: 18px;
  }
}
</style>
