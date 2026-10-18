export interface SmokeTestCaseDto {
  inputLines: string[];
  expectedOutput: string;
}

export interface CreateTaskRequestDto {
  description: string;
  projectPath?: string;
  smokeTests?: SmokeTestCaseDto[];
}

export interface RoutingDecisionDto {
  intent: string | null;
  agents: string[] | null;
  reason: string | null;
  requiresProjectAccess: boolean;
  requiresTesting: boolean;
  requiresDocumentation: boolean;
}

export interface ProjectAnalysisDto {
  task: string | null;
  projectType: string | null;
  technologies: string[];
  requirements: string[];
  filesToModify: string[];
  newFiles: string[];
  dependencies: string[];
  recommendations: string[];
  assumptions: string[];
  openQuestions: string[];
  risks: string[];
  requiresProjectInspection: boolean;
}

export interface ProposedFileChangeDto {
  filePath: string | null;
  action: string | null;
  description: string | null;
  content: string | null;
}

export interface CodingResultDto {
  status: string | null;
  summary: string | null;
  proposedChanges: ProposedFileChangeDto[];
  risks: string[];
  openQuestions: string[];
}

export interface TestingResultDto {
  status: string | null;
  summary: string | null;
  testType: string | null;
  testCommand: string | null;
  exitCode: number | null;
  standardOutput: string | null;
  standardError: string | null;
  timedOut: boolean;
  errors: string[];
}

export type TaskStatus =
  | "RECEIVED"
  | "RESEARCHING"
  | "CODING"
  | "AWAITING_APPROVAL"
  | "TESTING"
  | "DOCUMENTING"
  | "COMPLETED"
  | "FAILED"
  | "CANCELLED";

export interface TaskStateDto {
  taskId: string;
  description: string;
  status: TaskStatus | string;
  createdAt: string;
  retryCount: number;
  message: string | null;
  automaticRepairsAuthorized: boolean;
  routingDecision: RoutingDecisionDto | null;
  projectAnalysis: ProjectAnalysisDto | null;
  codingResult: CodingResultDto | null;
  projectPath: string | null;
  smokeTests: SmokeTestCaseDto[];
  testingResult: TestingResultDto | null;
}
