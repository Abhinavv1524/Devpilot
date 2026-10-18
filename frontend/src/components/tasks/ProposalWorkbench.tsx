import { useEffect, useState } from "react";
import {
  Box,
  Chip,
  Divider,
  Paper,
  Stack,
  Typography,
} from "@mui/material";
import ArticleRoundedIcon from "@mui/icons-material/ArticleRounded";
import FolderOpenRoundedIcon from "@mui/icons-material/FolderOpenRounded";
import type { CodingResultDto } from "../../types/task";

interface ProposalWorkbenchProps {
  result: CodingResultDto | null;
  applied: boolean;
  approved: boolean;
}

export function ProposalWorkbench({ result, applied, approved }: ProposalWorkbenchProps) {
  const changes = result?.proposedChanges ?? [];
  const [selectedIndex, setSelectedIndex] = useState(0);
  const selected = changes[selectedIndex];

  useEffect(() => {
    setSelectedIndex(0);
  }, [result]);

  if (!result || changes.length === 0) {
    return (
      <Paper variant="outlined" sx={{ p: 2.5 }}>
        <Typography color="text.secondary">
          {result?.summary ?? "No coding proposal is available for this task."}
        </Typography>
      </Paper>
    );
  }

  const source = selected.content ?? selected.description ?? "No file preview was returned.";
  const lines = source.split(/\r?\n/);
  const pathParts = (selected.filePath ?? "proposed-file").split(/[\\/]/);

  return (
    <Box
      sx={{
        display: "grid",
        gridTemplateColumns: { xs: "1fr", lg: "220px minmax(0, 1fr) 220px" },
        minHeight: { md: 520 },
        border: 1,
        borderColor: "divider",
        borderRadius: 2.5,
        overflow: "hidden",
        bgcolor: "background.paper",
      }}
    >
      <Box sx={{ borderRight: { lg: 1 }, borderColor: "divider", minWidth: 0 }}>
        <Stack direction="row" alignItems="center" spacing={1} sx={{ px: 1.5, py: 1.25 }}>
          <FolderOpenRoundedIcon fontSize="small" color="primary" />
          <Typography variant="overline">Explorer</Typography>
          <Chip size="small" label={changes.length} sx={{ ml: "auto !important" }} />
        </Stack>
        <Divider />
        <Stack spacing={0.35} sx={{ p: 0.75 }}>
          {changes.map((change, index) => {
            const selectedFile = selectedIndex === index;
            return (
              <Box
                key={`${change.filePath ?? "file"}-${index}`}
                component="button"
                type="button"
                onClick={() => setSelectedIndex(index)}
                aria-pressed={selectedFile}
                sx={{
                  display: "flex",
                  alignItems: "center",
                  gap: 1,
                  width: "100%",
                  px: 1,
                  py: 0.9,
                  border: 0,
                  borderRadius: 1.5,
                  color: selectedFile ? "text.primary" : "text.secondary",
                  bgcolor: selectedFile ? "action.selected" : "transparent",
                  textAlign: "left",
                  cursor: "pointer",
                  "&:hover": { bgcolor: "action.hover" },
                }}
              >
                <ArticleRoundedIcon sx={{ fontSize: 17, flexShrink: 0 }} />
                <Typography variant="caption" sx={{ minWidth: 0, overflowWrap: "anywhere" }}>
                  {change.filePath ?? "Unnamed file"}
                </Typography>
              </Box>
            );
          })}
        </Stack>
      </Box>

      <Box sx={{ minWidth: 0, display: "flex", flexDirection: "column" }}>
        <Stack
          direction="row"
          alignItems="center"
          spacing={0.75}
          sx={{ px: 1.5, py: 1.25, minHeight: 48, borderBottom: 1, borderColor: "divider" }}
        >
          <ArticleRoundedIcon fontSize="small" color="primary" />
          <Typography variant="caption" sx={{ minWidth: 0, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
            {pathParts.join(" › ")}
          </Typography>
          <Chip size="small" label={selected.action ?? "CHANGE"} color={selected.action === "CREATE" ? "secondary" : "primary"} variant="outlined" sx={{ ml: "auto !important", flexShrink: 0 }} />
        </Stack>
        <Box
          sx={{
            flex: 1,
            minHeight: 300,
            overflow: "auto",
            py: 1.5,
            bgcolor: (theme) => theme.palette.mode === "dark" ? "#0d1320" : "#f8f9fc",
            fontFamily: "ui-monospace, SFMono-Regular, Consolas, monospace",
            fontSize: 12.5,
            lineHeight: 1.7,
          }}
        >
          {lines.map((line, index) => (
            <Box key={index} sx={{ display: "grid", gridTemplateColumns: "48px minmax(0, 1fr)", px: 1.5 }}>
              <Typography component="span" sx={{ color: "text.disabled", textAlign: "right", pr: 2, userSelect: "none", font: "inherit" }}>
                {index + 1}
              </Typography>
              <Box component="span" sx={{ whiteSpace: "pre", minWidth: 0 }}>
                {line || " "}
              </Box>
            </Box>
          ))}
        </Box>
        <Stack direction="row" justifyContent="space-between" sx={{ px: 1.5, py: 0.75, borderTop: 1, borderColor: "divider" }}>
          <Typography variant="caption" color="text.secondary">{lines.length} lines · proposal preview</Typography>
          <Typography variant="caption" color="text.secondary">UTF-8</Typography>
        </Stack>
      </Box>

      <Box sx={{ borderLeft: { lg: 1 }, borderColor: "divider", p: 1.75, minWidth: 0 }}>
        <Typography variant="overline" color="text.secondary">Change details</Typography>
        <Stack spacing={1.5} sx={{ mt: 1.25 }}>
          <Box>
            <Typography variant="caption" color="text.secondary">File</Typography>
            <Typography variant="body2" sx={{ overflowWrap: "anywhere" }}>{selected.filePath ?? "Unnamed file"}</Typography>
          </Box>
          <Box>
            <Typography variant="caption" color="text.secondary">Operation</Typography>
            <Typography variant="body2">{selected.action ?? "Not specified"}</Typography>
          </Box>
          {selected.description && (
            <Box>
              <Typography variant="caption" color="text.secondary">Agent notes</Typography>
              <Typography variant="body2" sx={{ mt: 0.35 }}>{selected.description}</Typography>
            </Box>
          )}
          <Divider />
          <Chip
            size="small"
            color={applied ? "success" : approved ? "info" : "warning"}
            label={applied ? "Applied to project" : approved ? "Approval recorded" : "Preview only"}
            sx={{ alignSelf: "flex-start" }}
          />
          <Typography variant="caption" color="text.secondary">
            {applied
              ? "This shows the proposed content. The backend applied the approved change to the configured project path."
              : approved
                ? "Approval was recorded, but the task response does not confirm a successful application."
                : "No project files are changed until you approve this proposal."}
          </Typography>
        </Stack>
      </Box>
    </Box>
  );
}
