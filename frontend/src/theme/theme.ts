import { createTheme, type PaletteMode } from "@mui/material/styles";

export function createAppTheme(mode: PaletteMode) {
  const dark = mode === "dark";

  return createTheme({
    palette: {
      mode,
      primary: {
        main: dark ? "#a78bfa" : "#6d4aff",
        light: dark ? "#c4b5fd" : "#9378ff",
        dark: dark ? "#8b5cf6" : "#5135db",
        contrastText: dark ? "#171225" : "#ffffff",
      },
      secondary: {
        main: dark ? "#5eead4" : "#0f9e91",
      },
      success: { main: dark ? "#4ade80" : "#16834d" },
      warning: { main: dark ? "#fbbf24" : "#bd7800" },
      error: { main: dark ? "#fb7185" : "#cf3551" },
      background: dark
        ? { default: "#0b1020", paper: "#121a2b" }
        : { default: "#f5f6fb", paper: "#ffffff" },
      text: dark
        ? { primary: "#f1f3fa", secondary: "#9aa6bd" }
        : { primary: "#171b2c", secondary: "#667085" },
      divider: dark ? "rgba(171, 185, 216, 0.13)" : "rgba(35, 42, 72, 0.11)",
    },
    shape: { borderRadius: 14 },
    typography: {
      fontFamily: [
        "Inter",
        "ui-sans-serif",
        "system-ui",
        "-apple-system",
        "BlinkMacSystemFont",
        '"Segoe UI"',
        "sans-serif",
      ].join(","),
      h1: { fontWeight: 750, letterSpacing: "-0.055em" },
      h2: { fontWeight: 740, letterSpacing: "-0.045em" },
      h3: { fontWeight: 720, letterSpacing: "-0.04em" },
      h4: { fontWeight: 720, letterSpacing: "-0.035em" },
      h5: { fontWeight: 700, letterSpacing: "-0.025em" },
      h6: { fontWeight: 650, letterSpacing: "-0.015em" },
      button: { textTransform: "none", fontWeight: 650, letterSpacing: "0" },
      overline: { fontWeight: 700, letterSpacing: "0.11em" },
    },
    components: {
      MuiCssBaseline: {
        styleOverrides: {
          html: { scrollBehavior: "smooth" },
          body: {
            minHeight: "100vh",
            backgroundImage: dark
              ? "radial-gradient(ellipse at 14% 0%, rgba(109, 74, 255, 0.12), transparent 38%), radial-gradient(ellipse at 90% 8%, rgba(24, 190, 177, 0.055), transparent 28%)"
              : "radial-gradient(ellipse at 14% 0%, rgba(109, 74, 255, 0.075), transparent 36%)",
            backgroundAttachment: "fixed",
          },
          "::selection": {
            backgroundColor: dark
              ? "rgba(167, 139, 250, 0.35)"
              : "rgba(109, 74, 255, 0.2)",
          },
        },
      },
      MuiPaper: {
        styleOverrides: {
          root: {
            backgroundImage: "none",
            borderColor: dark
              ? "rgba(171, 185, 216, 0.13)"
              : "rgba(35, 42, 72, 0.11)",
          },
        },
      },
      MuiCard: {
        styleOverrides: {
          root: {
            backgroundImage: "none",
            borderColor: dark
              ? "rgba(171, 185, 216, 0.13)"
              : "rgba(35, 42, 72, 0.11)",
          },
        },
      },
      MuiButton: {
        styleOverrides: {
          root: { borderRadius: 10, minHeight: 40 },
          contained: { boxShadow: "none" },
        },
      },
      MuiOutlinedInput: {
        styleOverrides: {
          root: {
            borderRadius: 11,
            backgroundColor: dark
              ? "rgba(7, 12, 25, 0.45)"
              : "rgba(255, 255, 255, 0.75)",
          },
        },
      },
      MuiTabs: {
        styleOverrides: {
          indicator: { height: 3, borderRadius: 99 },
        },
      },
      MuiTab: {
        styleOverrides: {
          root: { minHeight: 48, textTransform: "none", fontWeight: 650 },
        },
      },
    },
  });
}
