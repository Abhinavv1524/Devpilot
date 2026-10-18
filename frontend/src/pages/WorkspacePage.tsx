import { useEffect, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Chip,
  Paper,
  Stack,
  Typography,
} from "@mui/material";
import AddRoundedIcon from "@mui/icons-material/AddRounded";
import ArrowBackRoundedIcon from "@mui/icons-material/ArrowBackRounded";
import FolderOpenRoundedIcon from "@mui/icons-material/FolderOpenRounded";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { workspaceStore, type ProjectWorkspace } from "../services/workspaceStore";

export function WorkspacePage() {
  const { username } = useAuth();
  const { workspaceId = "" } = useParams();
  const [workspace, setWorkspace] = useState<ProjectWorkspace | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    try {
      if (!username) throw new Error("Sign in to view this workspace.");
      setWorkspace(workspaceStore.get(username, workspaceId) ?? null);
      setError(null);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Unable to load this workspace.");
    }
  }, [username, workspaceId]);

  if (error) return <Alert severity="error">{error}</Alert>;
  if (!workspace) {
    return (
      <Stack spacing={2}>
        <Alert severity="warning">This workspace is not saved in this browser.</Alert>
        <Button component={Link} to="/" startIcon={<ArrowBackRoundedIcon />} sx={{ alignSelf: "flex-start" }}>
          Workspaces
        </Button>
      </Stack>
    );
  }

  return (
    <Stack spacing={2.5}>
      <Button component={Link} to="/" startIcon={<ArrowBackRoundedIcon />} sx={{ alignSelf: "flex-start", color: "text.secondary", px: 0.5 }}>
        All workspaces
      </Button>
      <Paper variant="outlined" sx={{ p: { xs: 2.5, md: 3.5 } }}>
        <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" spacing={2}>
          <Stack direction="row" spacing={1.5} alignItems="flex-start">
            <FolderOpenRoundedIcon color="secondary" sx={{ mt: 0.5 }} />
            <Box sx={{ minWidth: 0 }}>
              <Typography variant="h4" sx={{ overflowWrap: "anywhere" }}>{workspace.name}</Typography>
              <Typography color="text.secondary" sx={{ mt: 0.75, fontFamily: "monospace", overflowWrap: "anywhere" }}>
                {workspace.projectPath}
              </Typography>
            </Box>
          </Stack>
          <Button
            component={Link}
            to={`/tasks/new?workspace=${encodeURIComponent(workspace.id)}`}
            variant="contained"
            startIcon={<AddRoundedIcon />}
            sx={{ alignSelf: { sm: "center" }, flexShrink: 0 }}
          >
            New change
          </Button>
        </Stack>
      </Paper>

      <Stack direction="row" alignItems="baseline" justifyContent="space-between">
        <Box>
          <Typography variant="h5">Work in this project</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
            Each request is a separate agent run against this same project path.
          </Typography>
        </Box>
        <Typography variant="caption" color="text.secondary">{workspace.tasks.length} runs</Typography>
      </Stack>

      {workspace.tasks.length === 0 ? (
        <Paper variant="outlined" sx={{ p: 4, textAlign: "center" }}>
          <Typography variant="h6">Start the first change</Typography>
          <Typography color="text.secondary" sx={{ mt: 0.75, mb: 2 }}>
            Describe a concrete engineering change. DevPilot will research the project and prepare a proposal for review.
          </Typography>
          <Button component={Link} to={`/tasks/new?workspace=${encodeURIComponent(workspace.id)}`} variant="contained" startIcon={<AddRoundedIcon />}>
            Create a task
          </Button>
        </Paper>
      ) : (
        <Stack spacing={1.25}>
          {workspace.tasks.map((task) => (
            <Paper key={task.taskId} component={Link} to={`/tasks/${encodeURIComponent(task.taskId)}`} variant="outlined" sx={{ p: 2, color: "inherit", textDecoration: "none", transition: "border-color 160ms ease", "&:hover": { borderColor: "primary.main" } }}>
              <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5} justifyContent="space-between" alignItems={{ sm: "center" }}>
                <Box sx={{ minWidth: 0 }}>
                  <Typography sx={{ fontWeight: 650, overflowWrap: "anywhere" }}>{task.description}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {new Date(task.createdAt).toLocaleString()} · {task.taskId}
                  </Typography>
                </Box>
                <Chip size="small" label={task.status.replaceAll("_", " ")} color={task.status === "COMPLETED" ? "success" : task.status === "FAILED" ? "error" : task.status === "AWAITING_APPROVAL" ? "warning" : "default"} />
              </Stack>
            </Paper>
          ))}
        </Stack>
      )}
      <Alert severity="info" variant="outlined">
        Workspace membership and run links are stored in this browser. The backend still owns the project files, task state, and approval/apply workflow.
      </Alert>
    </Stack>
  );
}
