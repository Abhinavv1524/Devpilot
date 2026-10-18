import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from "react";
import { useNavigate } from "react-router-dom";
import { unauthorizedEventName } from "../api/axiosClient";
import { authService } from "../services/authService";
import type { LoginRequestDto, TokenResponseDto } from "../types/auth";
import {
  clearAccessToken,
  getAccessToken,
  getAccessTokenExpiry,
  getAuthenticatedUsername,
  setAccessToken,
} from "./tokenStore";

interface AuthContextValue {
  isAuthenticated: boolean;
  username: string | null;
  login: (credentials: LoginRequestDto) => Promise<TokenResponseDto>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: PropsWithChildren) {
  const navigate = useNavigate();
  const [isAuthenticated, setIsAuthenticated] = useState(
    () => getAccessToken() !== null,
  );
  const [username, setUsername] = useState(getAuthenticatedUsername);

  const logout = useCallback(() => {
    clearAccessToken();
    setIsAuthenticated(false);
    setUsername(null);
    navigate("/login", { replace: true });
  }, [navigate]);

  const login = useCallback(
    async (credentials: LoginRequestDto) => {
      const tokenResponse = await authService.login(credentials);
      setAccessToken(tokenResponse.token, tokenResponse.expiresAt, credentials.username);
      setIsAuthenticated(true);
      setUsername(credentials.username.trim());
      return tokenResponse;
    },
    [],
  );

  useEffect(() => {
    const handleUnauthorized = () => {
      clearAccessToken();
      setIsAuthenticated(false);
      setUsername(null);
      navigate("/login", { replace: true });
    };

    window.addEventListener(unauthorizedEventName, handleUnauthorized);
    return () =>
      window.removeEventListener(unauthorizedEventName, handleUnauthorized);
  }, [navigate]);

  useEffect(() => {
    if (!isAuthenticated) return;
    const expiry = getAccessTokenExpiry();
    if (expiry === null) {
      setIsAuthenticated(false);
      setUsername(null);
      navigate("/login", { replace: true });
      return;
    }

    const timeout = window.setTimeout(() => {
      clearAccessToken();
      setIsAuthenticated(false);
      navigate("/login", { replace: true });
    }, Math.max(0, expiry - Date.now()));

    return () => window.clearTimeout(timeout);
  }, [isAuthenticated, navigate]);

  const value = useMemo(
    () => ({ isAuthenticated, username, login, logout }),
    [isAuthenticated, username, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside an AuthProvider.");
  }
  return context;
}
