import request from './index'

/* ==================== 教师端 ==================== */

/** 教师考试列表项 */
export interface ExamListItem {
  id: number
  title: string
  courseName: string
  startTime: string
  endTime: string
  duration: number
  totalScore: number
  status: number
  submitCount: number
  totalCount: number
}

/** 教师考试列表响应 */
export interface ExamListData {
  records: ExamListItem[]
  total: number
  pageNum: number
  pageSize: number
}

/** 未发布考试列表项 */
export interface UnpublishedExamItem {
  id: number
  title: string
  courseName: string
}

/** 题目 ID + 分数 */
export interface QuestionScoreItem {
  questionId: string
  score: number
}

/** 保存考试草稿请求 */
export interface SaveDraftParams {
  title: string
  courseId: string
  sourceBankId: string
  questions: QuestionScoreItem[]
}

/** 发布考试请求 */
export interface PublishExamParams extends SaveDraftParams {
  classIds: string[]
  startTime: string
  duration: number
  earlySubmitMinutes: number
  lateBanMinutes?: number
  autoSubmit?: number
  passScore?: number
  description?: string
}

/** 获取教师考试列表 */
export function getExamList(params: {
  filterMode?: string
  pageNum: number
  pageSize: number
}): Promise<ExamListData> {
  return request.get('/exam/teacher/list', { params })
}

/** 获取未发布考试列表 */
export function getUnpublishedExamList(params: {
  pageNum: number
  pageSize: number
}): Promise<{ records: UnpublishedExamItem[]; total: number; pageNum: number; pageSize: number }> {
  return request.get('/exam/teacher/unpublished-list', { params })
}

/** 保存考试草稿 */
export function saveExamDraft(data: SaveDraftParams): Promise<void> {
  return request.post('/exam/teacher/draft', data)
}

/** 发布考试 */
export function publishExam(data: PublishExamParams): Promise<void> {
  return request.post('/exam/teacher/publish', data)
}

/** 超时自动提交 */
export function autoSubmitExams(): Promise<void> {
  return request.post('/exam/teacher/auto-submit')
}

/* ==================== 学生端 ==================== */

/** 学生端考试列表项 */
export interface StudentExamItem {
  id: number
  title: string
  startTime: string
  endTime: string
  duration: number
  status: number
}

/** 获取学生端考试列表 */
export function getStudentExamList(courseId: string): Promise<StudentExamItem[]> {
  return request.get('/exam/student/list', { params: { courseId } })
}

/** 做题页面数据 */
export interface ExamDoData {
  examId: number
  title: string
  description: string
  courseName: string
  totalScore: number
  duration: number
  earlySubmitMinutes: number
  startTime: string
  endTime: string
  status: number
  submitted: boolean
  remainingSeconds: number
  questions: ExamQuestionItem[]
}

export interface ExamQuestionItem {
  questionId: number
  questionType: 'single' | 'multiple' | 'judge' | 'fill' | 'short-answer' | 'programming'
  content: string
  score: number
  sortOrder: number
  answer: string | null
  options: { label: string; content: string }[] | null
}

/** 获取做题页面数据 */
export function getExamDoData(examId: string): Promise<ExamDoData> {
  return request.get(`/exam/student/${examId}/do`)
}

/** 暂存单题答案 */
export function saveExamAnswer(data: {
  examId: string | number
  questionId: string | number
  answer: string
}): Promise<void> {
  return request.post('/exam/student/answer/save', data)
}

/** 提交全部答案 */
export function submitExam(data: { examId: string | number }): Promise<void> {
  return request.post('/exam/student/submit', data)
}
