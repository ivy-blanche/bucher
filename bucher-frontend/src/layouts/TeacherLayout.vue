<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'

const route = useRoute()
const router = useRouter()

interface MenuItem {
  key: string
  label: string
}

const menuItems: MenuItem[] = [
  { key: 'courses', label: '课程' },
  { key: 'question-bank', label: '题库' },
  { key: 'homework', label: '作业' },
  { key: 'exams', label: '考试' },
]

const activeTab = computed(() => {
  const path = route.path
  if (path.includes('/teacher/question-bank')) return 'question-bank'
  if (path.includes('/teacher/homework')) return 'homework'
  if (path.includes('/teacher/exams')) return 'exams'
  return 'courses'
})

const routeMap: Record<string, string> = {
  courses: '/teacher/courses',
  'question-bank': '/teacher/question-bank',
  homework: '/teacher/homework',
  exams: '/teacher/exams',
}

function onMenuSelect(key: string) {
  router.push(routeMap[key])
}
</script>

<template>
  <div class="teacher-layout">
    <AppHeader />

    <div class="teacher-body">
      <!-- 左侧导航栏 -->
      <aside class="teacher-sidebar">
        <nav class="sidebar-nav">
          <div
            v-for="item in menuItems"
            :key="item.key"
            class="nav-item"
            :class="{ active: activeTab === item.key }"
            @click="onMenuSelect(item.key)"
          >
            <div class="nav-icon">
              <!-- 课程 -->
              <svg v-if="item.key === 'courses'" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="2" y="2" width="7" height="7" rx="1" />
                <rect x="11" y="2" width="7" height="7" rx="1" />
                <rect x="2" y="11" width="7" height="7" rx="1" />
                <rect x="11" y="11" width="7" height="7" rx="1" />
              </svg>
              <!-- 题库 -->
              <svg v-if="item.key === 'question-bank'" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="10" cy="7" r="3"/>
                <path d="M3 16c0-3 3-5 7-5s7 2 7 5"/>
              </svg>
              <!-- 作业 -->
              <svg v-if="item.key === 'homework'" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M5 2h10l3 3v13a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1z"/>
                <path d="M15 2v4h4M7 10h8M7 13h8M7 16h5"/>
              </svg>
              <!-- 考试 -->
              <svg v-if="item.key === 'exams'" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M4 4h12v2H4zM4 9h12v2H4zM4 14h8v2H4z"/>
              </svg>
            </div>
            <span class="nav-label">{{ item.label }}</span>
          </div>
        </nav>
      </aside>

      <!-- 右侧内容区 -->
      <main class="teacher-content">
        <slot />
      </main>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap');

.teacher-layout {
  min-height: 100vh;
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: linear-gradient(135deg, #e0f7ff 0%, #b3e5fc 25%, #e1f5fe 50%, #e3f2fd 75%, #f5f9ff 100%);
  background-attachment: fixed;
}

.teacher-body {
  display: flex;
  padding-top: 64px;
  min-height: calc(100vh - 64px);
}

/* 左侧导航栏 */
.teacher-sidebar {
  width: 220px;
  flex-shrink: 0;
  padding: 24px 16px;
  border-right: 1px solid rgba(0, 102, 255, 0.06);
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  cursor: pointer;
  color: #5a7a9a;
  transition: all 0.2s ease;
  font-size: 15px;
  font-weight: 500;
}

.nav-item:hover {
  background: rgba(0, 102, 255, 0.06);
  color: #0066ff;
}

.nav-item.active {
  background: linear-gradient(135deg, rgba(0, 102, 255, 0.1) 0%, rgba(0, 212, 255, 0.08) 100%);
  color: #0066ff;
  font-weight: 600;
}

.nav-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.nav-icon svg {
  width: 18px;
  height: 18px;
}

.nav-label {
  white-space: nowrap;
}

/* 右侧内容区 */
.teacher-content {
  flex: 1;
  padding: 32px 40px;
  overflow-y: auto;
}

/* 响应式 */
@media (max-width: 768px) {
  .teacher-sidebar {
    width: 72px;
    padding: 20px 8px;
  }

  .nav-item {
    justify-content: center;
    padding: 12px 8px;
  }

  .nav-label {
    display: none;
  }

  .teacher-content {
    padding: 24px 20px;
  }
}

@media (max-width: 480px) {
  .teacher-sidebar {
    width: 56px;
    padding: 16px 4px;
  }

  .nav-item {
    padding: 10px 4px;
  }
}
</style>
