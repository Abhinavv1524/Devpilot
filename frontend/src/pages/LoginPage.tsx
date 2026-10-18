import { useState, type FormEvent } from "react";
import {
  Alert,
  Avatar,
  Box,
  Button,
  CircularProgress,
  Divider,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import AutoAwesomeRoundedIcon from "@mui/icons-material/AutoAwesomeRounded";
import CodeRoundedIcon from "@mui/icons-material/CodeRounded";
import FactCheckRoundedIcon from "@mui/icons-material/FactCheckRounded";
import MenuBookRoundedIcon from "@mui/icons-material/MenuBookRounded";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { authService } from "../services/authService";
import { getErrorMessage } from "../utils/errorMessage";

export function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [mode, setMode] = useState<"login" | "signup">("login");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const returnTo = (
    location.state as { from?: { pathname?: string } } | null
  )?.from?.pathname;

  if (isAuthenticated) {
    return <Navigate to={returnTo ?? "/"} replace />;
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    setIsSubmitting(true);
    try {
      if (mode === "signup") {
        if (password !== confirmPassword) {
          setError("Passwords do not match.");
          return;
        }
        await authService.register({
          username: username.trim(),
          email: email.trim(),
          password,
        });
        setMode("login");
        setConfirmPassword("");
        setSuccess("Account created. Sign in with your new credentials.");
        return;
      }

      await login({ username: username.trim(), password });
      navigate(returnTo ?? "/", { replace: true });
    } catch (submitError) {
      setError(
        getErrorMessage(
          submitError,
          mode === "signup"
            ? "Unable to create your account. Please try again."
            : "Unable to sign in. Please try again.",
        ),
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const switchMode = () => {
    setMode((currentMode) => (currentMode === "login" ? "signup" : "login"));
    setError(null);
    setSuccess(null);
  };

  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "grid",
        placeItems: "center",
        bgcolor: "background.default",
        p: { xs: 2, md: 4 },
      }}
    >
      <Box
        sx={{
          width: "100%",
          maxWidth: 1040,
          display: "grid",
          gridTemplateColumns: { xs: "1fr", md: "1fr 420px" },
          overflow: "hidden",
          borderRadius: 4,
          border: 1,
          borderColor: "divider",
          boxShadow: "0 30px 100px rgba(0,0,0,0.28)",
          bgcolor: "background.paper",
        }}
      >
        <Box
          sx={{
            p: { xs: 3, sm: 5 },
            minHeight: { md: 570 },
            display: "flex",
            flexDirection: "column",
            justifyContent: "space-between",
            background:
              "radial-gradient(ellipse at 80% 10%, rgba(94,234,212,0.17), transparent 35%), linear-gradient(145deg, rgba(109,74,255,0.24), rgba(18,26,43,0.96) 54%, rgba(11,16,32,0.98))",
          }}
        >
          <Box>
            <Stack direction="row" spacing={1.25} alignItems="center">
              <Avatar
                variant="rounded"
                sx={{
                  color: "white",
                  width: 42,
                  height: 42,
                  background: "linear-gradient(140deg, #9b7cff, #6248ee 58%, #24c9b2)",
                }}
              >
                <Typography sx={{ fontWeight: 900 }}>D</Typography>
              </Avatar>
              <Box>
                <Typography sx={{ color: "white", fontWeight: 800, letterSpacing: "-0.04em" }}>
                  devpilot
                </Typography>
                <Typography variant="caption" sx={{ color: "rgba(255,255,255,0.55)" }}>
                  AI ENGINEERING WORKSPACE
                </Typography>
              </Box>
            </Stack>
            <Typography
              variant="h2"
              sx={{
                mt: { xs: 5, md: 9 },
                color: "white",
                fontSize: { xs: "2.4rem", md: "3.25rem" },
                maxWidth: 520,
              }}
            >
              Good ideas deserve a{" "}
              <Box component="span" sx={{ color: "#5eead4" }}>better build process.</Box>
            </Typography>
            <Typography sx={{ mt: 2, maxWidth: 480, color: "rgba(231,235,248,0.68)" }}>
              Research, propose, review, and test changes with a coordinated
              team of AI agents. You stay in control at every step.
            </Typography>
          </Box>

          <Stack
            direction="row"
            spacing={1}
            sx={{ mt: 5, flexWrap: "wrap", rowGap: 1 }}
          >
            {[
              { title: "Research", icon: <MenuBookRoundedIcon /> },
              { title: "Coding", icon: <CodeRoundedIcon /> },
              { title: "Testing", icon: <FactCheckRoundedIcon /> },
            ].map((item) => (
              <Stack
                key={item.title}
                direction="row"
                spacing={0.75}
                alignItems="center"
                sx={{
                  px: 1.25,
                  py: 0.75,
                  color: "rgba(255,255,255,0.76)",
                  border: "1px solid rgba(255,255,255,0.12)",
                  borderRadius: 99,
                  bgcolor: "rgba(255,255,255,0.035)",
                }}
              >
                {item.icon}
                <Typography variant="caption">{item.title}</Typography>
              </Stack>
            ))}
          </Stack>
        </Box>

        <Stack
          component="form"
          onSubmit={handleSubmit}
          justifyContent="center"
          spacing={2.5}
          sx={{ p: { xs: 3, sm: 4 }, bgcolor: "background.paper" }}
        >
          <Box>
            <AutoAwesomeRoundedIcon color="primary" />
            <Typography variant="h4" sx={{ mt: 1 }}>
              {mode === "login" ? "Welcome back" : "Create your account"}
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 0.75 }}>
              {mode === "login"
                ? "Sign in to pick up where your team left off."
                : "Create an account to get started with DevPilot."}
            </Typography>
          </Box>
          <Divider />
          <Stack spacing={2}>
            {error && <Alert severity="error">{error}</Alert>}
            {success && <Alert severity="success">{success}</Alert>}
            <TextField
              label="Username"
              name="username"
              autoComplete="username"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              required
              inputProps={mode === "signup" ? { minLength: 3, maxLength: 50 } : undefined}
              fullWidth
            />
            {mode === "signup" && (
              <TextField
                label="Email"
                name="email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                required
                inputProps={{ maxLength: 255 }}
                fullWidth
              />
            )}
            <TextField
              label="Password"
              name="password"
              type="password"
              autoComplete={mode === "login" ? "current-password" : "new-password"}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
              inputProps={mode === "signup" ? { minLength: 8, maxLength: 100 } : undefined}
              fullWidth
            />
            {mode === "signup" && (
              <TextField
                label="Confirm password"
                name="confirmPassword"
                type="password"
                autoComplete="new-password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                required
                fullWidth
              />
            )}
            <Button
              type="submit"
              variant="contained"
              size="large"
              disabled={
                isSubmitting ||
                !username.trim() ||
                !password ||
                (mode === "signup" &&
                  (!email.trim() || !confirmPassword || password !== confirmPassword))
              }
              sx={{ minHeight: 48, mt: 1 }}
            >
              {isSubmitting ? (
                <CircularProgress size={24} color="inherit" />
              ) : (
                mode === "login" ? "Sign in to DevPilot" : "Create account"
              )}
            </Button>
            <Button
              type="button"
              variant="text"
              onClick={switchMode}
              disabled={isSubmitting}
            >
              {mode === "login"
                ? "New to DevPilot? Create an account"
                : "Already have an account? Sign in"}
            </Button>
          </Stack>
          <Typography variant="caption" color="text.secondary">
            Your workspace session stays active in this browser tab until the
            session expires or you sign out.
          </Typography>
        </Stack>
      </Box>
    </Box>
  );
}
