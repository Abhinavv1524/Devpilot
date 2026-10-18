import axios from "axios";

export function getErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    if (error.code === "ECONNABORTED" || error.code === "ETIMEDOUT") {
      return "The request took too long. Task analysis can take several minutes; check the task again before retrying.";
    }
    const responseMessage = error.response?.data;
    if (typeof responseMessage === "string" && responseMessage.trim()) {
      return responseMessage;
    }
    if (
      responseMessage &&
      typeof responseMessage === "object" &&
      "message" in responseMessage &&
      typeof responseMessage.message === "string"
    ) {
      return responseMessage.message;
    }
    if (error.response?.status === 401) {
      return "Your session is invalid or has expired. Please sign in again.";
    }
    if (error.response?.status === 404) {
      return "The requested resource was not found.";
    }
    if (error.response?.status === 409) {
      return "The task cannot be modified in its current state.";
    }
    if (!error.response) {
      return "Could not reach the DevPilot Gateway. Check that the Gateway is running and that this app is using the configured API URL.";
    }
  }
  return fallback;
}
