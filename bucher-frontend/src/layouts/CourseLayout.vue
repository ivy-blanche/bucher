<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAIStore } from '@/stores/ai'
import AppHeader from '@/components/AppHeader.vue'
import AIChat from '@/components/AIChat.vue'
import AIChatToggle from '@/components/AIChatToggle.vue'

const route = useRoute()
const router = useRouter()
const aiStore = useAIStore()

const activeTab = computed(() => (route.query.tab as string) || 'chapters')

const currentCourseName = computed(() => (route.query.name as string) || aiStore.courseName)

interface MenuItem {
  key: string
  label: string
  icon: string
}

const menuItems: MenuItem[] = [
  { key: 'chapters', label: '课程章节', icon: 'chapters' },
  { key: 'homework', label: '作业', icon: 'homework' },
  { key: 'lab', label: '实验', icon: 'lab' },
  { key: 'exam', label: '考试', icon: 'exam' },
  { key: 'material', label: '资料', icon: 'material' },
  { key: 'ai', label: 'AI 助教', icon: 'ai' },
]

function onMenuSelect(key: string) {
  router.push({
    name: 'courseDetail',
    params: { id: route.params.id },
    query: { ...route.query, tab: key },
  })
}
</script>

<template>
  <div class="course-layout">
    <AppHeader />

    <div class="course-body">
      <!-- 左侧导航栏 -->
      <aside class="course-sidebar">
        <nav class="sidebar-nav">
          <div
            v-for="item in menuItems"
            :key="item.key"
            class="nav-item"
            :class="{ active: activeTab === item.key }"
            @click="onMenuSelect(item.key)"
          >
            <!-- 图标 -->
            <div class="nav-icon">
              <!-- 课程章节图标 -->
              <svg v-if="item.key === 'chapters'" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M2 4h16M2 10h16M2 16h10"/>
              </svg>
              <!-- 作业图标 -->
              <svg v-if="item.key === 'homework'" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M5 2h10l3 3v13a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1z"/>
                <path d="M15 2v4h4M7 10h8M7 13h8M7 16h5"/>
              </svg>
              <!-- 考试图标 -->
              <svg v-if="item.key === 'exam'" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="3" y="2" width="14" height="16" rx="1"/>
                <line x1="6" y1="6" x2="14" y2="6"/>
                <line x1="6" y1="9" x2="12" y2="9"/>
                <line x1="6" y1="12" x2="14" y2="12"/>
                <line x1="6" y1="15" x2="10" y2="15"/>
              </svg>
              <!-- 实验图标 -->
              <svg v-if="item.key === 'lab'" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M7 2v6l-4 8h14l-4-8V2"/>
                <line x1="7" y1="10" x2="13" y2="10"/>
              </svg>
              <!-- 资料图标 -->
              <svg v-if="item.key === 'material'" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M4 4h12v2H4zM4 9h12v2H4zM4 14h8v2H4z"/>
              </svg>
              <!-- AI 助教图标 -->
              <svg v-if="item.key === 'ai'" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="10" cy="10" r="8"/>
                <path d="M10 5v5l3 1.5" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M6 12.5c0 1.2 1.5 2.5 4 2.5s4-1.3 4-2.5" stroke-linecap="round"/>
              </svg>
            </div>
            <span class="nav-label">{{ item.label }}</span>
          </div>
        </nav>
      </aside>

      <!-- 右侧内容区 -->
      <div class="course-content-wrapper">
        <main class="course-content">
          <slot />
        </main>

        <AIChat
          mode="embedded"
          :visible="aiStore.visible"
          :course-id="(route.params.id as string)"
          :course-name="currentCourseName"
          @update:visible="aiStore.closeOverlay()"
        />
      </div>
    </div>

    <!-- AI 助手浮动按钮 -->
    <AIChatToggle />
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap');

.course-layout {
  min-height: 100vh;
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: linear-gradient(135deg, #e0f7ff 0%, #b3e5fc 25%, #e1f5fe 50%, #e3f2fd 75%, #f5f9ff 100%);
  background-attachment: fixed;
}

.course-body {
  display: flex;
  padding-top: 64px;
  min-height: calc(100vh - 64px);
}

/* 左侧导航栏 */
.course-sidebar {
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
.course-content-wrapper {
  flex: 1;
  display: flex;
  min-width: 0;
}

.course-content {
  flex: 1;
  padding: 32px 40px;
  overflow-y: auto;
  min-width: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .course-sidebar {
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

  .course-content {
    padding: 24px 20px;
  }
}

@media (max-width: 480px) {
  .course-sidebar {
    width: 56px;
    padding: 16px 4px;
  }

  .nav-item {
    padding: 10px 4px;
  }
}
</style>
