import request from './index'

export interface HomeworkListItem {
  id: number
  title: string
  courseName: string
  deadline: string
  submitCount: number
  gradedCount: number
  status: number
  gradingStatus: number
}

export interface HomeworkListData {
  records: HomeworkListItem[]
  total: number
  size: number
  current: number
  pages: number
}

export interface HomeworkListParams {
  filterMode: string
  pageNum: number
  pageSize: number
}

/* ==================== 布置作业 ==================== */

/** 作业中的单道题目（含分数） */
export interface HomeworkQuestion {
  questionId: string
  type: number
  typeName: string
  contentPreview: string
  score: number
}

/** 题目 ID + 分数 */
export interface QuestionItem {
  questionId: string
  score: number
}

/** 保存作业草稿请求 */
export interface SaveHomeworkDraftParams {
  title: string
  courseId: string
  sourceBankId: string
  questions: QuestionItem[]
}

/** 发布作业请求 */
export interface PublishHomeworkParams extends SaveHomeworkDraftParams {
  classIds: string[]
  deadline: string
}

/** 课程班级 */
export interface CourseClassItem {
  id: string
  name: string
  studentCount: number
}

/** 获取教师作业列表 */
export function getHomeworkList(params: HomeworkListParams): Promise<HomeworkListData> {
  return request.get('/homework/teacher/list', { params })
}

/* ==================== 未发布作业 ==================== */

/** 未发布作业列表项 */
export interface UnpublishedHomeworkListItem {
  id: number
  title: string
  courseName: string
  deadline: string
}

/** 未发布作业列表响应 */
export interface UnpublishedHomeworkListData {
  records: UnpublishedHomeworkListItem[]
  total: number
  pageNum: number
  pageSize: number
}

/** 获取教师未发布作业列表 */
export function getUnpublishedHomeworkList(params: {
  pageNum: number
  pageSize: number
}): Promise<UnpublishedHomeworkListData> {
  return request.get('/homework/teacher/unpublished-list', { params })
}

/** 保存作业草稿 */
export function saveHomeworkDraft(data: SaveHomeworkDraftParams): Promise<void> {
  return request.post('/homework/teacher/draft', data)
}

/** 发布作业 */
export function publishHomework(data: PublishHomeworkParams): Promise<void> {
  return request.post('/homework/teacher/publish', data)
}

/** 获取课程下的班级列表 */
export function getCourseClassList(courseId: string): Promise<CourseClassItem[]> {
  return request.get(`/course/class/list/${courseId}`)
}

/* ==================== 学生端作业列表 ==================== */

/** 学生端作业列表项 */
export interface StudentHomeworkItem {
  id: number
  title: string
  deadline: string
  status: number
}

/** 获取学生端作业列表 */
export function getStudentHomeworkList(courseId: string): Promise<StudentHomeworkItem[]> {
  return request.get('/homework/student/list', { params: { courseId } })
}

/* ==================== 学生作答 ==================== */

/** 获取做题页面数据 */
export function getHomeworkDoData(homeworkId: string): Promise<any> {
  return request.get(`/homework/student/${homeworkId}/do`)
}

/** 暂存单题答案（Redis） */
export function saveAnswer(data: {
  homeworkId: string | number
  questionId: string | number
  answer: string
}): Promise<void> {
  return request.post('/homework/student/answer/save', data)
}

/** 提交全部答案（MySQL） */
export function submitHomework(data: {
  homeworkId: string | number
}): Promise<void> {
  return request.post('/homework/student/submit', data)
}

/* ==================== 编程题运行 ==================== */

export interface RunCodeRequest {
  homeworkId: number | string
  questionId: number | string
  code: string
}

export interface TestCaseResult {
  passed: boolean
  stdout: string
  expectedOutput: string
  time: string
  memory: number
}

export interface RunCodeResponse {
  compileSuccess: boolean
  compileOutput: string | null
  passedCount: number
  totalCount: number
  testCaseResults: TestCaseResult[]
}

/** 运行编程题代码 */
export function runCode(data: RunCodeRequest): Promise<RunCodeResponse> {
  return request.post('/homework/student/answer/run', data)
}
