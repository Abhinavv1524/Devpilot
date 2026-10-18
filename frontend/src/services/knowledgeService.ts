import { axiosClient } from "../api/axiosClient";
import { endpoints } from "../api/endpoints";
import type {
  KnowledgeAnswerResponseDto,
  KnowledgeQuestionRequestDto,
} from "../types/knowledge";

export const knowledgeService = {
  async ask(
    request: KnowledgeQuestionRequestDto,
  ): Promise<KnowledgeAnswerResponseDto> {
    const response = await axiosClient.post<KnowledgeAnswerResponseDto>(
      endpoints.knowledge.ask,
      request,
    );
    return response.data;
  },
};
