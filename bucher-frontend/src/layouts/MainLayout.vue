<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAIStore } from '@/stores/ai'
import AppHeader from '@/components/AppHeader.vue'
import AIChat from '@/components/AIChat.vue'
import AIChatToggle from '@/components/AIChatToggle.vue'

const route = useRoute()
const aiStore = useAIStore()

const currentCourseId = computed(() => route.params.id as string || '')
const currentCourseName = computed(() => (route.query.name as string) || '')
</script>

<template>
  <div class="main-layout">
    <!-- 顶部导航栏 -->
    <AppHeader />

    <!-- 主内容区域（inline 布局，AI 侧边栏推动内容） -->
    <div class="main-body">
      <main class="main-content">
        <slot />
      </main>

      <AIChat
        mode="embedded"
        :visible="aiStore.visible"
        :course-id="currentCourseId"
        :course-name="currentCourseName"
        @update:visible="aiStore.closeOverlay()"
      />
    </div>

    <!-- AI 助手浮动按钮 -->
    <AIChatToggle />
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap');

.main-layout {
  min-height: 100vh;
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: linear-gradient(135deg, #e0f7ff 0%, #b3e5fc 25%, #e1f5fe 50%, #e3f2fd 75%, #f5f9ff 100%);
  background-attachment: fixed;
}

.main-body {
  display: flex;
  padding-top: 64px;
  min-height: 100vh;
}

.main-content {
  flex: 1;
  min-width: 0;
}
</style>
