import { useEffect, useState, type FormEvent } from "react";
import {
  Alert,
  Box,
  Button,
  Chip,
  Checkbox,
  CircularProgress,
  Divider,
  FormControl,
  FormControlLabel,
  IconButton,
  MenuItem,
  Paper,
  Select,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import ArrowForwardRoundedIcon from "@mui/icons-material/ArrowForwardRounded";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import AutoAwesomeRoundedIcon from "@mui/icons-material/AutoAwesomeRounded";
import FactCheckRoundedIcon from "@mui/icons-material/FactCheckRounded";
import MenuBookRoundedIcon from "@mui/icons-material/MenuBookRounded";
import RateReviewRoundedIcon from "@mui/icons-material/RateReviewRounded";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import type { SmokeTestCaseDto } from "../types/task";
import { taskService } from "../services/taskService";
import { workspaceStore, type ProjectWorkspace } from "../services/workspaceStore";
import { getErrorMessage } from "../utils/errorMessage";

interface SmokeTestDraft {
  inputLines: string;
  expectedOutput: string;
}

export function CreateTaskPage() {
  const { username } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [description, setDescription] = useState("");
  const [workspaces, setWorkspaces] = useState<ProjectWorkspace[]>([]);
  const [workspaceId, setWorkspaceId] = useState(searchParams.get("workspace") ?? "");
  const [smokeTests, setSmokeTests] = useState<SmokeTestDraft[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [confirmedEngineeringTask, setConfirmedEngineeringTask] = useState(false);

  useEffect(() => {
    try {
      if (!username) throw new Error("Sign in to access your workspaces.");
      const savedWorkspaces = workspaceStore.list(username);
      setWorkspaces(savedWorkspaces);
      if (!savedWorkspaces.some(({ id }) => id === workspaceId)) {
        setWorkspaceId(savedWorkspaces[0]?.id ?? "");
      }
    } catch (loadError) {
      setError(getErrorMessage(loadError, "Unable to load saved workspaces."));
    }
  }, [username, workspaceId]);

  const selectedWorkspace = workspaces.find(({ id }) => id === workspaceId);

  const updateSmokeTest = (
    index: number,
    field: keyof SmokeTestDraft,
    value: string,
  ) => {
    setSmokeTests((current) =>
      current.map((test, testIndex) =>
        testIndex === index ? { ...test, [field]: value } : test,
      ),
    );
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    if (!selectedWorkspace) {
      setError("Choose a workspace before starting an agent run.");
      return;
    }
    if (!confirmedEngineeringTask) {
      setError("Confirm that this is a concrete engineering change before submitting.");
      return;
    }
    setIsSubmitting(true);
    const validTests = smokeTests.filter((test) => test.expectedOutput.trim());
    const requestSmokeTests: SmokeTestCaseDto[] = validTests.map((test) => ({
      inputLines: test.inputLines
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter(Boolean),
      expectedOutput: test.expectedOutput.trim(),
    }));

    try {
      const task = await taskService.createTask({
        description: description.trim(),
        projectPath: selectedWorkspace.projectPath,
        smokeTests: requestSmokeTests,
      });
      let workspaceWarning: string | null = null;
      try {
        if (!username) throw new Error("Sign in to save this task to a workspace.");
        workspaceStore.addTask(username, selectedWorkspace.id, {
          taskId: task.taskId,
          description: task.description,
          status: task.status,
          createdAt: task.createdAt,
        });
      } catch (storeError) {
        workspaceWarning = `Task ${task.taskId} was created, but its workspace link could not be saved: ${getErrorMessage(storeError, "browser storage is unavailable.")}`;
      }
      navigate(`/tasks/${encodeURIComponent(task.taskId)}`, {
        replace: true,
        state: { created: true, workspaceId: selectedWorkspace.id, workspaceWarning },
      });
    } catch (createError) {
      setError(getErrorMessage(createError, "Unable to create the task."));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Stack spacing={3}>
      <Box
        sx={{
          p: { xs: 2.5, md: 3.5 },
          borderRadius: 3,
          border: 1,
          borderColor: "divider",
          background:
            "linear-gradient(105deg, rgba(109,74,255,0.17), rgba(18,26,43,0.3) 62%, rgba(24,190,177,0.08))",
        }}
      >
        <Chip
          icon={<AutoAwesomeRoundedIcon />}
          label="TASK WORKSHOP"
          size="small"
          sx={{
            mb: 1.5,
            color: "primary.light",
            bgcolor: "rgba(167,139,250,0.12)",
            "& .MuiChip-icon": { color: "inherit" },
          }}
        />
        <Typography variant="h3" sx={{ fontSize: { xs: "2rem", md: "2.65rem" } }}>
          What are we building?
        </Typography>
        <Typography color="text.secondary" sx={{ mt: 1, maxWidth: 650 }}>
          Define a concrete change in a project. This starts an engineering workflow—not a chat turn—and can take a few minutes.
        </Typography>
      </Box>

      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: { xs: "1fr", lg: "minmax(0, 1fr) 300px" },
          gap: 2.5,
          alignItems: "start",
        }}
      >
      <Paper
        component="form"
        onSubmit={handleSubmit}
        variant="outlined"
        sx={{ p: { xs: 2, sm: 3.5 } }}
      >
        <Stack spacing={3}>
          {error && <Alert severity="error">{error}</Alert>}
          <Box>
            <Typography variant="h6" gutterBottom>
              Project workspace
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Choose the project directory the agents should inspect and update.
            </Typography>
          </Box>
          {workspaces.length === 0 ? (
            <Alert
              severity="info"
              action={<Button component={Link} to="/">Add workspace</Button>}
            >
              Create a workspace first. Workspaces point to existing relative paths under the AI Agent's configured root.
            </Alert>
          ) : (
            <FormControl fullWidth>
              <Select
                value={selectedWorkspace ? selectedWorkspace.id : ""}
                onChange={(event) => setWorkspaceId(event.target.value)}
                displayEmpty
                disabled={isSubmitting}
                inputProps={{ "aria-label": "Project workspace" }}
              >
                {workspaces.map((workspace) => (
                  <MenuItem key={workspace.id} value={workspace.id}>
                    {workspace.name} · {workspace.projectPath}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}
          {selectedWorkspace && (
            <Paper variant="outlined" sx={{ px: 2, py: 1.5 }}>
              <Typography variant="caption" color="text.secondary">AGENT PROJECT PATH</Typography>
              <Typography sx={{ fontFamily: "monospace", overflowWrap: "anywhere" }}>{selectedWorkspace.projectPath}</Typography>
            </Paper>
          )}
          <Box>
            <Typography variant="h6" gutterBottom>
              Engineering change
            </Typography>
            <Typography variant="body2" color="text.secondary">
              State the expected behavior, relevant constraints, and what a correct result looks like.
            </Typography>
          </Box>
          <TextField
            label="Change request"
            placeholder="Example: Add a GET endpoint that returns menu items with name, category, and price. Follow the existing controller and DTO patterns."
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            helperText={`${description.length}/2000 · This request will be analyzed by the agents; it is not a general-purpose chat message.`}
            inputProps={{ maxLength: 2000 }}
            minRows={4}
            multiline
            required
            fullWidth
          />
          <FormControlLabel
            control={
              <Checkbox
                checked={confirmedEngineeringTask}
                onChange={(event) => setConfirmedEngineeringTask(event.target.checked)}
                disabled={isSubmitting}
              />
            }
            label="I intend to make a concrete engineering change in this project."
          />

          <Box>
            <Stack
              direction="row"
              alignItems="center"
              justifyContent="space-between"
              sx={{ mb: 1 }}
            >
              <Box>
                <Typography variant="h6">Smoke tests</Typography>
                <Typography variant="body2" color="text.secondary">
                  Optional checks the Testing Agent can run after approval.
                </Typography>
              </Box>
              <Button
                startIcon={<AddIcon />}
                onClick={() =>
                  setSmokeTests((current) => [
                    ...current,
                    { inputLines: "", expectedOutput: "" },
                  ])
                }
                disabled={isSubmitting}
              >
                Add a test
              </Button>
            </Stack>
            <Stack spacing={2}>
              {smokeTests.map((test, index) => (
                <Paper key={index} variant="outlined" sx={{ p: 2 }}>
                  <Stack spacing={2}>
                    <Stack direction="row" justifyContent="space-between">
                      <Typography variant="subtitle1">
                        Test {index + 1}
                      </Typography>
                      <IconButton
                        aria-label={`Remove test ${index + 1}`}
                        onClick={() =>
                          setSmokeTests((current) =>
                            current.filter((_, testIndex) => testIndex !== index),
                          )
                        }
                        disabled={isSubmitting}
                        size="small"
                      >
                        <DeleteOutlineIcon />
                      </IconButton>
                    </Stack>
                    <TextField
                      label="Input lines"
                      value={test.inputLines}
                      onChange={(event) =>
                        updateSmokeTest(index, "inputLines", event.target.value)
                      }
                      helperText="Enter one input line per row."
                      minRows={2}
                      multiline
                      fullWidth
                    />
                    <TextField
                      label="Expected output"
                      value={test.expectedOutput}
                      onChange={(event) =>
                        updateSmokeTest(
                          index,
                          "expectedOutput",
                          event.target.value,
                        )
                      }
                      required
                      fullWidth
                    />
                  </Stack>
                </Paper>
              ))}
            </Stack>
          </Box>

          <Divider />
          <Box>
            <Button
              type="submit"
              variant="contained"
              size="large"
              endIcon={!isSubmitting && <ArrowForwardRoundedIcon />}
              disabled={isSubmitting || !description.trim() || !selectedWorkspace || !confirmedEngineeringTask}
              sx={{ minWidth: 180, minHeight: 48 }}
            >
              {isSubmitting ? (
                <Stack direction="row" spacing={1} alignItems="center">
                  <CircularProgress size={20} color="inherit" />
                  <span>Creating task…</span>
                </Stack>
              ) : (
                "Start agent workflow"
              )}
            </Button>
          </Box>
        </Stack>
      </Paper>

      <Stack spacing={2}>
        <Paper variant="outlined" sx={{ p: 2.5 }}>
          <Typography variant="overline" color="text.secondary">
            YOUR AGENT CREW
          </Typography>
          <Stack spacing={2} sx={{ mt: 2 }}>
            {[
              {
                title: "Research",
                detail: "Inspects this project for the change",
                icon: <MenuBookRoundedIcon />,
                color: "#5eead4",
              },
              {
                title: "Coding",
                detail: "Prepares a file-by-file proposal",
                icon: <AutoAwesomeRoundedIcon />,
                color: "#a78bfa",
              },
              {
                title: "Testing",
                detail: "Runs checks when the route requires them",
                icon: <FactCheckRoundedIcon />,
                color: "#f5bd5c",
              },
            ].map((agent, index) => (
              <Stack key={agent.title} direction="row" spacing={1.5} alignItems="center">
                <Box
                  sx={{
                    width: 38,
                    height: 38,
                    flexShrink: 0,
                    borderRadius: 2,
                    display: "grid",
                    placeItems: "center",
                    color: agent.color,
                    bgcolor: `${agent.color}18`,
                  }}
                >
                  {agent.icon}
                </Box>
                <Box sx={{ minWidth: 0 }}>
                  <Typography variant="subtitle2">{agent.title} Agent</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {agent.detail}
                  </Typography>
                </Box>
                {index < 2 && (
                  <ArrowForwardRoundedIcon
                    sx={{ ml: "auto !important", color: "text.disabled", fontSize: 16 }}
                  />
                )}
              </Stack>
            ))}
          </Stack>
        </Paper>

        <Paper variant="outlined" sx={{ p: 2.5 }}>
          <Box
            sx={{
              width: 42,
              height: 42,
              display: "grid",
              placeItems: "center",
              borderRadius: 2,
              color: "secondary.main",
              bgcolor: "rgba(94,234,212,0.1)",
              mb: 1.5,
            }}
          >
            <RateReviewRoundedIcon />
          </Box>
          <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
            You’re in control
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75 }}>
            Proposed changes are shown for review. Nothing is applied until you
            approve the proposal.
          </Typography>
        </Paper>
      </Stack>
      </Box>
    </Stack>
  );
}
