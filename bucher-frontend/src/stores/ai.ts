import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ChatMessage } from '@/api/ai'
import { sendChatMessage, getChatHistory, clearChatHistory } from '@/api/ai'

// 用 uuid 风格 id 简单生成
let msgIdSeq = 0
function nextId(): string {
  return `msg_${Date.now()}_${++msgIdSeq}`
}

/**
 * AI 助手显示模式
 * - 'overlay': 浮动按钮触发的侧边栏（absolute/fixed 覆盖层）
 * - 'embedded': 嵌入页面布局的侧边栏（如作业页右侧）
 * - 'full': 全页面模式（路由跳转）
 */
export type AIDisplayMode = 'overlay' | 'embedded' | 'full'

export const useAIStore = defineStore('ai', () => {
  // ==================== 状态 ====================
  /** 当前课程 ID */
  const courseId = ref<string>('')
  /** 课程名称 */
  const courseName = ref<string>('')

  /** 聊天消息列表 */
  const messages = ref<ChatMessage[]>([])
  /** 是否正在等待 AI 回复 */
  const loading = ref(false)
  /** 输入框文本 */
  const inputText = ref('')

  /** 当前显示模式 */
  const displayMode = ref<AIDisplayMode>('overlay')
  /** overlay 模式是否可见 */
  const visible = ref(false)
  /** embedded 模式是否展开 */
  const embeddedVisible = ref(false)

  /** 错误信息 */
  const error = ref<string | null>(null)

  // ==================== 计算属性 ====================
  const hasMessages = computed(() => messages.value.length > 0)
  const lastMessage = computed(() => messages.value[messages.value.length - 1] || null)

  // ==================== 操作 ====================

  /** 初始化 / 切换课程上下文 */
  function initCourse(cId: string, cName: string) {
    if (courseId.value === cId) return
    courseId.value = cId
    courseName.value = cName
    messages.value = []
    error.value = null
    loadHistory()
  }

  /** 加载历史消息 */
  async function loadHistory() {
    if (!courseId.value) return
    try {
      const res = await getChatHistory(courseId.value)
      messages.value = (res || []).map(msg => ({
        id: `hist_${msg.createTime}`,
        role: (msg.role === 'ASSISTANT' ? 'assistant' : 'user') as 'user' | 'assistant',
        content: msg.content,
        timestamp: new Date(msg.createTime).getTime(),
      }))
    } catch {
      // 静默失败
    }
  }

  /** 发送消息（SSE 流式） */
  async function sendMessage(text?: string) {
    const content = text ?? inputText.value
    if (!content.trim() || !courseId.value || loading.value) return

    const userMsg: ChatMessage = {
      id: nextId(),
      role: 'user',
      content: content.trim(),
      timestamp: Date.now(),
    }
    messages.value.push(userMsg)
    inputText.value = ''
    loading.value = true
    error.value = null

    const assistantMsg: ChatMessage = {
      id: nextId(),
      role: 'assistant',
      content: '',
      timestamp: Date.now(),
    }
    messages.value.push(assistantMsg)

    try {
      await sendChatMessage(
        courseId.value,
        content.trim(),
        (token) => {
          assistantMsg.content += token
        },
      )
    } catch (e: any) {
      error.value = e?.message || '获取回复失败，请重试'
      if (!assistantMsg.content) {
        messages.value = messages.value.filter(m => m.id !== assistantMsg.id)
      }
    } finally {
      loading.value = false
    }
  }

  /** 清空对话 */
  async function clearMessages() {
    if (!courseId.value) return
    try {
      await clearChatHistory(courseId.value)
      messages.value = []
      error.value = null
    } catch {
      // 静默
    }
  }

  /** 切换 overlay 可见性 */
  function toggleOverlay() {
    visible.value = !visible.value
    if (visible.value) {
      displayMode.value = 'overlay'
    }
  }

  /** 打开 overlay */
  function openOverlay() {
    visible.value = true
    displayMode.value = 'overlay'
  }

  /** 关闭 overlay */
  function closeOverlay() {
    visible.value = false
  }

  /** 切换 embedded 可见性 */
  function toggleEmbedded() {
    embeddedVisible.value = !embeddedVisible.value
    if (embeddedVisible.value) {
      displayMode.value = 'embedded'
    }
  }

  /** 设置模式为 full */
  function setFullMode() {
    displayMode.value = 'full'
    visible.value = false
    embeddedVisible.value = false
  }

  /** 重置状态 */
  function reset() {
    courseId.value = ''
    courseName.value = ''
    messages.value = []
    loading.value = false
    inputText.value = ''
    visible.value = false
    embeddedVisible.value = false
    error.value = null
  }

  return {
    courseId,
    courseName,
    messages,
    loading,
    inputText,
    displayMode,
    visible,
    embeddedVisible,
    error,
    hasMessages,
    lastMessage,
    initCourse,
    loadHistory,
    sendMessage,
    clearMessages,
    toggleOverlay,
    openOverlay,
    closeOverlay,
    toggleEmbedded,
    setFullMode,
    reset,
  }
})
