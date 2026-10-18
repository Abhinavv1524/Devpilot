import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Alert,
  CircularProgress,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { TaskHistoryList } from "../components/tasks/TaskHistoryList";
import { taskHistoryService } from "../services/taskHistoryService";
import type { TaskHistoryDto } from "../types/taskHistory";
import { getErrorMessage } from "../utils/errorMessage";

export function HistoryPage() {
  const [tasks, setTasks] = useState<TaskHistoryDto[]>([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadHistory = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setTasks(await taskHistoryService.getHistory());
    } catch (loadError) {
      setError(getErrorMessage(loadError, "Unable to load task history."));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadHistory();
  }, [loadHistory]);

  const filteredTasks = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    if (!normalizedQuery) return tasks;
    return tasks.filter((task) =>
      [
        task.userPrompt,
        task.taskStatus,
        task.researchSummary,
        task.codingSummary,
        task.testingSummary,
      ]
        .filter(Boolean)
        .some((value) => value!.toLowerCase().includes(normalizedQuery)),
    );
  }, [query, tasks]);

  return (
    <Stack spacing={3}>
      <Stack spacing={0.5}>
        <Typography variant="h4">Task history</Typography>
        <Typography color="text.secondary">
          Completed and failed task summaries available from the backend.
        </Typography>
      </Stack>
      <Alert severity="info">
        The history API currently returns at most 20 terminal tasks. Active and
        awaiting-approval tasks aren’t included.
      </Alert>
      <TextField
        label="Filter returned history"
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        fullWidth
      />
      {error && <Alert severity="error">{error}</Alert>}
      {loading ? (
        <Stack alignItems="center" spacing={1} sx={{ py: 5 }}>
          <CircularProgress />
          <Typography color="text.secondary">Loading task history…</Typography>
        </Stack>
      ) : (
        <TaskHistoryList
          tasks={filteredTasks}
          emptyMessage={
            query ? "No history items match this filter." : undefined
          }
        />
      )}
    </Stack>
  );
}
