import { useContext } from "react";
import {
  AppBar,
  Avatar,
  Box,
  Button,
  Chip,
  Container,
  IconButton,
  Stack,
  Toolbar,
  Tooltip,
  Typography,
} from "@mui/material";
import DashboardRoundedIcon from "@mui/icons-material/DashboardRounded";
import HistoryRoundedIcon from "@mui/icons-material/HistoryRounded";
import LightModeRoundedIcon from "@mui/icons-material/LightModeRounded";
import LogoutRoundedIcon from "@mui/icons-material/LogoutRounded";
import MemoryRoundedIcon from "@mui/icons-material/MemoryRounded";
import NightsStayRoundedIcon from "@mui/icons-material/NightsStayRounded";
import AddRoundedIcon from "@mui/icons-material/AddRounded";
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../../auth/AuthContext";
import { ThemeModeContext } from "../../app/AppProviders";

const navigationItems = [
  { label: "Workspaces", to: "/", icon: <DashboardRoundedIcon fontSize="small" /> },
  { label: "New change", to: "/tasks/new", icon: <AddRoundedIcon fontSize="small" /> },
  { label: "History", to: "/history", icon: <HistoryRoundedIcon fontSize="small" /> },
  { label: "Knowledge", to: "/knowledge", icon: <MemoryRoundedIcon fontSize="small" /> },
];

export function AppLayout() {
  const { logout } = useAuth();
  const { mode, toggleMode } = useContext(ThemeModeContext);

  return (
    <Box sx={{ minHeight: "100vh", color: "text.primary" }}>
      <AppBar
        position="sticky"
        color="transparent"
        elevation={0}
        sx={{
          top: 0,
          zIndex: (theme) => theme.zIndex.appBar,
          bgcolor: "background.default",
          borderBottom: 1,
          borderColor: "divider",
          backdropFilter: "blur(18px)",
          backgroundImage: "none",
        }}
      >
        <Toolbar
          disableGutters
          sx={{
            minHeight: { xs: 64, md: 76 },
            width: "100%",
            maxWidth: 1440,
            mx: "auto",
            px: { xs: 2, md: 4 },
            gap: { xs: 1.5, md: 3 },
          }}
        >
          <Stack
            component={NavLink}
            to="/"
            direction="row"
            alignItems="center"
            spacing={1.25}
            sx={{ color: "text.primary", textDecoration: "none", flexShrink: 0 }}
          >
            <Avatar
              variant="rounded"
              sx={{
                width: 38,
                height: 38,
                color: "common.white",
                background: "linear-gradient(140deg, #9b7cff 0%, #6248ee 55%, #24c9b2 140%)",
                boxShadow: "0 8px 24px rgba(111, 78, 238, 0.3)",
              }}
            >
              <Typography sx={{ fontWeight: 900, fontSize: 17 }}>D</Typography>
            </Avatar>
            <Box sx={{ display: { xs: "none", sm: "block" } }}>
              <Typography sx={{ fontWeight: 800, lineHeight: 1.05, letterSpacing: "-0.04em" }}>
                devpilot
              </Typography>
              <Typography
                variant="caption"
                color="text.secondary"
                sx={{ letterSpacing: "0.05em" }}
              >
                AI ENGINEERING
              </Typography>
            </Box>
          </Stack>

          <Box
            component="nav"
            aria-label="Main navigation"
            sx={{
              display: "flex",
              alignItems: "center",
              gap: 0.5,
              flex: 1,
              overflowX: "auto",
              scrollbarWidth: "none",
              "&::-webkit-scrollbar": { display: "none" },
            }}
          >
            {navigationItems.map((item) => (
              <Button
                key={item.to}
                component={NavLink}
                to={item.to}
                end={item.to === "/"}
                startIcon={item.icon}
                color="inherit"
                sx={{
                  minWidth: "max-content",
                  px: { xs: 1, md: 1.5 },
                  color: "text.secondary",
                  border: "1px solid transparent",
                  "&.active": {
                    color: "primary.main",
                    bgcolor: "action.selected",
                    borderColor: "divider",
                  },
                }}
              >
                {item.label}
              </Button>
            ))}
          </Box>

          <Stack direction="row" alignItems="center" spacing={{ xs: 0.25, md: 1 }}>
            <Chip
              size="small"
              label="AI workspace"
              sx={{
                display: { xs: "none", lg: "flex" },
                bgcolor: "action.selected",
                color: "text.secondary",
                border: 1,
                borderColor: "divider",
              }}
            />
            <Tooltip title={`Switch to ${mode === "dark" ? "light" : "dark"} mode`}>
              <IconButton onClick={toggleMode} aria-label="Toggle color theme" color="inherit">
                {mode === "dark" ? <LightModeRoundedIcon /> : <NightsStayRoundedIcon />}
              </IconButton>
            </Tooltip>
            <Tooltip title="Sign out">
              <IconButton onClick={logout} aria-label="Sign out" color="inherit">
                <LogoutRoundedIcon />
              </IconButton>
            </Tooltip>
          </Stack>
        </Toolbar>
      </AppBar>

      <Container
        component="main"
        maxWidth={false}
        sx={{
          maxWidth: 1320,
          px: { xs: 2, sm: 3, lg: 4 },
          py: { xs: 3, md: 5 },
          minHeight: "calc(100vh - 150px)",
        }}
      >
        <Outlet />
      </Container>

      <Box component="footer" sx={{ borderTop: 1, borderColor: "divider" }}>
        <Container
          maxWidth={false}
          sx={{
            maxWidth: 1320,
            px: { xs: 2, sm: 3, lg: 4 },
            py: 2,
            display: "flex",
            justifyContent: "space-between",
            gap: 2,
          }}
        >
          <Typography variant="caption" color="text.secondary">
            DevPilot · Human-guided AI engineering
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Plan thoughtfully. Review every change.
          </Typography>
        </Container>
      </Box>
    </Box>
  );
}
