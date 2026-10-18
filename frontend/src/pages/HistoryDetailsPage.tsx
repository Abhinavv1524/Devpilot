import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  Alert,
  Button,
  CircularProgress,
  Paper,
  Stack,
  Typography,
} from "@mui/material";
import { ExecutionTimeline } from "../components/tasks/ExecutionTimeline";
import { taskHistoryService } from "../services/taskHistoryService";
import type { TaskHistoryDto } from "../types/taskHistory";
import { getErrorMessage } from "../utils/errorMessage";

export function HistoryDetailsPage() {
  const { taskId = "" } = useParams<{ taskId: string }>();
  const [item, setItem] = useState<TaskHistoryDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadItem = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setItem(await taskHistoryService.getHistoryItem(taskId));
    } catch (loadError) {
      setError(getErrorMessage(loadError, "Unable to load this history item."));
    } finally {
      setLoading(false);
    }
  }, [taskId]);

  useEffect(() => {
    void loadItem();
  }, [loadItem]);

  if (loading) {
    return (
      <Stack alignItems="center" spacing={1} sx={{ py: 8 }}>
        <CircularProgress />
        <Typography color="text.secondary">Loading history item…</Typography>
      </Stack>
    );
  }

  if (error || !item) {
    return (
      <Stack spacing={2}>
        <Alert severity="error">{error ?? "History item not found."}</Alert>
        <Button component={Link} to="/history" sx={{ alignSelf: "flex-start" }}>
          Back to history
        </Button>
      </Stack>
    );
  }

  return (
    <Stack spacing={3}>
      <Button component={Link} to="/history" sx={{ alignSelf: "flex-start" }}>
        Back to history
      </Button>
      <Stack spacing={1}>
        <Typography variant="h4">Task summary</Typography>
        <Typography color="text.secondary">{item.userPrompt}</Typography>
        <Typography variant="body2" color="text.secondary">
          Task ID: {item.taskId}
        </Typography>
      </Stack>
      <Paper variant="outlined" sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Workflow status
        </Typography>
        <ExecutionTimeline status={item.taskStatus} />
        <Typography variant="body2" color="text.secondary">
          This is a status-derived display; history does not store individual
          agent events or timestamps.
        </Typography>
      </Paper>
      {[
        ["Research summary", item.researchSummary],
        ["Coding summary", item.codingSummary],
        ["Testing summary", item.testingSummary],
      ].map(([title, summary]) => (
        <Paper key={title} variant="outlined" sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            {title}
          </Typography>
          <Typography color={summary ? "text.primary" : "text.secondary"}>
            {summary || "No summary was recorded."}
          </Typography>
        </Paper>
      ))}
    </Stack>
  );
}
