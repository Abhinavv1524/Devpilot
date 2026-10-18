import { lazy, Suspense } from "react";
import { CircularProgress, Stack } from "@mui/material";
import { Route, Routes } from "react-router-dom";
import { AppLayout } from "../components/layout/AppLayout";
import { NotFoundPage } from "../pages/NotFoundPage";
import { LoginPage } from "../pages/LoginPage";
import { ProtectedRoute } from "../routing/ProtectedRoute";

const DashboardPage = lazy(() =>
  import("../pages/DashboardPage").then((module) => ({
    default: module.DashboardPage,
  })),
);
const WorkspacePage = lazy(() =>
  import("../pages/WorkspacePage").then((module) => ({
    default: module.WorkspacePage,
  })),
);
const CreateTaskPage = lazy(() =>
  import("../pages/CreateTaskPage").then((module) => ({
    default: module.CreateTaskPage,
  })),
);
const TaskDetailsPage = lazy(() =>
  import("../pages/TaskDetailsPage").then((module) => ({
    default: module.TaskDetailsPage,
  })),
);
const HistoryPage = lazy(() =>
  import("../pages/HistoryPage").then((module) => ({
    default: module.HistoryPage,
  })),
);
const HistoryDetailsPage = lazy(() =>
  import("../pages/HistoryDetailsPage").then((module) => ({
    default: module.HistoryDetailsPage,
  })),
);
const KnowledgePage = lazy(() =>
  import("../pages/KnowledgePage").then((module) => ({
    default: module.KnowledgePage,
  })),
);

export default function App() {
  return (
    <Suspense
      fallback={
        <Stack alignItems="center" justifyContent="center" sx={{ minHeight: "60vh" }}>
          <CircularProgress />
        </Stack>
      }
    >
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route index element={<DashboardPage />} />
            <Route path="workspaces/:workspaceId" element={<WorkspacePage />} />
            <Route path="tasks/new" element={<CreateTaskPage />} />
            <Route path="tasks/:taskId" element={<TaskDetailsPage />} />
            <Route
              path="tasks/:taskId/approval"
              element={<TaskDetailsPage />}
            />
            <Route path="history" element={<HistoryPage />} />
            <Route path="history/:taskId" element={<HistoryDetailsPage />} />
            <Route path="knowledge" element={<KnowledgePage />} />
          </Route>
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  );
}
