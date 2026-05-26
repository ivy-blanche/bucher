import request from './index'

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: number
}

/**
 * 发送聊天消息 — SSE 流式对话
 * 返回 AbortController 以支持取消请求
 * @param onToken 每次收到 token 时的回调
 */
export function sendChatMessage(
  courseId: string,
  message: string,
  onToken: (token: string) => void,
): Promise<void> {
  const token = localStorage.getItem('token')

  return new Promise((resolve, reject) => {
    fetch(`/api/ai/chat/courses/${courseId}/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ message }),
    }).then(async (response) => {
      if (!response.ok) {
        const text = await response.text().catch(() => '')
        throw new Error(text || `HTTP ${response.status}`)
      }
      const reader = response.body!.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const token = line.slice(5)
            if (token !== '') onToken(token)
          }
        }
      }
      resolve()
    }).catch((err) => {
      reject(err)
    })
  })
}

/** 获取聊天历史（返回 ChatMessageVO[]，含 role / content / createTime） */
export function getChatHistory(courseId: string): Promise<{ role: string; content: string; createTime: string }[]> {
  return request.get(`/ai/chat/courses/${courseId}/history`)
}

/** 清空聊天历史 */
export function clearChatHistory(courseId: string): Promise<null> {
  return request.delete(`/ai/chat/history/${courseId}`)
}

export interface AiConfig {
  id: number
  projectName: string
  provider: string
  apiKey: string
  apiEndpoint: string
  modelName: string
  embeddingModel: string
  embeddingApiKey: string
  embeddingApiEndpoint: string
  maxTokens: number
  temperature: number
  status: number
  createTime: string
  updateTime: string
}

export interface AiConfigResponse {
  code: number
  message: string
  data: AiConfig | null
  timestamp: number
}

/** 获取 AI 配置（admin） */
export function getAiConfig(): Promise<AiConfigResponse> {
  return request.get('/ai/admin/config')
}

export interface SaveAiConfigRequest {
  projectName: string
  provider: string
  apiKey: string
  apiEndpoint: string
  modelName: string
  embeddingModel: string
  embeddingApiKey: string
  embeddingApiEndpoint: string
  maxTokens: number
  temperature: number
  status: number
}

/** 保存 AI 配置（admin） */
export function saveAiConfig(data: SaveAiConfigRequest): Promise<AiConfigResponse> {
  return request.post('/ai/admin/config', data)
}

export interface PermissionRecord {
  id: number
  teacherId: number
  status: number
  grantedBy: number
  createTime: string
  updateTime: string
}

export interface PermissionPageData {
  records: PermissionRecord[]
  total: number
  size: number
  current: number
  pages: number
}

export interface PermissionPageResponse {
  code: number
  message: string
  data: PermissionPageData
  timestamp: number
}

/** 分页查询已授权教师 */
export function getPermissionsPage(page: number, size: number, keyword?: string): Promise<PermissionPageResponse> {
  const params: Record<string, number | string> = { page, size }
  if (keyword?.trim()) {
    params.keyword = keyword.trim()
  }
  return request.get('/ai/admin/permissions', { params })
}

/** 搜索教师授权状态 */
export function searchTeachers(keyword: string): Promise<{
  code: number
  message: string
  data: { id: number; userNo: string; realName: string; granted: boolean }[]
  timestamp: number
}> {
  return request.get('/ai/admin/teachers/search', { params: { keyword } })
}

/** 授权教师 */
export function grantPermission(teacherId: number): Promise<{ code: number; message: string }> {
  return request.post(`/ai/admin/permissions/${teacherId}`)
}

/** 撤销教师权限 */
export function deletePermission(teacherId: number): Promise<{ code: number; message: string }> {
  return request.delete(`/ai/admin/permissions/${teacherId}`)
}

// ==================== Part 2: 课程 AI 配置 ====================

/** 获取课程 AI 启用状态 */
export function getCourseAiStatus(courseId: string): Promise<boolean> {
  return request.get(`/ai/courses/${courseId}/config`)
}

/** 启用/关闭课程 AI */
export function setCourseAiStatus(courseId: string, enabled: boolean): Promise<null> {
  return request.post(`/ai/courses/${courseId}/config`, null, {
    params: { enabled }
  })
}

// ==================== Part 3: 知识库文档 ====================

/** 知识库文档 VO */
export interface KnowledgeDocument {
  id: number
  courseId: number
  teacherId: number
  teacherName: string
  fileName: string
  fileSize: number
  fileExt: string
  status: number   // 0=待处理 1=已向量化 2=失败
  chunkCount: number
  errorMsg: string
  createTime: string
}

/** 获取课程知识库文档列表（分页） */
export function getKnowledgeDocuments(
  courseId: string,
  page: number = 1,
  size: number = 20
): Promise<{ records: KnowledgeDocument[]; total: number }> {
  return request.get(`/ai/knowledge-base/courses/${courseId}/documents`, {
    params: { page, size }
  })
}

/** 上传知识库文档 */
export function uploadKnowledgeDocument(
  courseId: string,
  file: File
): Promise<KnowledgeDocument> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/ai/knowledge-base/courses/${courseId}/documents`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000,
  })
}

/** 删除知识库文档 */
export function deleteKnowledgeDocument(courseId: string, id: number): Promise<null> {
  return request.delete(`/ai/knowledge-base/courses/${courseId}/documents/${id}`)
}

// ==================== Part 4: 全局知识库（管理员） ====================

/** 获取全局知识库文档列表 */
export function getGlobalKnowledgeDocuments(
  page: number = 1,
  size: number = 20
): Promise<{ records: KnowledgeDocument[]; total: number }> {
  return request.get('/ai/admin/knowledge-base/documents', {
    params: { page, size }
  })
}

/** 上传全局文档 */
export function uploadGlobalKnowledgeDocument(file: File): Promise<KnowledgeDocument> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/ai/admin/knowledge-base/documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000,
  })
}

/** 删除全局文档 */
export function deleteGlobalKnowledgeDocument(id: number): Promise<null> {
  return request.delete(`/ai/admin/knowledge-base/documents/${id}`)
}
