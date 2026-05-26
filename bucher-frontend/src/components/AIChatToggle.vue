<script setup lang="ts">
import { computed, watch, ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAIStore } from '@/stores/ai'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const store = useAIStore()
const userStore = useUserStore()

const isStudent = computed(() => userStore.userInfo?.roleName === '学生')

const currentCourseId = computed(() => route.params.id as string)
const currentCourseName = computed(() => {
  return (route.query.name as string) || store.courseName
})

const showToggle = computed(() => {
  if (!isStudent.value) return false
  if (route.name === 'aiChat') return false
  if (route.name === 'homeworkDo') return false
  return true
})

watch(
  () => route.params.id,
  (id) => {
    if (id && typeof id === 'string') {
      store.initCourse(id, currentCourseName.value)
    }
  },
  { immediate: true },
)

watch(
  () => route.path,
  () => {
    store.closeOverlay()
  },
)

// ==================== 可拖拽 ====================

/** 从 localStorage 读取存储的位置，fallback 到右下角 */
function loadPosition() {
  try {
    const saved = localStorage.getItem('ai_toggle_pos')
    if (saved) return JSON.parse(saved)
  } catch { /* ignore */ }
  return { x: window.innerWidth - 52 - 32, y: window.innerHeight - 52 - 32 }
}

const pos = ref(loadPosition())

const DRAG_THRESHOLD = 5
let dragging = false
let dragMoved = false
let startX = 0
let startY = 0
let startPosX = 0
let startPosY = 0

function onPointerDown(e: PointerEvent) {
  if (e.button !== 0) return
  dragging = true
  dragMoved = false
  startX = e.clientX
  startY = e.clientY
  startPosX = pos.value.x
  startPosY = pos.value.y
  // 拖拽时暂时去掉 transition
  const btn = (e.currentTarget as HTMLElement)
  btn.style.transition = 'none'
  btn.setPointerCapture(e.pointerId)
}

function onPointerMove(e: PointerEvent) {
  if (!dragging) return
  const dx = e.clientX - startX
  const dy = e.clientY - startY
  if (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD) {
    dragMoved = true
  }
  pos.value = {
    x: startPosX + dx,
    y: startPosY + dy,
  }
}

function onPointerUp(e: PointerEvent) {
  if (!dragging) return
  dragging = false
  const btn = (e.currentTarget as HTMLElement)
  btn.style.transition = ''
  btn.releasePointerCapture(e.pointerId)

  // 限制在视口内
  const w = 52
  const h = 52
  const clamped = {
    x: Math.max(8, Math.min(window.innerWidth - w - 8, pos.value.x)),
    y: Math.max(8, Math.min(window.innerHeight - h - 8, pos.value.y)),
  }
  pos.value = clamped
  localStorage.setItem('ai_toggle_pos', JSON.stringify(clamped))

  // 没拖拽 → 视为点击
  if (!dragMoved) {
    store.toggleOverlay()
  }
}

// 窗口大小变化时修正位置
function onResize() {
  const w = 52
  const h = 52
  pos.value = {
    x: Math.max(8, Math.min(window.innerWidth - w - 8, pos.value.x)),
    y: Math.max(8, Math.min(window.innerHeight - h - 8, pos.value.y)),
  }
}

onMounted(() => {
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
})
</script>

<template>
  <div v-if="showToggle" class="ai-toggle-wrapper">
    <!-- 可拖拽浮动按钮 -->
    <button
      class="ai-toggle-btn"
      :class="{ 'ai-toggle-btn--active': store.visible, 'ai-toggle-btn--dragging': dragging }"
      :style="{ left: pos.x + 'px', top: pos.y + 'px' }"
      @pointerdown="onPointerDown"
      @pointermove="onPointerMove"
      @pointerup="onPointerUp"
      :title="store.visible ? '关闭 AI 助教' : '打开 AI 助教'"
    >
      <svg v-if="!store.visible" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="toggle-icon">
        <circle cx="12" cy="12" r="10" />
        <path d="M12 6v6l4 2" stroke-linecap="round" stroke-linejoin="round" />
        <path d="M8 15c0 1.5 1.5 3 4 3s4-1.5 4-3" stroke-linecap="round" />
      </svg>
      <svg v-else viewBox="0 0 16 16" width="16" height="16" class="toggle-icon">
        <line x1="4" y1="4" x2="12" y2="12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
        <line x1="12" y1="4" x2="4" y2="12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
      </svg>
    </button>

  </div>
</template>

<style scoped>
.ai-toggle-wrapper {
  /* 占位，不干涉布局 */
}

.ai-toggle-btn {
  position: fixed;
  z-index: 1999;
  width: 52px;
  height: 52px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, #0066ff, #00d4ff);
  color: #fff;
  cursor: grab;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 6px 20px rgba(0, 102, 255, 0.3),
    0 2px 8px rgba(0, 102, 255, 0.15);
  transition:
    box-shadow 0.3s ease,
    transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  touch-action: none;
  user-select: none;
}

.ai-toggle-btn:hover {
  transform: scale(1.08);
  box-shadow:
    0 8px 28px rgba(0, 102, 255, 0.4),
    0 3px 12px rgba(0, 102, 255, 0.2);
}

.ai-toggle-btn:active {
  cursor: grabbing;
}

.ai-toggle-btn--dragging {
  transform: scale(1.12);
  box-shadow:
    0 12px 36px rgba(0, 102, 255, 0.35),
    0 4px 16px rgba(0, 102, 255, 0.2);
}

.ai-toggle-btn--active {
  background: #fff;
  color: #0066ff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.ai-toggle-btn--active:hover {
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.16);
}

.toggle-icon {
  width: 24px;
  height: 24px;
  pointer-events: none;
}

@media (max-width: 768px) {
  .ai-toggle-btn {
    width: 46px;
    height: 46px;
  }

  .toggle-icon {
    width: 20px;
    height: 20px;
  }
}
</style>
