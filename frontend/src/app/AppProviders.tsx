import {
  createContext,
  useMemo,
  useState,
  type PropsWithChildren,
} from "react";
import { CssBaseline, ThemeProvider, type PaletteMode } from "@mui/material";
import { AuthProvider } from "../auth/AuthContext";
import { createAppTheme } from "../theme/theme";

interface ThemeModeContextValue {
  mode: PaletteMode;
  toggleMode: () => void;
}

export const ThemeModeContext = createContext<ThemeModeContextValue>({
  mode: "dark",
  toggleMode: () => undefined,
});

const themeStorageKey = "devpilot.color-mode";

export function AppProviders({ children }: PropsWithChildren) {
  const [mode, setMode] = useState<PaletteMode>(() => {
    const savedMode = window.localStorage.getItem(themeStorageKey);
    return savedMode === "light" ? "light" : "dark";
  });
  const theme = useMemo(() => createAppTheme(mode), [mode]);
  const themeModeValue = useMemo(
    () => ({
      mode,
      toggleMode: () => {
        setMode((currentMode) => {
          const nextMode = currentMode === "dark" ? "light" : "dark";
          window.localStorage.setItem(themeStorageKey, nextMode);
          return nextMode;
        });
      },
    }),
    [mode],
  );

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <ThemeModeContext.Provider value={themeModeValue}>
        <AuthProvider>{children}</AuthProvider>
      </ThemeModeContext.Provider>
    </ThemeProvider>
  );
}
