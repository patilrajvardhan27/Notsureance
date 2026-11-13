export type AgentRunStatus = 'RUNNING' | 'COMPLETED' | 'STEP_LIMIT' | 'TIMED_OUT' | 'FAILED';
export type ProposedAction = 'FAST_TRACK' | 'REQUEST_INFO' | 'INVESTIGATE';

export interface AgentTrace {
  step: number;
  tool: string;
  input: string;
  output: string;
  failed: boolean;
  durationMs: number;
}

export interface AgentProposal {
  id: number;
  claimId: number;
  claimNumber: string;
  farmName: string;
  action: ProposedAction;
  reason: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  decisionNote: string | null;
  createdAt: string;
}

export interface AgentRun {
  id: number;
  status: AgentRunStatus;
  question: string;
  answer: string | null;
  supported: boolean;
  citations: string[];
  toolCalls: number;
  model: string;
  startedAt: string;
  trace: AgentTrace[];
  proposal: AgentProposal | null;
}
