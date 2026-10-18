import { axiosClient } from "../api/axiosClient";
import { endpoints } from "../api/endpoints";
import type { CreateTaskRequestDto, TaskStateDto } from "../types/task";

export const taskService = {
  async createTask(request: CreateTaskRequestDto): Promise<TaskStateDto> {
    const response = await axiosClient.post<TaskStateDto>(
      endpoints.tasks.collection,
      request,
    );
    return response.data;
  },

  async getTask(taskId: string): Promise<TaskStateDto> {
    const response = await axiosClient.get<TaskStateDto>(
      endpoints.tasks.byId(taskId),
    );
    return response.data;
  },

  async approveTask(taskId: string): Promise<TaskStateDto> {
    const response = await axiosClient.post<TaskStateDto>(
      endpoints.tasks.approve(taskId),
    );
    return response.data;
  },

  async cancelTask(taskId: string): Promise<TaskStateDto> {
    const response = await axiosClient.post<TaskStateDto>(
      endpoints.tasks.cancel(taskId),
    );
    return response.data;
  },
};
