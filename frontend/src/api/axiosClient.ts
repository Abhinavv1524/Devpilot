import axios, { AxiosHeaders } from "axios";
import { clearAccessToken, getAccessToken } from "../auth/tokenStore";

export const unauthorizedEventName = "devpilot:unauthorized";

export const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "",
  headers: {
    Accept: "application/json",
    "Content-Type": "application/json",
  },
  timeout: 30_000,
});

axiosClient.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers = AxiosHeaders.from(config.headers);
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

axiosClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      clearAccessToken();
      if (typeof window !== "undefined") {
        window.dispatchEvent(new Event(unauthorizedEventName));
      }
    }
    return Promise.reject(error);
  },
);
