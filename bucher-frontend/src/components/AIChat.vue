<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { useAIStore } from '@/stores/ai'

const props = withDefaults(defineProps<{
  /** 显示模式 */
  mode?: 'overlay' | 'embedded' | 'full'
  /** 课程 ID */
  courseId?: string
  /** 课程名称 */
  courseName?: string
  /** embedded 模式下是否可见 */
  visible?: boolean
}>(), {
  mode: 'overlay',
  courseId: '',
  courseName: '',
  visible: false,
})

const emit = defineEmits<{
  close: []
  'update:visible': [value: boolean]
}>()

const store = useAIStore()
const messagesContainer = ref<HTMLElement | null>(null)
const textareaRef = ref<HTMLTextAreaElement | null>(null)

// 初始化课程上下文
if (props.courseId) {
  store.initCourse(props.courseId, props.courseName)
}

// 本地输入
const localInput = ref('')

// 自动调整 textarea 高度
function autoResize() {
  nextTick(() => {
    const el = textareaRef.value
    if (el) {
      el.style.height = 'auto'
      el.style.height = Math.min(el.scrollHeight, 120) + 'px'
    }
  })
}

// 发送
function handleSend() {
  const text = localInput.value
  if (!text.trim() || store.loading) return
  store.sendMessage(text.trim())
  localInput.value = ''
  autoResize()
}

// 键盘发送（Shift+Enter 换行，Enter 发送）
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

// 自动滚动到底部
watch(
  () => store.messages.length,
  async () => {
    await nextTick()
    scrollToBottom()
  },
)

function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 新消息后自动滚动
watch(
  () => store.loading,
  async () => {
    await nextTick()
    scrollToBottom()
  },
)

// 关闭
function handleClose() {
  if (props.mode === 'overlay') {
    store.closeOverlay()
  } else if (props.mode === 'embedded') {
    emit('update:visible', false)
  }
  emit('close')
}

// 清空对话
async function handleClear() {
  await store.clearMessages()
}

// 快捷问题列表
const quickQuestions = computed(() => [
  `这门课的主要内容是什么？`,
  `帮我总结一下这门课的知识点`,
  `我该如何学习这门课？`,
  `这门课的作业要求有哪些？`,
])

function askQuick(q: string) {
  localInput.value = q
  handleSend()
}

// 格式化时间
function formatTime(ts: number): string {
  const d = new Date(ts)
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}
</script>

<template>
  <div
    class="ai-chat"
    :class="[
      `ai-chat--${mode}`,
      { 'ai-chat--visible': mode === 'overlay' ? store.visible : mode === 'embedded' ? visible : true },
    ]"
  >
    <!-- ===== 头部 ===== -->
    <div class="chat-header">
      <div class="chat-header-left">
        <div class="chat-header-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="10" />
            <path d="M12 6v6l4 2" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M8 14c0 2 1.5 3.5 4 3.5s4-1.5 4-3.5" stroke-linecap="round" />
          </svg>
        </div>
        <div class="chat-header-text">
          <span class="chat-header-title">AI 助教</span>
          <span v-if="store.courseName" class="chat-header-subtitle">{{ store.courseName }}</span>
        </div>
      </div>
      <div class="chat-header-actions">
        <button
          v-if="store.hasMessages"
          class="header-btn"
          title="清空对话"
          @click="handleClear"
        >
          <svg viewBox="0 0 16 16" width="14" height="14">
            <path d="M2 4h12M5 4V3a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v1M6 7v5M10 7v5M3 4l1 10a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1l1-10" stroke="currentColor" stroke-width="1.3" fill="none" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>
        <button
          class="header-btn header-btn-close"
          title="关闭"
          @click="handleClose"
        >
          <svg viewBox="0 0 16 16" width="14" height="14">
            <line x1="4" y1="4" x2="12" y2="12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
            <line x1="12" y1="4" x2="4" y2="12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
          </svg>
        </button>
      </div>
    </div>

    <!-- ===== 消息区 ===== -->
    <div ref="messagesContainer" class="chat-messages">
      <!-- 空状态 -->
      <div v-if="!store.hasMessages" class="chat-empty">
        <div class="empty-icon">
          <svg viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="1.2">
            <circle cx="24" cy="24" r="20" opacity="0.3" />
            <circle cx="24" cy="24" r="14" opacity="0.5" />
            <circle cx="24" cy="24" r="8" opacity="0.7" />
            <circle cx="24" cy="24" r="3" />
          </svg>
        </div>
        <p class="empty-title">有什么想问的吗？</p>
        <p class="empty-desc">我是 AI 助教，可以帮你解答课程相关的问题</p>

        <div class="quick-questions">
          <button
            v-for="(q, idx) in quickQuestions"
            :key="idx"
            class="quick-q-btn"
            @click="askQuick(q)"
            :disabled="store.loading"
          >
            <svg viewBox="0 0 14 14" width="12" height="12" class="q-icon">
              <polyline points="5,3 9,7 5,11" stroke="currentColor" stroke-width="1.3" fill="none" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
            {{ q }}
          </button>
        </div>
      </div>

      <!-- 消息列表 -->
      <div v-for="msg in store.messages" :key="msg.id" class="chat-message" :class="`chat-message--${msg.role}`">
        <div class="msg-avatar">
          <svg v-if="msg.role === 'assistant'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="10" />
            <path d="M12 6v6l4 2" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="10" />
            <path d="M12 6v6l4 2" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </div>
        <div class="msg-content">
          <div class="msg-bubble">{{ msg.content }}</div>
          <span class="msg-time">{{ formatTime(msg.timestamp) }}</span>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="store.loading" class="chat-message chat-message--assistant">
        <div class="msg-avatar">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="10" />
            <path d="M12 6v6l4 2" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </div>
        <div class="msg-content">
          <div class="msg-bubble msg-bubble--thinking">
            <span class="thinking-dot" />
            <span class="thinking-dot" />
            <span class="thinking-dot" />
          </div>
        </div>
      </div>

      <!-- 错误提示 -->
      <div v-if="store.error && !store.loading" class="chat-error">
        <svg viewBox="0 0 16 16" width="14" height="14">
          <circle cx="8" cy="8" r="7" stroke="currentColor" stroke-width="1.2" fill="none" />
          <line x1="8" y1="5" x2="8" y2="9" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
          <circle cx="8" cy="11" r="0.8" fill="currentColor" />
        </svg>
        <span>{{ store.error }}</span>
        <button class="retry-btn" @click="store.sendMessage()">重试</button>
      </div>
    </div>

    <!-- ===== 输入区 ===== -->
    <div class="chat-input-area">
      <div class="chat-input-wrapper">
        <textarea
          ref="textareaRef"
          v-model="localInput"
          class="chat-input"
          placeholder="输入你的问题..."
          rows="1"
          :disabled="store.loading"
          @keydown="handleKeydown"
          @input="autoResize"
        />
        <button
          class="send-btn"
          :class="{ 'send-btn--active': localInput.trim() && !store.loading }"
          :disabled="!localInput.trim() || store.loading"
          @click="handleSend"
        >
          <svg viewBox="0 0 20 20" width="16" height="16">
            <path d="M3 3l14 7L3 17l3-7-3-7z" stroke="currentColor" stroke-width="1.3" fill="none" stroke-linejoin="round" />
            <line x1="6" y1="10" x2="17" y2="10" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
          </svg>
        </button>
      </div>
      <p class="chat-hint">Enter 发送 · Shift+Enter 换行</p>
    </div>
  </div>
</template>

<style scoped>
/* ==================== 容器 ==================== */
.ai-chat {
  display: flex;
  flex-direction: column;
  background: #fff;
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* ===== Overlay 模式（浮动侧边栏） ===== */
.ai-chat--overlay {
  position: fixed;
  top: 0;
  right: 0;
  width: 420px;
  height: 100vh;
  z-index: 2000;
  box-shadow: -8px 0 32px rgba(0, 0, 0, 0.08);
  border-left: 1px solid rgba(0, 102, 255, 0.08);
  transform: translateX(100%);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.ai-chat--overlay.ai-chat--visible {
  transform: translateX(0);
}

/* ===== Embedded 模式（嵌入页面） ===== */
.ai-chat--embedded {
  width: 0;
  height: 100%;
  overflow: hidden;
  border-left: none;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
}

.ai-chat--embedded.ai-chat--visible {
  width: 400px;
  border-left: 1px solid rgba(0, 102, 255, 0.08);
  box-shadow: -4px 0 16px rgba(0, 102, 255, 0.04);
}

/* ===== Full 模式（全页面） ===== */
.ai-chat--full {
  width: 100%;
  height: 100%;
  max-width: 860px;
  margin: 0 auto;
  border-radius: 0;
  box-shadow: none;
  background: transparent;
}

/* ==================== 头部 ==================== */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(0, 102, 255, 0.06);
  background: rgba(255, 255, 255, 0.95);
  flex-shrink: 0;
}

.ai-chat--full .chat-header {
  background: transparent;
  padding: 20px 24px;
}

.chat-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-header-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: linear-gradient(135deg, rgba(0, 102, 255, 0.1), rgba(0, 212, 255, 0.08));
  color: #0066ff;
}

.chat-header-icon svg {
  width: 20px;
  height: 20px;
}

.chat-header-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.chat-header-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a3a5a;
}

.chat-header-subtitle {
  font-size: 12px;
  color: #7a9aba;
  font-weight: 400;
}

.chat-header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.header-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #7a9aba;
  cursor: pointer;
  transition: all 0.2s ease;
}

.header-btn:hover {
  background: rgba(0, 102, 255, 0.06);
  color: #0066ff;
}

.header-btn-close:hover {
  background: rgba(255, 100, 100, 0.08);
  color: #e53935;
}

/* ==================== 消息区 ==================== */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  scroll-behavior: smooth;
}

.chat-messages::-webkit-scrollbar {
  width: 4px;
}

.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: rgba(0, 102, 255, 0.12);
  border-radius: 2px;
}

/* ===== 空状态 ===== */
.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  gap: 8px;
  padding: 40px 20px;
  text-align: center;
}

.empty-icon {
  width: 60px;
  height: 60px;
  color: #0066ff;
  opacity: 0.3;
  margin-bottom: 8px;
}

.empty-icon svg {
  width: 100%;
  height: 100%;
}

.empty-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a3a5a;
  margin: 0;
}

.empty-desc {
  font-size: 14px;
  color: #7a9aba;
  margin: 0 0 20px;
}

.quick-questions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  max-width: 340px;
}

.quick-q-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border: 1px solid rgba(0, 102, 255, 0.10);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.7);
  color: #3a5a7a;
  font-size: 14px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s ease;
}

.quick-q-btn:hover:not(:disabled) {
  border-color: rgba(0, 102, 255, 0.25);
  background: rgba(0, 102, 255, 0.04);
  color: #0066ff;
  transform: translateX(4px);
}

.quick-q-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.q-icon {
  flex-shrink: 0;
  color: #b0c8e0;
}

.quick-q-btn:hover .q-icon {
  color: #0066ff;
}

/* ===== 消息气泡 ===== */
.chat-message {
  display: flex;
  gap: 10px;
  max-width: 100%;
}

.chat-message--user {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.chat-message--assistant .msg-avatar {
  background: linear-gradient(135deg, rgba(0, 102, 255, 0.1), rgba(0, 212, 255, 0.08));
  color: #0066ff;
}

.chat-message--user .msg-avatar {
  background: linear-gradient(135deg, #0066ff, #00d4ff);
  color: #fff;
}

.msg-avatar svg {
  width: 16px;
  height: 16px;
}

.msg-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: calc(100% - 50px);
}

.chat-message--user .msg-content {
  align-items: flex-end;
}

.msg-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.6;
  color: #2a4a6a;
  word-break: break-word;
  white-space: pre-wrap;
}

.chat-message--assistant .msg-bubble {
  background: rgba(240, 247, 255, 0.8);
  border-bottom-left-radius: 4px;
}

.chat-message--user .msg-bubble {
  background: linear-gradient(135deg, #0066ff, #00d4ff);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.msg-time {
  font-size: 11px;
  color: #b0c8e0;
  padding: 0 4px;
}

.chat-message--user .msg-time {
  text-align: right;
}

/* 思考动画 */
.msg-bubble--thinking {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 16px 20px;
  min-width: 60px;
  justify-content: center;
}

.thinking-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(0, 102, 255, 0.3);
  animation: thinkBounce 1.4s ease-in-out infinite;
}

.thinking-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.thinking-dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes thinkBounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* 错误 */
.chat-error {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(229, 57, 53, 0.06);
  border: 1px solid rgba(229, 57, 53, 0.12);
  border-radius: 10px;
  font-size: 13px;
  color: #c62828;
}

.retry-btn {
  margin-left: auto;
  padding: 4px 12px;
  border: 1px solid rgba(229, 57, 53, 0.2);
  border-radius: 6px;
  background: #fff;
  color: #c62828;
  font-size: 12px;
  font-family: inherit;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.retry-btn:hover {
  background: rgba(229, 57, 53, 0.06);
}

/* ==================== 输入区 ==================== */
.chat-input-area {
  padding: 16px 20px;
  border-top: 1px solid rgba(0, 102, 255, 0.06);
  background: rgba(255, 255, 255, 0.98);
  flex-shrink: 0;
}

.ai-chat--full .chat-input-area {
  background: transparent;
  border-top: 1px solid rgba(0, 102, 255, 0.06);
}

.chat-input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  background: rgba(240, 247, 255, 0.5);
  border: 1.5px solid rgba(0, 102, 255, 0.08);
  border-radius: 14px;
  padding: 8px 8px 8px 16px;
  transition: all 0.2s ease;
}

.chat-input-wrapper:focus-within {
  border-color: #0066ff;
  background: rgba(240, 247, 255, 0.8);
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.06);
}

.chat-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  font-family: inherit;
  color: #1a3a5a;
  resize: none;
  line-height: 1.5;
  max-height: 120px;
  padding: 4px 0;
}

.chat-input::placeholder {
  color: #b0c8e0;
}

.send-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 10px;
  background: rgba(0, 102, 255, 0.08);
  color: #7a9aba;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  background: rgba(0, 102, 255, 0.12);
  color: #0066ff;
}

.send-btn--active {
  background: linear-gradient(135deg, #0066ff, #00d4ff);
  color: #fff;
  box-shadow: 0 3px 10px rgba(0, 102, 255, 0.25);
}

.send-btn--active:hover:not(:disabled) {
  box-shadow: 0 5px 16px rgba(0, 102, 255, 0.35);
  transform: translateY(-1px);
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.chat-hint {
  margin: 6px 0 0;
  font-size: 11px;
  color: #c0d0e0;
  text-align: right;
}

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .ai-chat--overlay {
    width: 100vw;
  }

  .ai-chat--embedded.ai-chat--visible {
    width: 100%;
  }

  .ai-chat--full {
    max-width: 100%;
  }

  .chat-messages {
    padding: 16px;
  }

  .quick-questions {
    max-width: 100%;
  }
}
</style>
