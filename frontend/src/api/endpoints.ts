export const endpoints = {
  auth: {
    login: "/api/auth/login",
    signup: "/api/auth/signup",
  },
  tasks: {
    collection: "/api/tasks",
    byId: (taskId: string) => `/api/tasks/${encodeURIComponent(taskId)}`,
    approve: (taskId: string) =>
      `/api/tasks/${encodeURIComponent(taskId)}/approve`,
    cancel: (taskId: string) =>
      `/api/tasks/${encodeURIComponent(taskId)}/cancel`,
  },
  taskHistory: {
    collection: "/tasks/history",
    byId: (taskId: string) =>
      `/tasks/history/${encodeURIComponent(taskId)}`,
  },
  knowledge: {
    ask: "/api/knowledge/ask",
  },
} as const;
