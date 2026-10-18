import {
  Alert,
  Box,
  Chip,
  Stack,
  Typography,
} from "@mui/material";
import CheckCircleRoundedIcon from "@mui/icons-material/CheckCircleRounded";
import CircleOutlinedIcon from "@mui/icons-material/CircleOutlined";
import ErrorRoundedIcon from "@mui/icons-material/ErrorRounded";
import PauseCircleOutlineRoundedIcon from "@mui/icons-material/PauseCircleOutlineRounded";
import SkipNextRoundedIcon from "@mui/icons-material/SkipNextRounded";

type StageState = "complete" | "current" | "pending" | "failed" | "skipped";

interface WorkflowStage {
  name: string;
  detail: string;
  state: StageState;
}

interface ExecutionTimelineProps {
  status: string;
  analysisAvailable?: boolean;
  codingRequired?: boolean;
  proposalAvailable?: boolean;
  approvalStarted?: boolean;
  testingAvailable?: boolean;
  testingRequired?: boolean;
  retryCount?: number;
}

function stageStateLabel(state: StageState): string {
  return {
    complete: "Done",
    current: "Current",
    pending: "Queued",
    failed: "Failed",
    skipped: "Skipped",
  }[state];
}

export function ExecutionTimeline({
  status,
  analysisAvailable,
  codingRequired,
  proposalAvailable,
  approvalStarted,
  testingAvailable,
  testingRequired,
  retryCount = 0,
}: ExecutionTimelineProps) {
  if (status === "CANCELLED") {
    return (
      <Alert severity="warning" variant="outlined">
        The task was cancelled. No further processing was performed.
      </Alert>
    );
  }

  if (
    analysisAvailable === undefined ||
    codingRequired === undefined ||
    proposalAvailable === undefined ||
    testingRequired === undefined ||
    testingAvailable === undefined
  ) {
    return (
      <Alert severity="info" variant="outlined">
        The history API records a final status and summaries, not per-agent events. Recorded status: {status.replaceAll("_", " ")}.
      </Alert>
    );
  }

  const failed = status === "FAILED" || status === "CANCELLED";
  const researchState: StageState =
    analysisAvailable ? "complete" : status === "RESEARCHING" ? "current" : failed ? "failed" : "pending";
  const codingState: StageState = !codingRequired
    ? "skipped"
    : proposalAvailable
      ? "complete"
      : status === "CODING"
        ? "current"
        : failed && analysisAvailable
          ? "failed"
          : failed
            ? "skipped"
          : "pending";
  const approvalState: StageState = !codingRequired
    ? "skipped"
    : status === "AWAITING_APPROVAL"
      ? "current"
      : failed && !proposalAvailable
        ? "skipped"
        : failed && proposalAvailable && !approvalStarted
          ? "failed"
          : failed && approvalStarted && !testingAvailable
            ? "failed"
      : proposalAvailable && (approvalStarted || testingAvailable || status === "COMPLETED")
        ? "complete"
        : "pending";
  const testingState: StageState = !testingRequired
    ? "skipped"
    : testingAvailable
      ? failed
        ? "failed"
        : "complete"
      : status === "TESTING"
        ? "current"
        : status === "COMPLETED"
          ? "skipped"
        : failed
          ? "skipped"
          : "pending";
  const stages: WorkflowStage[] = [
    {
      name: "Research",
      detail: analysisAvailable
        ? "Project analysis is available."
        : status === "RESEARCHING"
          ? "The Research Agent is inspecting the project."
          : "Project analysis has not been returned.",
      state: researchState,
    },
    {
      name: "Coding proposal",
      detail: !codingRequired
        ? "The router did not select a coding step for this task."
        : proposalAvailable
          ? "A proposal and its file changes are available to review."
          : status === "CODING"
            ? "The Coding Agent is preparing a proposal."
            : failed && analysisAvailable
              ? "The coding step did not return an approvable proposal."
              : "Coding was not run after an earlier failure.",
      state: codingState,
    },
    {
      name: "Approval and apply",
      detail: !codingRequired
        ? "No proposed files need approval."
        : status === "AWAITING_APPROVAL"
          ? "Waiting for your approval before applying any proposed changes."
        : failed && proposalAvailable
          ? "The approval or file-application stage failed; see the task failure message."
        : approvalState === "complete"
            ? "The proposal was approved and application began."
            : "Approval is required before proposed files are applied.",
      state: approvalState,
    },
    {
      name: "Testing",
      detail: !testingRequired
        ? "The router marked testing as not required for this task."
        : testingAvailable
          ? "A Testing Agent result is available."
          : status === "TESTING"
            ? "The Testing Agent is checking the applied changes."
            : status === "COMPLETED"
              ? "The workflow completed without a Testing Agent result."
            : failed
              ? "No Testing Agent result was recorded before the workflow failed."
            : "Testing runs after approval and application.",
      state: testingState,
    },
  ];

  if (retryCount > 0) {
    stages.push({
      name: "Repair loop",
      detail: `${retryCount} automatic ${retryCount === 1 ? "repair attempt" : "repair attempts"} recorded.`,
      state: failed ? "failed" : "complete",
    });
  }
  if (status === "COMPLETED" || status === "FAILED" || status === "CANCELLED") {
    stages.push({
      name: status === "COMPLETED" ? "Workflow completed" : status === "FAILED" ? "Workflow failed" : "Workflow cancelled",
      detail: status === "COMPLETED" ? "The backend marked this task complete." : status === "FAILED" ? "The backend marked this task failed." : "The backend marked this task cancelled.",
      state: status === "COMPLETED" ? "complete" : "failed",
    });
  }

  return (
    <Stack spacing={0}>
      {stages.map((stage, index) => {
        const color =
          stage.state === "complete"
            ? "success.main"
            : stage.state === "current"
              ? "primary.main"
              : stage.state === "failed"
                ? "error.main"
                : "text.disabled";
        const StageIcon =
          stage.state === "complete"
            ? CheckCircleRoundedIcon
            : stage.state === "failed"
              ? ErrorRoundedIcon
              : stage.state === "skipped"
                ? SkipNextRoundedIcon
                : stage.state === "current"
                  ? PauseCircleOutlineRoundedIcon
                  : CircleOutlinedIcon;
        return (
          <Stack key={`${stage.name}-${index}`} direction="row" spacing={1.5} sx={{ minHeight: 66 }}>
            <Stack alignItems="center" sx={{ width: 24, flexShrink: 0 }}>
              <StageIcon sx={{ fontSize: 19, color }} />
              {index < stages.length - 1 && <Box sx={{ width: 1, flex: 1, minHeight: 28, bgcolor: "divider" }} />}
            </Stack>
            <Box sx={{ pb: 2, minWidth: 0, flex: 1 }}>
              <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                <Typography variant="subtitle2">{stage.name}</Typography>
                <Chip size="small" label={stageStateLabel(stage.state)} color={stage.state === "failed" ? "error" : stage.state === "complete" ? "success" : stage.state === "current" ? "primary" : "default"} variant={stage.state === "pending" ? "outlined" : "filled"} />
              </Stack>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.35 }}>
                {stage.detail}
              </Typography>
            </Box>
          </Stack>
        );
      })}
    </Stack>
  );
}
