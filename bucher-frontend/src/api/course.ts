import request from './index'

// 创建课程参数
export interface CreateCourseParams {
  name: string
  semester: string
  description?: string
}

// 课程信息
export interface CourseInfo {
  id: string
  courseCode: string
  name: string
  teacherId: string
  semester: string
  description: string | null
  coverUrl: string | null
  status: number
  classCount: number
  createTime: string
}

// 创建课程
export function createCourse(data: CreateCourseParams): Promise<CourseInfo> {
  return request.post('/course', data)
}

// 课程列表项
export interface CourseListItem {
  id: string
  name: string
  semester: string
  courseCode: string
}

// 获取教师课程列表（网关自动注入 X-User-Id）
export function getCourseList(): Promise<CourseListItem[]> {
  return request.get('/course/list')
}

// 学生课程列表项
export interface StudentCourseItem {
  id: string
  name: string
  teacherId: string
  teacherName: string
  semester: string
}

// 获取学生课程列表（网关自动注入 X-User-Id）
export function getStudentCourseList(): Promise<StudentCourseItem[]> {
  return request.get('/course/student/list')
}

// 创建班级参数
export interface CreateClassParams {
  courseId: string
  name: string
}

// 班级信息
export interface ClassInfo {
  id: string
  courseId: string
  courseName: string
  name: string
  inviteCode: string
  studentCount: number
  status: number
  createTime: string
}

// 创建班级（网关自动注入 X-User-Id 和 X-User-Role）
export function createClass(data: CreateClassParams): Promise<ClassInfo> {
  return request.post('/course/class', data)
}

// 加入课程班级参数
export interface JoinClassParams {
  inviteCode: string
}

// 学生加入课程班级
export function joinClass(data: JoinClassParams): Promise<null> {
  return request.post('/course/class/join', data)
}

// 上传资料响应
export interface MaterialInfo {
  id: number
  courseId: number
  fileName: string
  fileSize: number
  fileType: string
  fileExt: string
  duration: string | null
  createTime: string
}

// 上传课程资料
export function uploadMaterial(courseId: string, file: File): Promise<MaterialInfo> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/course/material/upload/${courseId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 获取课程资料列表
export function getMaterialList(courseId: string): Promise<MaterialInfo[]> {
  return request.get(`/course/material/list/${courseId}`)
}

// 删除课程资料
export function deleteCourseMaterial(materialId: number): Promise<null> {
  return request.delete(`/course/material/${materialId}`)
}

// 下载课程资料
export function downloadMaterial(materialId: number): Promise<any> {
  return request.get(`/course/material/download/${materialId}`, {
    responseType: 'blob'
  })
}

// 批量下载资料（返回 ZIP blob）
export function batchDownloadMaterials(materialIds: number[]): Promise<any> {
  return request.get('/course/material/batch-download', {
    params: { materialIds: materialIds.join(',') },
    responseType: 'blob'
  })
}
