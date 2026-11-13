import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AgentProposal, AgentRun } from '../models/agent.models';
import { ClaimDetail } from '../models/claim.models';

@Injectable({ providedIn: 'root' })
export class AgentApi {
  private readonly http = inject(HttpClient);

  run(claimId: number, question: string) {
    return this.http.post<AgentRun>(`/api/claims/${claimId}/agent-runs`, { question });
  }

  runs(claimId: number) {
    return this.http.get<AgentRun[]>(`/api/claims/${claimId}/agent-runs`);
  }

  pending() {
    return this.http.get<AgentProposal[]>('/api/agent/proposals');
  }

  approve(proposalId: number, note: string) {
    return this.http.post<ClaimDetail>(`/api/agent/proposals/${proposalId}/approve`, { note });
  }

  reject(proposalId: number, note: string) {
    return this.http.post<void>(`/api/agent/proposals/${proposalId}/reject`, { note });
  }
}
