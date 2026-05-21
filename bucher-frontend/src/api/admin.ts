import request from './index'

// ======================== 类型定义 ========================

export interface Department {
  id: number
  name: string
  type: 1 | 2
  description?: string
  createTime?: string
  classCount?: number
  studentCount?: number
  teacherCount?: number
}

export interface AdminClass {
  id: number
  name: string
  deptId: number
  deptName: string
  year: number
  createTime?: string
}

export interface UserItem {
  id: number
  userNo: string
  realName: string
  email: string
  role: number
  roleName: string
  deptName?: string
  adminClassName?: string
  status: number
  pwdReset: number
  createTime?: string
}

export interface PaginatedResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export interface ImportResult {
  totalCount: number
  successCount: number
  failCount: number
  errors: Array<{
    row: number
    userNo: string
    reason: string
  }>
}

// ======================== 院系 API ========================

export function getDepartments(params?: {
  page?: number
  size?: number
  name?: string
  type?: number
}): Promise<PaginatedResult<Department>> {
  return request.get('/user/admin/departments', { params })
}

export function getDepartmentDetail(id: number): Promise<Department> {
  return request.get(`/user/admin/departments/${id}`)
}

export function createDepartment(data: {
  name: string
  type: number
  description?: string
}): Promise<Department> {
  return request.post('/user/admin/departments', data)
}

export function updateDepartment(
  id: number,
  data: { name: string; type: number; description?: string }
): Promise<void> {
  return request.put(`/user/admin/departments/${id}`, data)
}

export function deleteDepartment(id: number): Promise<void> {
  return request.delete(`/user/admin/departments/${id}`)
}

// ======================== 行政班级 API ========================

export function getClasses(params?: {
  page?: number
  size?: number
  deptId?: number
  name?: string
}): Promise<PaginatedResult<AdminClass>> {
  return request.get('/user/admin/classes', { params })
}

export function getClassDetail(id: number): Promise<AdminClass> {
  return request.get(`/user/admin/classes/${id}`)
}

export function createClass(data: {
  name: string
  deptId: number
  year: number
}): Promise<AdminClass> {
  return request.post('/user/admin/classes', data)
}

export function updateClass(
  id: number,
  data: { name: string; deptId: number; year: number }
): Promise<void> {
  return request.put(`/user/admin/classes/${id}`, data)
}

export function deleteClass(id: number): Promise<void> {
  return request.delete(`/user/admin/classes/${id}`)
}

// ======================== 用户 API ========================

export function getUsers(params?: {
  page?: number
  size?: number
  userRole?: number
  deptId?: number
  adminClassId?: number
  keyword?: string
}): Promise<PaginatedResult<UserItem>> {
  return request.get('/user/admin/users', { params })
}

export function resetUserPassword(id: number): Promise<void> {
  return request.put(`/user/admin/users/${id}/reset-password`)
}

// ======================== 批量导入 API ========================

export function importStudents(
  adminClassId: number,
  file: File
): Promise<ImportResult> {
  const formData = new FormData()
  formData.append('adminClassId', String(adminClassId))
  formData.append('file', file)
  return request.post('/user/admin/students/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function importTeachers(
  deptId: number,
  file: File
): Promise<ImportResult> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/user/admin/departments/${deptId}/teachers/import`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      'X-User-Role': '1'
    }
  })
}
