# bucher-frontend

智学平台前端项目，基于 Vue 3 + TypeScript + Element Plus 构建。

## 技术栈

- Vue 3.4
- TypeScript 5.4
- Vue Router 4
- Pinia 2
- Element Plus 2.6
- Axios
- Vite 5

## 开发环境要求

- Node.js >= 18.0.0
- pnpm >= 8.0.0（推荐）或 npm >= 9.0.0

## 安装依赖

```bash
npm install
# 或
pnpm install
```

## 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:5173

## 构建生产版本

```bash
npm run build
```

## 代码规范

```bash
# ESLint 检查
npm run lint

# Prettier 格式化
npm run format
```

## 目录结构

```
src/
├── api/          # API 接口
├── assets/       # 静态资源
├── components/   # 公共组件
├── layouts/      # 布局组件
├── router/       # 路由配置
├── stores/       # Pinia 状态管理
├── styles/       # 全局样式
├── utils/        # 工具函数
└── views/        # 页面组件
```
