import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/home/index.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/index.vue'),
      meta: { public: true }
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/register/index.vue'),
      meta: { public: true }
    },
    {
      path: '/forgot-password',
      name: 'forgot-password',
      component: () => import('@/views/forgot-password/index.vue'),
      meta: { public: true }
    },
    {
      path: '/courses',
      name: 'courseList',
      component: () => import('@/views/courses/index.vue')
    },
    {
      path: '/courses/:id',
      name: 'courseDetail',
      component: () => import('@/views/courses/detail.vue')
    },
    {
      path: '/admin',
      redirect: '/admin/departments'
    },
    {
      path: '/admin/departments',
      name: 'adminDepartments',
      component: () => import('@/views/admin/DepartmentManage.vue')
    },
    {
      path: '/admin/students',
      name: 'adminStudents',
      component: () => import('@/views/admin/StudentManage.vue')
    },
    {
      path: '/admin/teachers',
      name: 'adminTeachers',
      component: () => import('@/views/admin/TeacherManage.vue')
    },
    {
      path: '/admin/learning-status',
      name: 'adminLearningStatus',
      component: () => import('@/views/admin/LearningStatus.vue')
    },
    {
      path: '/admin/settings',
      name: 'adminSettings',
      component: () => import('@/views/admin/InstitutionSettings.vue')
    },
    {
      path: '/teacher',
      redirect: '/teacher/courses'
    },
    {
      path: '/teacher/courses',
      name: 'teacherCourses',
      component: () => import('@/views/teacher/CourseManage.vue')
    },
    {
      path: '/teacher/courses/:id',
      name: 'teacherCourseDetail',
      component: () => import('@/views/teacher/CourseDetail.vue')
    },
    {
      path: '/teacher/homework',
      name: 'teacherHomework',
      component: () => import('@/views/teacher/HomeworkManage.vue')
    },
    {
      path: '/teacher/homework/assign',
      name: 'teacherHomeworkAssign',
      component: () => import('@/views/teacher/HomeworkAssign.vue')
    },
    {
      path: '/teacher/exams',
      name: 'teacherExams',
      component: () => import('@/views/teacher/ExamManage.vue')
    },
    {
      path: '/teacher/question-bank',
      name: 'teacherQuestionBank',
      component: () => import('@/views/teacher/QuestionBank.vue')
    },
    {
      path: '/teacher/question-bank/:bankId/questions',
      name: 'teacherQuestionManage',
      component: () => import('@/views/teacher/QuestionManage.vue')
    },
    {
      path: '/teacher/question-bank/:bankId/editor',
      name: 'teacherQuestionEditor',
      component: () => import('@/views/teacher/QuestionEditor.vue')
    },
    {
      path: '/homework/:id',
      name: 'homeworkDo',
      component: () => import('@/views/homework/do.vue')
    },
    {
      path: '/messages',
      name: 'messages',
      component: () => import('@/views/messages/index.vue')
    },
    {
      path: '/test-oj',
      name: 'testOJ',
      component: () => import('@/views/test-oj.vue'),
      meta: { public: true }
    }
  ]
})

// 全局前置守卫 - 登录检查
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const isPublicPage = to.meta.public === true

  // 如果没有 token 且不是公开页面，重定向到登录页
  if (!token && !isPublicPage) {
    next({ name: 'login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
