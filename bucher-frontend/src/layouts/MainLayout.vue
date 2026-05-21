<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'
</script>

<template>
  <div class="main-layout">
    <!-- 顶部导航栏 -->
    <AppHeader />

    <!-- 主内容区域 -->
    <main class="main-content">
      <slot />
    </main>
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

.main-content {
  padding-top: 64px; /* 为固定的 header 留出空间 */
  min-height: 100vh;
}
</style>
