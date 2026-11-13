import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
  DashboardStats,
  FraudRing,
  LinkedParties,
  ModelReport,
  Neighborhood,
  Segment,
} from '../models/insight.models';
import { EquipmentView, PolicySummary } from '../models/policy.models';
import { Lead, LeadReceipt, LeadRequest, LeadStatus, QuoteRequest, QuoteResult } from '../models/quote.models';

@Injectable({ providedIn: 'root' })
export class PlatformApi {
  private readonly http = inject(HttpClient);

  policies() {
    return this.http.get<PolicySummary[]>('/api/policies');
  }

  equipment(policyId: number) {
    return this.http.get<EquipmentView[]>(`/api/policies/${policyId}/equipment`);
  }

  dashboard() {
    return this.http.get<DashboardStats>('/api/dashboard');
  }

  quote(request: QuoteRequest) {
    return this.http.post<QuoteResult>('/api/quotes', request);
  }

  captureLead(request: LeadRequest) {
    return this.http.post<LeadReceipt>('/api/quotes/leads', request);
  }

  leads() {
    return this.http.get<Lead[]>('/api/leads');
  }

  updateLead(id: number, status: LeadStatus) {
    return this.http.patch<Lead>(`/api/leads/${id}`, { status });
  }

  modelReport() {
    return this.http.get<ModelReport>('/api/ml/report');
  }

  retrain() {
    return this.http.post<ModelReport>('/api/ml/retrain', {});
  }

  segments() {
    return this.http.get<Segment[]>('/api/ml/segments');
  }

  linkedParties(claimId: number) {
    return this.http.get<LinkedParties>(`/api/claims/${claimId}/linked-parties`);
  }

  fraudRings() {
    return this.http.get<FraudRing[]>('/api/fraud/rings');
  }

  similarClaims(claimId: number) {
    return this.http.get<Neighborhood>(`/api/ml/claims/${claimId}/similar`);
  }
}
