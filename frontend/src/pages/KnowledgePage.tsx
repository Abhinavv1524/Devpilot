import { useState, type FormEvent } from "react";
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import { knowledgeService } from "../services/knowledgeService";
import type {
  KnowledgeAnswerResponseDto,
} from "../types/knowledge";
import { getErrorMessage } from "../utils/errorMessage";

export function KnowledgePage() {
  const [question, setQuestion] = useState("");
  const [response, setResponse] = useState<KnowledgeAnswerResponseDto | null>(
    null,
  );
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSearch = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setResponse(null);
    setSearching(true);
    try {
      setResponse(await knowledgeService.ask({ question: question.trim() }));
    } catch (searchError) {
      setError(getErrorMessage(searchError, "Knowledge question failed."));
    } finally {
      setSearching(false);
    }
  };

  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant="h4" gutterBottom>
          Knowledge base
        </Typography>
        <Typography color="text.secondary">
          Ask a question and get an answer grounded in the indexed reference
          documents.
        </Typography>
      </Box>

      <Paper component="form" onSubmit={handleSearch} variant="outlined" sx={{ p: 3 }}>
        <Stack spacing={2}>
          <Typography variant="h6">Ask DevPilot knowledge</Typography>
          <TextField
            label="Ask a question"
            value={question}
            onChange={(event) => setQuestion(event.target.value)}
            required
            fullWidth
          />
          <Box>
            <Button
              type="submit"
              variant="contained"
              startIcon={
                searching ? (
                  <CircularProgress size={18} color="inherit" />
                ) : (
                  <SearchIcon />
                )
              }
              disabled={searching || !question.trim()}
            >
              {searching ? "Searching…" : "Search"}
            </Button>
          </Box>
        </Stack>
      </Paper>

      {error && <Alert severity="error">{error}</Alert>}
      {response && (
        <Stack spacing={2}>
          <Paper variant="outlined" sx={{ p: 3 }}>
            <Stack spacing={2}>
              <Typography variant="h5">Answer</Typography>
              <Typography sx={{ whiteSpace: "pre-wrap" }}>
                {response.answer}
              </Typography>
              {response.sources.length > 0 && (
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Sources
                  </Typography>
                  <Stack spacing={0.5} sx={{ mt: 0.5 }}>
                    {response.sources.map((source, index) => (
                      <Typography
                        key={`${source.source ?? "source"}-${source.heading ?? "heading"}-${index}`}
                        variant="body2"
                        color="text.secondary"
                      >
                        {[source.source, source.heading, source.section]
                          .filter(Boolean)
                          .join(" · ")}
                      </Typography>
                    ))}
                  </Stack>
                </Box>
              )}
            </Stack>
          </Paper>
        </Stack>
      )}
    </Stack>
  );
}
