import { useCallback, useEffect, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Tab,
  Tabs,
  Paper,
  Stack,
  Typography,
} from "@mui/material";
import { alpha } from "@mui/material/styles";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutline";
import ArrowBackRoundedIcon from "@mui/icons-material/ArrowBackRounded";
import AutoAwesomeRoundedIcon from "@mui/icons-material/AutoAwesomeRounded";
import CodeRoundedIcon from "@mui/icons-material/CodeRounded";
import FactCheckRoundedIcon from "@mui/icons-material/FactCheckRounded";
import MenuBookRoundedIcon from "@mui/icons-material/MenuBookRounded";
import { ExecutionTimeline } from "../components/tasks/ExecutionTimeline";
import { ProposalWorkbench } from "../components/tasks/ProposalWorkbench";
import { useAuth } from "../auth/AuthContext";
import { taskService } from "../services/taskService";
import { workspaceStore } from "../services/workspaceStore";
import type {
  ProjectAnalysisDto,
  TaskStateDto,
  TestingResultDto,
} from "../types/task";
import { getErrorMessage } from "../utils/errorMessage";

function ContentList({ items }: { items: string[] | undefined }) {
  if (!items?.length) {
    return <Typography color="text.secondary">None reported.</Typography>;
  }
  return (
    <Box component="ul" sx={{ mt: 1, mb: 0, pl: 3 }}>
      {items.map((item, index) => (
        <li key={`${item}-${index}`}>
          <Typography component="span">{item}</Typography>
        </li>
      ))}
    </Box>
  );
}

function Section({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <Paper component="section" variant="outlined" sx={{ p: { xs: 2, sm: 3 } }}>
      <Typography variant="h6" gutterBottom>
        {title}
      </Typography>
      {children}
    </Paper>
  );
}

function ResearchAnalysis({ analysis }: { analysis: ProjectAnalysisDto | null }) {
  if (!analysis) {
    return <Typography color="text.secondary">No research analysis is available.</Typography>;
  }

  const fields: Array<[string, string[]]> = [
    ["Technologies", analysis.technologies],
    ["Requirements", analysis.requirements],
    ["Files to modify", analysis.filesToModify],
    ["New files", analysis.newFiles],
    ["Dependencies", analysis.dependencies],
    ["Recommendations", analysis.recommendations],
    ["Assumptions", analysis.assumptions],
  ];

  return (
    <Stack spacing={2}>
      {analysis.task && <Typography>{analysis.task}</Typography>}
      {analysis.projectType && (
        <Typography>
          <strong>Project type:</strong> {analysis.projectType}
        </Typography>
      )}
      {fields.map(([label, items]) =>
        items.length ? (
          <Box key={label}>
            <Typography variant="subtitle2">{label}</Typography>
            <ContentList items={items} />
          </Box>
        ) : null,
      )}
    </Stack>
  );
}

function TestingResult({ result }: { result: TestingResultDto | null }) {
  if (!result) {
    return <Typography color="text.secondary">Testing has not reported a result.</Typography>;
  }
  return (
    <Stack spacing={1}>
      {result.status && <Chip label={result.status} size="small" sx={{ alignSelf: "flex-start" }} />}
      {result.summary && <Typography>{result.summary}</Typography>}
      {result.testType && (
        <Typography><strong>Test type:</strong> {result.testType}</Typography>
      )}
      {result.testCommand && (
        <Typography><strong>Command:</strong> {result.testCommand}</Typography>
      )}
      {result.exitCode !== null && (
        <Typography><strong>Exit code:</strong> {result.exitCode}</Typography>
      )}
      {result.timedOut && <Alert severity="warning">The test run timed out.</Alert>}
      {result.errors.length > 0 && (
        <>
          <Typography variant="subtitle2">Errors</Typography>
          <ContentList items={result.errors} />
        </>
      )}
      {result.standardOutput && (
        <Box>
          <Typography variant="subtitle2">Standard output</Typography>
          <Box component="pre" sx={{ overflow: "auto", whiteSpace: "pre-wrap" }}>
            {result.standardOutput}
          </Box>
        </Box>
      )}
      {result.standardError && (
        <Box>
          <Typography variant="subtitle2">Standard error</Typography>
          <Box component="pre" sx={{ overflow: "auto", whiteSpace: "pre-wrap" }}>
            {result.standardError}
          </Box>
        </Box>
      )}
    </Stack>
  );
}

export function TaskDetailsPage() {
  const { username } = useAuth();
  const { taskId } = useParams<{ taskId: string }>();
  const location = useLocation();
  const navigationState = location.state as {
    created?: boolean;
    workspaceId?: string;
    workspaceWarning?: string | null;
  } | null;
  const [task, setTask] = useState<TaskStateDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [approving, setApproving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [approvalError, setApprovalError] = useState<string | null>(null);
  const [workspaceId, setWorkspaceId] = useState<string | undefined>();
  const [workspaceWarning, setWorkspaceWarning] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState(0);
  const [cancelling, setCancelling] = useState(false);
  const [cancelError, setCancelError] = useState<string | null>(null);
  const createdFromForm = Boolean(navigationState?.created);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "instant" });
    setActiveTab(0);
  }, [taskId]);

  const loadTask = useCallback(async () => {
    if (!taskId) {
      setError("Task ID is missing from the URL.");
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const taskData = await taskService.getTask(taskId);
      setTask(taskData);
    } catch (loadError) {
      setError(getErrorMessage(loadError, "Unable to load this task."));
      setTask(null);
    } finally {
      setLoading(false);
    }
  }, [taskId]);

  useEffect(() => {
    void loadTask();
  }, [loadTask]);

  useEffect(() => {
    if (!task) return;
    const terminal = task.status === "COMPLETED" || task.status === "FAILED" || task.status === "CANCELLED";
    if (terminal) return;
    const interval = window.setInterval(() => {
      void loadTask();
    }, 3000);
    return () => window.clearInterval(interval);
  }, [task, loadTask]);

  useEffect(() => {
    if (!task || !username) return;
    try {
      workspaceStore.updateTaskStatus(username, task.taskId, task.status);
      setWorkspaceId(
        workspaceStore.findWorkspaceIdForTask(username, task.taskId) ??
          navigationState?.workspaceId,
      );
      setWorkspaceWarning(null);
    } catch (storeError) {
      setWorkspaceWarning(
        getErrorMessage(storeError, "Workspace run status could not be saved in this browser."),
      );
    }
  }, [navigationState?.workspaceId, task, username]);

  const handleApprove = async () => {
    if (!taskId || task?.status !== "AWAITING_APPROVAL") return;
    setApproving(true);
    setApprovalError(null);
    try {
      setTask(await taskService.approveTask(taskId));
    } catch (approveError) {
      setApprovalError(
        getErrorMessage(approveError, "Unable to approve this proposal."),
      );
    } finally {
      setApproving(false);
    }
  };

  const handleCancel = async () => {
    if (!taskId) return;
    const terminal = task?.status === "COMPLETED" || task?.status === "FAILED" || task?.status === "CANCELLED";
    if (terminal) return;
    setCancelling(true);
    setCancelError(null);
    try {
      setTask(await taskService.cancelTask(taskId));
    } catch (err) {
      setCancelError(getErrorMessage(err, "Unable to cancel this task."));
    } finally {
      setCancelling(false);
    }
  };

  if (loading) {
    return (
      <Stack alignItems="center" spacing={2} sx={{ py: 10 }}>
        <CircularProgress />
        <Typography color="text.secondary">Loading task…</Typography>
      </Stack>
    );
  }

  if (error || !task) {
    return (
      <Stack spacing={2}>
        <Alert severity="error">{error ?? "Task data is unavailable."}</Alert>
        <Box>
          <Button onClick={() => void loadTask()} variant="outlined">
            Retry
          </Button>
        </Box>
      </Stack>
    );
  }

  const questionCount =
    (task.projectAnalysis?.openQuestions.length ?? 0) +
    (task.codingResult?.openQuestions.length ?? 0);
  const statusColor =
    task.status === "FAILED"
      ? "error"
      : task.status === "COMPLETED"
        ? "success"
        : task.status === "AWAITING_APPROVAL"
          ? "warning"
          : task.status === "CANCELLED"
            ? "warning"
            : "primary";

  return (
    <Stack spacing={2.5}>
      <Button
        component={Link}
        to={workspaceId ? `/workspaces/${encodeURIComponent(workspaceId)}` : "/"}
        startIcon={<ArrowBackRoundedIcon />}
        sx={{ alignSelf: "flex-start", color: "text.secondary", px: 0.5 }}
      >
        {workspaceId ? "Project workspace" : "All workspaces"}
      </Button>

      {(workspaceWarning || navigationState?.workspaceWarning) && (
        <Alert severity="warning">
          {workspaceWarning ?? navigationState?.workspaceWarning}
        </Alert>
      )}
      {createdFromForm && (
        <Alert severity="success" variant="outlined">
          Agent run created. Review the research and proposed file changes below; nothing is applied until you approve.
        </Alert>
      )}

      <Paper
        variant="outlined"
        sx={{
          p: { xs: 2.5, md: 3.5 },
          overflow: "hidden",
          background: (theme) =>
            `linear-gradient(118deg, ${alpha(theme.palette.primary.main, theme.palette.mode === "dark" ? 0.15 : 0.08)}, ${theme.palette.background.paper} 64%)`,
        }}
      >
        <Stack spacing={2.5}>
          <Stack
            direction={{ xs: "column", md: "row" }}
            justifyContent="space-between"
            alignItems={{ md: "flex-start" }}
            spacing={2}
          >
            <Box sx={{ minWidth: 0 }}>
              <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                <Chip
                  size="small"
                  label="TASK RUN"
                  sx={{ bgcolor: "action.selected", color: "text.secondary" }}
                />
                <Chip size="small" label={task.status.replaceAll("_", " ")} color={statusColor} />
              </Stack>
              <Typography
                variant="h3"
                sx={{ fontSize: { xs: "1.75rem", md: "2.25rem" }, overflowWrap: "anywhere" }}
              >
                {task.description}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                Run ID · {task.taskId}
              </Typography>
            </Box>

            {task.status === "AWAITING_APPROVAL" && (
              <Button
                variant="contained"
                size="large"
                startIcon={
                  approving ? (
                    <CircularProgress size={18} color="inherit" />
                  ) : (
                    <CheckCircleOutlineIcon />
                  )
                }
                onClick={() => {
                  setActiveTab(2);
                  void handleApprove();
                }}
                disabled={approving}
                sx={{ minWidth: 190, minHeight: 48, flexShrink: 0 }}
              >
                {approving ? "Applying proposal…" : "Approve proposal"}
              </Button>
            )}
            {task.status !== "COMPLETED" && task.status !== "FAILED" && task.status !== "CANCELLED" && (
              <Button
                variant="outlined"
                color="warning"
                onClick={() => void handleCancel()}
                disabled={cancelling}
                sx={{ minHeight: 48, flexShrink: 0 }}
              >
                {cancelling ? "Cancelling…" : "Cancel task"}
              </Button>
            )}
          </Stack>

          {task.message && (
            <Alert
              severity={
                task.status === "FAILED"
                  ? "error"
                  : task.status === "AWAITING_APPROVAL"
                    ? "warning"
                    : task.status === "COMPLETED"
                      ? "success"
                      : task.status === "CANCELLED"
                        ? "warning"
                        : "info"
              }
              variant="outlined"
              sx={{ alignItems: "center" }}
            >
              {task.message}
            </Alert>
          )}
          {approvalError && <Alert severity="error">{approvalError}</Alert>}
        </Stack>
      </Paper>

      <Paper
        variant="outlined"
        sx={{
          position: "sticky",
          top: { xs: 64, md: 76 },
          zIndex: 5,
          overflow: "hidden",
          bgcolor: "background.paper",
          backdropFilter: "blur(16px)",
        }}
      >
        <Tabs
          value={activeTab}
          onChange={(_, value: number) => setActiveTab(value)}
          variant="scrollable"
          scrollButtons="auto"
          aria-label="Task details sections"
          sx={{ px: 1 }}
        >
          <Tab icon={<AutoAwesomeRoundedIcon />} iconPosition="start" label="Overview" />
          <Tab icon={<MenuBookRoundedIcon />} iconPosition="start" label="Research" />
          <Tab
            icon={<CodeRoundedIcon />}
            iconPosition="start"
            label={`Proposal${task.codingResult ? ` · ${task.codingResult.proposedChanges.length}` : ""}`}
          />
          <Tab
            icon={<FactCheckRoundedIcon />}
            iconPosition="start"
            label="Testing"
          />
        </Tabs>
      </Paper>

      {activeTab === 0 && (
        <Stack spacing={2.5}>
          <Section title="Execution timeline">
            <ExecutionTimeline
              status={task.status}
              analysisAvailable={Boolean(task.projectAnalysis)}
              codingRequired={task.routingDecision?.agents?.includes("CODING") ?? Boolean(task.codingResult)}
              proposalAvailable={Boolean(task.codingResult)}
              approvalStarted={task.automaticRepairsAuthorized}
              testingAvailable={Boolean(task.testingResult)}
              testingRequired={task.routingDecision?.requiresTesting ?? Boolean(task.testingResult)}
              retryCount={task.retryCount}
            />
          </Section>
          <Box
            sx={{
              display: "grid",
              gridTemplateColumns: { xs: "repeat(2, 1fr)", md: "repeat(4, 1fr)" },
              gap: 1.5,
            }}
          >
            {[
              { label: "Proposed files", value: task.codingResult?.proposedChanges.length ?? 0 },
              { label: "Research risks", value: task.projectAnalysis?.risks.length ?? 0 },
              { label: "Open questions", value: questionCount },
              { label: "Repair attempts", value: task.retryCount },
            ].map((metric) => (
              <Paper key={metric.label} variant="outlined" sx={{ p: 2 }}>
                <Typography variant="h4">{metric.value}</Typography>
                <Typography variant="caption" color="text.secondary">
                  {metric.label}
                </Typography>
              </Paper>
            ))}
          </Box>
          <Box
            sx={{
              display: "grid",
              gridTemplateColumns: { xs: "1fr", md: "repeat(3, 1fr)" },
              gap: 1.5,
            }}
          >
            {[
              {
                title: "Research",
                description: task.projectAnalysis?.projectType ?? "Project analysis and requirements",
                icon: <MenuBookRoundedIcon />,
                done: Boolean(task.projectAnalysis),
              },
              {
                title: "Proposal",
                description: task.codingResult?.summary ?? "A proposed change set will appear here",
                icon: <CodeRoundedIcon />,
                done: Boolean(task.codingResult),
              },
              {
                title: "Verification",
                description: task.testingResult?.summary ?? "Testing starts after proposal approval",
                icon: <FactCheckRoundedIcon />,
                done: Boolean(task.testingResult),
              },
            ].map((item, index) => (
              <Paper
                key={item.title}
                variant="outlined"
                onClick={() => setActiveTab(index === 0 ? 1 : index === 1 ? 2 : 3)}
                sx={{
                  p: 2.25,
                  cursor: "pointer",
                  transition: "transform 180ms ease, border-color 180ms ease",
                  "&:hover": { transform: "translateY(-2px)", borderColor: "primary.main" },
                }}
              >
                <Stack direction="row" spacing={1.5} alignItems="flex-start">
                  <Box
                    sx={{
                      width: 38,
                      height: 38,
                      flexShrink: 0,
                      display: "grid",
                      placeItems: "center",
                      borderRadius: 2,
                      bgcolor: item.done ? "rgba(74,222,128,0.12)" : "action.selected",
                      color: item.done ? "success.main" : "primary.main",
                    }}
                  >
                    {item.icon}
                  </Box>
                  <Box sx={{ minWidth: 0 }}>
                    <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                      {item.title}
                    </Typography>
                    <Typography
                      variant="body2"
                      color="text.secondary"
                      sx={{
                        mt: 0.5,
                        display: "-webkit-box",
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: "vertical",
                        overflow: "hidden",
                      }}
                    >
                      {item.description}
                    </Typography>
                  </Box>
                </Stack>
              </Paper>
            ))}
          </Box>
        </Stack>
      )}

      {activeTab === 1 && (
        <Stack spacing={2.5}>
          <Section title="Research analysis">
            <ResearchAnalysis analysis={task.projectAnalysis} />
          </Section>
          <Box
            sx={{
              display: "grid",
              gridTemplateColumns: { xs: "1fr", md: "repeat(2, 1fr)" },
              gap: 2,
            }}
          >
            <Section title="Research risks">
              <ContentList items={task.projectAnalysis?.risks} />
            </Section>
            <Section title="Open questions">
              <ContentList items={task.projectAnalysis?.openQuestions} />
            </Section>
          </Box>
        </Stack>
      )}

      {activeTab === 2 && (
        <Stack spacing={2.5}>
          {task.status === "AWAITING_APPROVAL" && (
            <Alert
              severity="warning"
              variant="outlined"
              action={
                <Button
                  color="inherit"
                  size="small"
                  onClick={() => void handleApprove()}
                  disabled={approving}
                >
                  {approving ? "Applying…" : "Approve"}
                </Button>
              }
            >
              Review each proposed file. Approval applies these changes to the
              configured workspace.
            </Alert>
          )}
          <Section title="Coding proposal">
            {task.codingResult?.summary && (
              <Typography sx={{ mb: 2 }}>{task.codingResult.summary}</Typography>
            )}
            <ProposalWorkbench
              result={task.codingResult}
              approved={task.automaticRepairsAuthorized}
              applied={Boolean(task.testingResult) || task.status === "COMPLETED"}
            />
          </Section>
          <Box
            sx={{
              display: "grid",
              gridTemplateColumns: { xs: "1fr", md: "repeat(2, 1fr)" },
              gap: 2,
            }}
          >
            <Section title="Proposal risks">
              <ContentList items={task.codingResult?.risks} />
            </Section>
            <Section title="Proposal questions">
              <ContentList items={task.codingResult?.openQuestions} />
            </Section>
          </Box>
        </Stack>
      )}

      {activeTab === 3 && (
        <Stack spacing={2.5}>
          <Section title="Testing result">
            <TestingResult result={task.testingResult} />
          </Section>
          {task.retryCount > 0 && (
            <Alert severity="info" variant="outlined">
              The repair loop made {task.retryCount} automatic{" "}
              {task.retryCount === 1 ? "attempt" : "attempts"}.
            </Alert>
          )}
          {!task.testingResult && (
            <Alert severity="info" variant="outlined">
              Testing results will be available after the proposal is approved
              and applied.
            </Alert>
          )}
        </Stack>
      )}
    </Stack>
  );
}
