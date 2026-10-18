export interface TaskHistoryDto {
  taskId: string;
  userPrompt: string;
  taskStatus: string;
  researchSummary: string | null;
  codingSummary: string | null;
  testingSummary: string | null;
  createdAt: string;
  completedAt: string;
}
