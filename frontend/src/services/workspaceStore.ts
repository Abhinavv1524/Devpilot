export interface WorkspaceTaskSummary {
  taskId: string;
  description: string;
  status: string;
  createdAt: string;
}

export interface ProjectWorkspace {
  id: string;
  name: string;
  projectPath: string;
  createdAt: string;
  tasks: WorkspaceTaskSummary[];
}

function storageKey(username: string): string {
  if (!username.trim()) {
    throw new Error("A signed-in user is required to access workspaces.");
  }
  return `devpilot.workspaces.v1.${encodeURIComponent(username.trim())}`;
}

function isWorkspace(value: unknown): value is ProjectWorkspace {
  if (typeof value !== "object" || value === null) return false;
  const workspace = value as Record<string, unknown>;
  return (
    typeof workspace.id === "string" &&
    typeof workspace.name === "string" &&
    typeof workspace.projectPath === "string" &&
    typeof workspace.createdAt === "string" &&
    Array.isArray(workspace.tasks) &&
    workspace.tasks.every((task) => {
      if (typeof task !== "object" || task === null) return false;
      const summary = task as Record<string, unknown>;
      return (
        typeof summary.taskId === "string" &&
        typeof summary.description === "string" &&
        typeof summary.status === "string" &&
        typeof summary.createdAt === "string"
      );
    })
  );
}

function readWorkspaces(username: string): ProjectWorkspace[] {
  const storedValue = window.localStorage.getItem(storageKey(username));
  if (storedValue === null) return [];

  const parsed: unknown = JSON.parse(storedValue);
  if (!Array.isArray(parsed) || !parsed.every(isWorkspace)) {
    throw new Error("Saved workspace data is invalid. Clear this browser's DevPilot workspace data and try again.");
  }
  return parsed;
}

function writeWorkspaces(username: string, workspaces: ProjectWorkspace[]): void {
  window.localStorage.setItem(storageKey(username), JSON.stringify(workspaces));
}

export const workspaceStore = {
  list(username: string): ProjectWorkspace[] {
    return readWorkspaces(username).sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  },

  get(username: string, workspaceId: string): ProjectWorkspace | undefined {
    return readWorkspaces(username).find((workspace) => workspace.id === workspaceId);
  },

  findWorkspaceIdForTask(username: string, taskId: string): string | undefined {
    return readWorkspaces(username).find((workspace) =>
      workspace.tasks.some((task) => task.taskId === taskId),
    )?.id;
  },

  create(username: string, name: string, projectPath: string): ProjectWorkspace {
    const workspaces = readWorkspaces(username);
    const normalizedPath = projectPath.trim().replace(/[\\/]+$/, "").toLowerCase();
    if (workspaces.some((workspace) =>
      workspace.projectPath.replace(/[\\/]+$/, "").toLowerCase() === normalizedPath
    )) {
      throw new Error("A workspace already uses this project path.");
    }

    const workspace: ProjectWorkspace = {
      id: crypto.randomUUID(),
      name: name.trim(),
      projectPath: projectPath.trim().replace(/[\\/]+$/, ""),
      createdAt: new Date().toISOString(),
      tasks: [],
    };
    writeWorkspaces(username, [workspace, ...workspaces]);
    return workspace;
  },

  addTask(username: string, workspaceId: string, task: WorkspaceTaskSummary): void {
    const workspaces = readWorkspaces(username);
    const workspaceIndex = workspaces.findIndex(({ id }) => id === workspaceId);
    if (workspaceIndex === -1) throw new Error("The selected workspace no longer exists.");

    const workspace = workspaces[workspaceIndex];
    workspaces[workspaceIndex] = {
      ...workspace,
      tasks: [
        task,
        ...workspace.tasks.filter(({ taskId }) => taskId !== task.taskId),
      ],
    };
    writeWorkspaces(username, workspaces);
  },

  updateTaskStatus(username: string, taskId: string, status: string): void {
    const workspaces = readWorkspaces(username);
    let changed = false;
    const updated = workspaces.map((workspace) => ({
      ...workspace,
      tasks: workspace.tasks.map((task) => {
        if (task.taskId !== taskId) return task;
        changed = true;
        return { ...task, status };
      }),
    }));
    if (changed) writeWorkspaces(username, updated);
  },
};
