import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
  ClaimDetail,
  ClaimDocument,
  ClaimSort,
  ClaimStatus,
  ClaimSummary,
  DecisionRequest,
  FileClaimRequest,
  Page,
  TriageLane,
} from '../models/claim.models';

export interface ClaimFilters {
  status?: ClaimStatus | '';
  lane?: TriageLane | '';
  q?: string;
  openOnly?: boolean;
  sort?: ClaimSort;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class ClaimsApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/claims';

  search(filters: ClaimFilters) {
    let params = new HttpParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return this.http.get<Page<ClaimSummary>>(this.base, { params });
  }

  queue(lane?: TriageLane) {
    const params = lane ? new HttpParams().set('lane', lane) : undefined;
    return this.http.get<Page<ClaimSummary>>(`${this.base}/queue`, { params });
  }

  get(id: number) {
    return this.http.get<ClaimDetail>(`${this.base}/${id}`);
  }

  file(request: FileClaimRequest) {
    return this.http.post<ClaimDetail>(this.base, request);
  }

  decide(id: number, decision: DecisionRequest) {
    return this.http.post<ClaimDetail>(`${this.base}/${id}/decision`, decision);
  }

  provideInfo(id: number, note: string) {
    return this.http.post<ClaimDetail>(`${this.base}/${id}/info`, { note });
  }

  documents(id: number) {
    return this.http.get<ClaimDocument[]>(`${this.base}/${id}/documents`);
  }

  upload(id: number, file: File) {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ClaimDocument>(`${this.base}/${id}/documents`, form);
  }

  download(id: number, documentId: number) {
    return this.http.get(`${this.base}/${id}/documents/${documentId}`, { responseType: 'blob' });
  }
}
