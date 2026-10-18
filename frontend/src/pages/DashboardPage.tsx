import { useEffect, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Card,
  CardActionArea,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import AddRoundedIcon from "@mui/icons-material/AddRounded";
import CodeRoundedIcon from "@mui/icons-material/CodeRounded";
import FolderOpenRoundedIcon from "@mui/icons-material/FolderOpenRounded";
import ArrowForwardRoundedIcon from "@mui/icons-material/ArrowForwardRounded";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { workspaceStore, type ProjectWorkspace } from "../services/workspaceStore";

export function DashboardPage() {
  const { username } = useAuth();
  const [workspaces, setWorkspaces] = useState<ProjectWorkspace[]>([]);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [name, setName] = useState("");
  const [projectPath, setProjectPath] = useState("");
  const [error, setError] = useState<string | null>(null);

  const refresh = () => {
    try {
      if (!username) throw new Error("Sign in to view your workspaces.");
      setWorkspaces(workspaceStore.list(username));
      setError(null);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Unable to read saved workspaces.");
    }
  };

  useEffect(() => refresh(), [username]);

  const createWorkspace = () => {
    const path = projectPath.trim();
    const segments = path.split(/[\\/]+/);
    if (!name.trim() || !path) {
      setError("Enter a workspace name and a project path.");
      return;
    }
    if (/^(?:[\\/]|[a-z]:)/i.test(path) || segments.includes("..") || path === ".") {
      setError("Use a project path relative to the AI Agent's configured workspace, without '..'.");
      return;
    }
    try {
      if (!username) throw new Error("Sign in to create a workspace.");
      workspaceStore.create(username, name, path);
      setName("");
      setProjectPath("");
      setDialogOpen(false);
      refresh();
    } catch (createError) {
      setError(createError instanceof Error ? createError.message : "Unable to save this workspace.");
    }
  };

  return (
    <Stack spacing={3}>
      <Paper
        variant="outlined"
        sx={{
          p: { xs: 2.5, md: 4 },
          background: "linear-gradient(110deg, rgba(109,74,255,0.16), rgba(18,26,43,0.82) 58%, rgba(24,190,177,0.08))",
        }}
      >
        <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" spacing={2}>
          <Box>
            <Chip icon={<CodeRoundedIcon />} label="DEVPILOT WORKBENCH" size="small" sx={{ mb: 1.5 }} />
            <Typography variant="h3" sx={{ fontSize: { xs: "2rem", md: "2.7rem" } }}>
              Your projects, one workspace at a time.
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 1, maxWidth: 680 }}>
              Keep engineering requests tied to a project path. Return to a workspace to continue the work with another reviewed task.
            </Typography>
          </Box>
          <Button
            variant="contained"
            startIcon={<AddRoundedIcon />}
            onClick={() => { setError(null); setDialogOpen(true); }}
            sx={{ alignSelf: { sm: "center" }, flexShrink: 0 }}
          >
            Add workspace
          </Button>
        </Stack>
      </Paper>

      {error && <Alert severity="error">{error}</Alert>}
      <Stack direction="row" alignItems="baseline" justifyContent="space-between">
        <Box>
          <Typography variant="h5">Workspaces</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
            Saved in this browser and mapped to paths inside the agent's configured workspace.
          </Typography>
        </Box>
        <Typography variant="caption" color="text.secondary">{workspaces.length} projects</Typography>
      </Stack>

      {workspaces.length === 0 ? (
        <Paper variant="outlined" sx={{ p: { xs: 3, md: 5 }, textAlign: "center" }}>
          <FolderOpenRoundedIcon color="primary" sx={{ fontSize: 42, mb: 1 }} />
          <Typography variant="h6">No workspaces yet</Typography>
          <Typography color="text.secondary" sx={{ mt: 0.75, mb: 2.5 }}>
            Add an existing project path to keep follow-up changes organized.
          </Typography>
          <Button variant="contained" startIcon={<AddRoundedIcon />} onClick={() => setDialogOpen(true)}>
            Add your first workspace
          </Button>
        </Paper>
      ) : (
        <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", md: "repeat(2, minmax(0, 1fr))" }, gap: 2 }}>
          {workspaces.map((workspace) => {
            const latestTask = workspace.tasks[0];
            return (
              <Card key={workspace.id} variant="outlined">
                <CardActionArea component={Link} to={`/workspaces/${encodeURIComponent(workspace.id)}`}>
                  <CardContent sx={{ p: 2.5 }}>
                    <Stack direction="row" spacing={1.5} alignItems="flex-start">
                      <Box sx={{ width: 42, height: 42, display: "grid", placeItems: "center", color: "secondary.main", bgcolor: "action.selected", borderRadius: 2 }}>
                        <FolderOpenRoundedIcon />
                      </Box>
                      <Box sx={{ minWidth: 0, flex: 1 }}>
                        <Typography variant="h6" noWrap>{workspace.name}</Typography>
                        <Typography variant="body2" color="text.secondary" noWrap sx={{ fontFamily: "monospace" }}>
                          {workspace.projectPath}
                        </Typography>
                      </Box>
                      <ArrowForwardRoundedIcon sx={{ color: "text.disabled", mt: 1 }} />
                    </Stack>
                    <Stack direction="row" spacing={1} sx={{ mt: 2.5 }}>
                      <Chip size="small" label={`${workspace.tasks.length} task${workspace.tasks.length === 1 ? "" : "s"}`} />
                      {latestTask && <Chip size="small" label={latestTask.status.replaceAll("_", " ")} color={latestTask.status === "COMPLETED" ? "success" : latestTask.status === "FAILED" ? "error" : "default"} />}
                    </Stack>
                    {latestTask && (
                      <Typography variant="body2" color="text.secondary" noWrap sx={{ mt: 1.5 }}>
                        Latest: {latestTask.description}
                      </Typography>
                    )}
                  </CardContent>
                </CardActionArea>
              </Card>
            );
          })}
        </Box>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Connect a project workspace</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            <Typography variant="body2" color="text.secondary">
              This creates a browser-side project entry. The path must already exist inside the AI Agent's configured workspace; this UI does not create or browse server folders.
            </Typography>
            <TextField autoFocus label="Workspace name" value={name} onChange={(event) => setName(event.target.value)} placeholder="e.g. Menu API" required fullWidth />
            <TextField label="Project path" value={projectPath} onChange={(event) => setProjectPath(event.target.value)} placeholder="e.g. menu-service" helperText="Relative path under the agent's configured workspace." required fullWidth />
            {error && <Alert severity="error">{error}</Alert>}
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={createWorkspace}>Add workspace</Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
