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
  { key: 'departments', label: '院系管理' },
  { key: 'students', label: '学生管理' },
  { key: 'teachers', label: '教师管理' },
  { key: 'learning-status', label: '学习情况' },
  { key: 'settings', label: '机构设置' },
]

const activeTab = computed(() => {
  const path = route.path
  if (path.includes('/admin/departments')) return 'departments'
  if (path.includes('/admin/students')) return 'students'
  if (path.includes('/admin/teachers')) return 'teachers'
  if (path.includes('/admin/learning-status')) return 'learning-status'
  if (path.includes('/admin/settings')) return 'settings'
  return 'departments'
})

const routeMap: Record<string, string> = {
  departments: '/admin/departments',
  students: '/admin/students',
  teachers: '/admin/teachers',
  'learning-status': '/admin/learning-status',
  settings: '/admin/settings',
}

function onMenuSelect(key: string) {
  router.push(routeMap[key])
}
</script>

<template>
  <div class="admin-layout">
    <AppHeader />

    <div class="admin-body">
      <!-- 左侧导航栏 -->
      <aside class="admin-sidebar">
        <nav class="sidebar-nav">
          <div
            v-for="item in menuItems"
            :key="item.key"
            class="nav-item"
            :class="{ active: activeTab === item.key }"
            @click="onMenuSelect(item.key)"
          >
            <div class="nav-icon">
              <!-- 院系管理 — 建筑/组织图标 -->
              <svg v-if="item.key === 'departments'" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="2" y="2" width="7" height="7" rx="1" />
                <rect x="11" y="2" width="7" height="7" rx="1" />
                <rect x="2" y="11" width="7" height="7" rx="1" />
                <rect x="11" y="11" width="7" height="7" rx="1" />
              </svg>
              <!-- 学生管理 — 用户图标 -->
              <svg v-if="item.key === 'students'" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="8" cy="6" r="3" />
                <path d="M2 18c0-3.31 2.69-6 6-6s6 2.69 6 6" />
                <circle cx="15" cy="6" r="2" />
                <path d="M12 16c0-1.66 1.12-3 2.5-3s2.5 1.34 2.5 3" />
              </svg>
              <!-- 教师管理 — 学术帽图标 -->
              <svg v-if="item.key === 'teachers'" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M2 7L10 3l8 4-8 4-8-4z" />
                <path d="M5 9v4l5 3 5-3V9" />
                <circle cx="10" cy="8" r="2" />
              </svg>
              <!-- 学习情况 — 图表图标 -->
              <svg v-if="item.key === 'learning-status'" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="3" y="13" width="3" height="5" rx="0.5" />
                <rect x="8.5" y="9" width="3" height="9" rx="0.5" />
                <rect x="14" y="5" width="3" height="13" rx="0.5" />
              </svg>
              <!-- 机构设置 — 齿轮图标 -->
              <svg v-if="item.key === 'settings'" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="10" cy="10" r="3" />
                <path d="M10 2v2M10 16v2M2 10h2M16 10h2M4.93 4.93l1.41 1.41M13.66 13.66l1.41 1.41M4.93 15.07l1.41-1.41M13.66 6.34l1.41-1.41" />
              </svg>
            </div>
            <span class="nav-label">{{ item.label }}</span>
          </div>
        </nav>
      </aside>

      <!-- 右侧内容区 -->
      <main class="admin-content">
        <slot />
      </main>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap');

.admin-layout {
  min-height: 100vh;
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: linear-gradient(135deg, #e0f7ff 0%, #b3e5fc 25%, #e1f5fe 50%, #e3f2fd 75%, #f5f9ff 100%);
  background-attachment: fixed;
}

.admin-body {
  display: flex;
  padding-top: 64px;
  min-height: calc(100vh - 64px);
}

/* 左侧导航栏 */
.admin-sidebar {
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
.admin-content {
  flex: 1;
  overflow-y: auto;
}

/* 响应式 */
@media (max-width: 768px) {
  .admin-sidebar {
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
}

@media (max-width: 480px) {
  .admin-sidebar {
    width: 56px;
    padding: 16px 4px;
  }

  .nav-item {
    padding: 10px 4px;
  }
}
</style>
