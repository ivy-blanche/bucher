<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAIStore } from '@/stores/ai'
import AIChat from '@/components/AIChat.vue'

const route = useRoute()
const router = useRouter()
const store = useAIStore()

const courseId = route.params.id as string
const courseName = (route.query.name as string) || ''

onMounted(() => {
  store.initCourse(courseId, courseName)
  store.setFullMode()
})

function goBack() {
  router.back()
}
</script>

<template>
  <div class="ai-full-page">
    <!-- 页面头部 -->
    <div class="ai-page-header">
      <div class="ai-page-header-inner">
        <button class="back-btn" @click="goBack">
          <svg viewBox="0 0 16 16" width="16" height="16">
            <polyline points="10,3 5,8 10,13" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <span>返回</span>
        </button>
        <div class="ai-page-title">
          <div class="title-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="12" cy="12" r="10" />
              <path d="M12 6v6l4 2" stroke-linecap="round" stroke-linejoin="round" />
              <path d="M8 14c0 2 1.5 3.5 4 3.5s4-1.5 4-3.5" stroke-linecap="round" />
            </svg>
          </div>
          <div class="title-text">
            <h1>AI 助教</h1>
            <p v-if="courseName">{{ courseName }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 聊天区域 -->
    <div class="ai-page-body">
      <AIChat
        mode="full"
        :course-id="courseId"
        :course-name="courseName"
      />
    </div>
  </div>
</template>

<style scoped>
.ai-full-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px);
  background: linear-gradient(135deg, #e0f7ff 0%, #b3e5fc 25%, #e1f5fe 50%, #e3f2fd 75%, #f5f9ff 100%);
}

.ai-page-header {
  flex-shrink: 0;
  padding: 20px 32px 0;
}

.ai-page-header-inner {
  max-width: 860px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid rgba(0, 102, 255, 0.12);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.7);
  color: #0066ff;
  font-size: 14px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
  align-self: flex-start;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(0, 102, 255, 0.25);
}

.ai-page-title {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(0, 102, 255, 0.1), rgba(0, 212, 255, 0.08));
  color: #0066ff;
}

.title-icon svg {
  width: 26px;
  height: 26px;
}

.title-text h1 {
  font-size: 22px;
  font-weight: 700;
  color: #1a3a5a;
  margin: 0;
}

.title-text p {
  font-size: 14px;
  color: #7a9aba;
  margin: 2px 0 0;
}

.ai-page-body {
  flex: 1;
  padding: 20px 32px 32px;
  min-height: 0;
  display: flex;
}

@media (max-width: 768px) {
  .ai-page-header {
    padding: 16px 16px 0;
  }

  .ai-page-body {
    padding: 12px 16px 20px;
  }
}
</style>
