import request from './index'
import type { AxiosResponse } from 'axios'

/* ==================== 题库分组 ==================== */
export interface QuestionBankItem {
  id: string
  name: string
  questionCount: number
  createTime: string
}

/** 获取题库分组列表 */
export function getQuestionBankList(): Promise<QuestionBankItem[]> {
  return request.get('/question/group/list')
}

/** 创建题库分组，返回新题库 ID */
export function createQuestionBank(data: { name: string }): Promise<string> {
  return request.post('/question/group', data)
}

/** 更新题库分组名称 */
export function updateQuestionBank(id: string, data: { name: string }): Promise<void> {
  return request.put(`/question/group/${id}`, data)
}

/** 删除题库分组 */
export function deleteQuestionBank(id: string): Promise<void> {
  return request.delete(`/question/group/${id}`)
}

/* ==================== 题目 CRUD ==================== */

/** 题目列表项（左侧面板 / 管理页表格） */
export interface QuestionListItem {
  id: string
  contentPreview: string
  type: number
  typeName: string
  difficulty: number
  createTime: string
}

/** 题目选项 */
export interface QuestionOption {
  id?: string
  label: string
  content: string
  isCorrect: boolean
  sortOrder: number
}

/** 题目详情 */
export interface QuestionDetail {
  id: string
  type: number
  content: string
  answer: string
  analysis: string
  options: QuestionOption[]
  difficulty?: number
  score?: number
  programmingConfig?: ProgrammingConfig
  testCases?: TestCase[]
  createTime: string
  updateTime: string
}

/* ==================== 编程题 ==================== */

export interface ProgrammingConfig {
  templateCode: string
  judge0LanguageId: number
  timeLimit: number
  memoryLimit: number
}

export interface TestCase {
  input: string
  expectedOutput: string
  isSample: boolean
  sortOrder: number
}

/** 批量保存中的单道题目 */
export interface BatchSaveQuestion {
  id: string | null
  type: number
  content: string
  answer: string
  analysis: string
  difficulty?: number
  score?: number
  options: { id?: string; label: string; content: string; isCorrect: boolean; sortOrder: number }[]
  programmingConfig?: ProgrammingConfig
  testCases?: TestCase[]
  deleted: boolean
}

/** 批量保存请求体 */
export interface BatchSaveRequest {
  groupId: string
  questions: BatchSaveQuestion[]
}

/** 获取题目列表 */
export function getQuestionList(groupId: string): Promise<QuestionListItem[]> {
  return request.get('/question/list', { params: { groupId } })
}

/** 获取题目详情 */
export function getQuestionDetail(id: string): Promise<QuestionDetail> {
  return request.get(`/question/${id}`)
}

/** 批量保存题目 */
export function batchSaveQuestions(data: BatchSaveRequest): Promise<void> {
  return request.post('/question/batch-save', data)
}

/* ==================== 批量导入 ==================== */

export interface ImportUploadResult {
  fileKey: string
  originalName: string
  rowCount: number
}

export interface ImportErrorItem {
  row: number
  reason: string
}

export interface ImportExecuteResult {
  totalCount: number
  successCount: number
  failCount: number
  errors: ImportErrorItem[]
}

export interface ImportExecuteRequest {
  groupId: string
  fileKey: string
}

/** 下载导入模板（.xlsx 文件流） */
export async function downloadImportTemplate(): Promise<void> {
  const response = await request.get('/question/import/template', {
    responseType: 'blob',
  } as any) as unknown as AxiosResponse
  const blob = new Blob([response.data], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', '题目导入模板.xlsx')
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}

/** 上传导入文件 */
export function uploadImportFile(file: File): Promise<ImportUploadResult> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/question/import/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 执行导入 */
export function executeImport(data: ImportExecuteRequest): Promise<ImportExecuteResult> {
  return request.post('/question/import/execute', data)
}
