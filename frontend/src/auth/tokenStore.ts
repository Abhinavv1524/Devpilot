interface StoredSession {
  token: string;
  expiresAt: string;
  username: string;
}

const storageKey = "devpilot.session";
let accessToken: string | null = null;
let expiresAt: number | null = null;
let username: string | null = null;

function clearMemory(): void {
  accessToken = null;
  expiresAt = null;
  username = null;
}

function restoreSession(): void {
  if (typeof window === "undefined") return;

  const savedSession = window.sessionStorage.getItem(storageKey);
  if (!savedSession) return;

  try {
    const session = JSON.parse(savedSession) as Partial<StoredSession>;
    const parsedExpiry =
      typeof session.expiresAt === "string"
        ? Date.parse(session.expiresAt)
        : Number.NaN;
    if (
      typeof session.token !== "string" ||
      !session.token ||
      typeof session.username !== "string" ||
      !session.username.trim() ||
      !Number.isFinite(parsedExpiry) ||
      parsedExpiry <= Date.now()
    ) {
      window.sessionStorage.removeItem(storageKey);
      return;
    }

    accessToken = session.token;
    expiresAt = parsedExpiry;
    username = session.username;
  } catch (error) {
    if (!(error instanceof SyntaxError)) throw error;
    window.sessionStorage.removeItem(storageKey);
  }
}

restoreSession();

export function getAccessToken(): string | null {
  if (expiresAt !== null && expiresAt <= Date.now()) {
    clearAccessToken();
    return null;
  }
  return accessToken;
}

export function getAccessTokenExpiry(): number | null {
  getAccessToken();
  return expiresAt;
}

export function getAuthenticatedUsername(): string | null {
  getAccessToken();
  return username;
}

export function setAccessToken(token: string, expiry: string, user: string): void {
  const parsedExpiry = Date.parse(expiry);
  if (
    !token ||
    !user.trim() ||
    !Number.isFinite(parsedExpiry) ||
    parsedExpiry <= Date.now()
  ) {
    throw new Error("The authentication service returned an invalid or expired token.");
  }

  accessToken = token;
  expiresAt = parsedExpiry;
  username = user.trim();
  window.sessionStorage.setItem(
    storageKey,
    JSON.stringify({ token, expiresAt: expiry, username } satisfies StoredSession),
  );
}

export function clearAccessToken(): void {
  clearMemory();
  if (typeof window !== "undefined") {
    window.sessionStorage.removeItem(storageKey);
  }
}
