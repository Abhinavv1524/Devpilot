export interface KnowledgeQuestionRequestDto {
  question: string;
}

export interface KnowledgeSourceDto {
  source: string | null;
  heading: string | null;
  section: string | null;
}

export interface KnowledgeAnswerResponseDto {
  answer: string;
  sources: KnowledgeSourceDto[];
}
