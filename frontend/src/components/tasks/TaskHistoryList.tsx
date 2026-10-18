import {
  Button,
  Card,
  CardContent,
  Chip,
  Stack,
  Typography,
} from "@mui/material";
import { Link } from "react-router-dom";
import type { TaskHistoryDto } from "../../types/taskHistory";

interface TaskHistoryListProps {
  tasks: TaskHistoryDto[];
  emptyMessage?: string;
}

function formatDate(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Date unavailable" : date.toLocaleString();
}

export function TaskHistoryList({
  tasks,
  emptyMessage = "No completed or failed task history is available yet.",
}: TaskHistoryListProps) {
  if (tasks.length === 0) {
    return <Typography color="text.secondary">{emptyMessage}</Typography>;
  }

  return (
    <Stack spacing={1.5}>
      {tasks.map((task) => (
        <Card key={task.taskId} variant="outlined">
          <CardContent>
            <Stack
              direction={{ xs: "column", sm: "row" }}
              spacing={1.5}
              alignItems={{ sm: "center" }}
              justifyContent="space-between"
            >
              <Stack spacing={0.75} sx={{ minWidth: 0 }}>
                <Typography sx={{ fontWeight: 600, overflowWrap: "anywhere" }}>
                  {task.userPrompt}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Created {formatDate(task.createdAt)}
                </Typography>
              </Stack>
              <Stack direction="row" spacing={1} alignItems="center">
                <Chip
                  label={task.taskStatus}
                  size="small"
                  color={task.taskStatus === "COMPLETED" ? "success" : "error"}
                />
                <Button
                  component={Link}
                  to={`/history/${encodeURIComponent(task.taskId)}`}
                  size="small"
                >
                  View summary
                </Button>
              </Stack>
            </Stack>
          </CardContent>
        </Card>
      ))}
    </Stack>
  );
}
