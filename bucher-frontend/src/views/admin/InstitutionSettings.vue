<script setup lang="ts">
import AdminLayout from '@/layouts/AdminLayout.vue'
</script>

<template>
  <AdminLayout>
    <div class="placeholder-page">
      <div class="placeholder-card">
        <div class="placeholder-icon">
          <svg viewBox="0 0 80 80" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="40" cy="40" r="12" />
            <path d="M40 12v6M40 62v6M12 40h6M62 40h6M20.5 20.5l4.24 4.24M55.26 55.26l4.24 4.24M20.5 59.5l4.24-4.24M55.26 24.74l4.24-4.24" />
            <circle cx="40" cy="18" r="3" fill="currentColor" />
            <circle cx="58" cy="34" r="3" fill="currentColor" />
            <circle cx="58" cy="46" r="3" fill="currentColor" />
            <circle cx="40" cy="62" r="3" fill="currentColor" />
            <circle cx="22" cy="46" r="3" fill="currentColor" />
            <circle cx="22" cy="34" r="3" fill="currentColor" />
          </svg>
        </div>
        <h2>机构设置</h2>
        <p>功能开发中，敬请期待...</p>
      </div>
    </div>
  </AdminLayout>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap');

.placeholder-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 64px);
  padding: 40px;
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.placeholder-card {
  text-align: center;
  padding: 60px 80px;
  background: rgba(255, 255, 255, 0.75);
  border-radius: 24px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 32px rgba(0, 102, 255, 0.08);
}

.placeholder-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  color: #a0b8d0;
}

.placeholder-icon svg {
  width: 100%;
  height: 100%;
}

.placeholder-card h2 {
  font-size: 24px;
  font-weight: 600;
  color: #1a3a5a;
  margin: 0 0 12px;
}

.placeholder-card p {
  font-size: 15px;
  color: #7a9aba;
  margin: 0;
}
</style>
