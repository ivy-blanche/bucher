import request from './index'

export interface NotificationVO {
  id: number
  userId: number
  type: 1 | 2            // 1=作业通知, 2=考试通知
  relatedId: number       // 业务ID（homeworkId/examId）
  teacherName: string
  courseName: string
  bizTitle: string        // 作业/考试名称
  deadline: string        // ISO格式
  startTime: string | null // ISO格式，仅考试时非空
  isRead: boolean
  createTime: string      // ISO格式
}

/** 占位数据：后端未就绪时使用 */
const mockNotifications: NotificationVO[] = [
  {
    id: 1891234567890123456,
    userId: 100001,
    type: 1,
    relatedId: 1891234567890123400,
    teacherName: '张老师',
    courseName: '高等数学',
    bizTitle: '第三章习题',
    deadline: '2026-05-25T23:59:59',
    startTime: null,
    isRead: false,
    createTime: '2026-05-20T14:30:00'
  },
  {
    id: 1891234567890123457,
    userId: 100001,
    type: 1,
    relatedId: 1891234567890123401,
    teacherName: '李老师',
    courseName: '数据结构',
    bizTitle: '树与二叉树',
    deadline: '2026-05-22T18:00:00',
    startTime: null,
    isRead: true,
    createTime: '2026-05-19T10:00:00'
  },
  {
    id: 1891234567890123458,
    userId: 100001,
    type: 1,
    relatedId: 1891234567890123402,
    teacherName: '王老师',
    courseName: '操作系统',
    bizTitle: '进程管理实验',
    deadline: '2026-05-24T23:59:59',
    startTime: null,
    isRead: false,
    createTime: '2026-05-19T09:00:00'
  },
  {
    id: 1891234567890123459,
    userId: 100001,
    type: 2,
    relatedId: 1891234567890123410,
    teacherName: '赵老师',
    courseName: '计算机网络',
    bizTitle: '期中考试',
    deadline: '2026-05-28T10:00:00',
    startTime: '2026-05-28T08:00:00',
    isRead: false,
    createTime: '2026-05-18T16:00:00'
  },
  {
    id: 1891234567890123460,
    userId: 100001,
    type: 2,
    relatedId: 1891234567890123411,
    teacherName: '陈老师',
    courseName: '数据库系统概论',
    bizTitle: '单元测验',
    deadline: '2026-05-20T11:00:00',
    startTime: '2026-05-20T09:00:00',
    isRead: true,
    createTime: '2026-05-17T14:00:00'
  }
]

/** 获取通知列表（API 失败时使用占位数据） */
export async function getNotifications(): Promise<NotificationVO[]> {
  try {
    return await request.get('/message/notifications')
  } catch {
    return mockNotifications
  }
}

/** 标记所有通知为已读 */
export function markNotificationsRead(): Promise<void> {
  return request.put('/message/notifications/read')
}

/** 获取未读通知数 */
export async function getUnreadCount(): Promise<number> {
  try {
    return await request.get('/message/notifications/unread-count')
  } catch {
    return mockNotifications.filter((n) => !n.isRead).length
  }
}
