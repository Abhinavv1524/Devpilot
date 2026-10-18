import { axiosClient } from "../api/axiosClient";
import { endpoints } from "../api/endpoints";
import type { TaskHistoryDto } from "../types/taskHistory";

export const taskHistoryService = {
  async getHistory(): Promise<TaskHistoryDto[]> {
    const response = await axiosClient.get<TaskHistoryDto[]>(
      endpoints.taskHistory.collection,
    );
    return response.data;
  },

  async getHistoryItem(taskId: string): Promise<TaskHistoryDto> {
    const response = await axiosClient.get<TaskHistoryDto>(
      endpoints.taskHistory.byId(taskId),
    );
    return response.data;
  },
};
