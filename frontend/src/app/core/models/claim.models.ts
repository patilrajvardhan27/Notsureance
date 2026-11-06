import { EquipmentView, PolicySummary } from './policy.models';

export type ClaimStatus = 'SUBMITTED' | 'UNDER_REVIEW' | 'NEEDS_INFO' | 'APPROVED' | 'DENIED' | 'PAID';
export type TriageLane = 'FAST_TRACK' | 'STANDARD' | 'SPECIAL_INVESTIGATION';
export type LossCause =
  | 'COLLISION'
  | 'ROLLOVER'
  | 'FIRE'
  | 'THEFT'
  | 'HAIL'
  | 'WIND'
  | 'FLOOD'
  | 'LIGHTNING'
  | 'VANDALISM'
  | 'MECHANICAL_BREAKDOWN';
export type DecisionAction = 'START_REVIEW' | 'APPROVE' | 'DENY' | 'REQUEST_INFO' | 'MARK_PAID';
export type ClaimSort = 'NEWEST' | 'PRIORITY' | 'FRAUD_RISK' | 'LARGEST_LOSS';

export const LOSS_CAUSES: LossCause[] = [
  'COLLISION', 'ROLLOVER', 'FIRE', 'THEFT', 'HAIL', 'WIND', 'FLOOD', 'LIGHTNING', 'VANDALISM', 'MECHANICAL_BREAKDOWN',
];
export const CLAIM_STATUSES: ClaimStatus[] = ['SUBMITTED', 'UNDER_REVIEW', 'NEEDS_INFO', 'APPROVED', 'DENIED', 'PAID'];

export interface ClaimSummary {
  id: number;
  claimNumber: string;
  farmName: string;
  equipmentName: string;
  cause: LossCause;
  status: ClaimStatus;
  lane: TriageLane | null;
  fraudScore: number | null;
  estimatedLoss: number;
  predictedPayout: number | null;
  priority: number;
  harvestCritical: boolean;
  incidentDate: string;
  filedAt: string;
}

export interface RiskFactor {
  label: string;
  impact: number;
}

export interface ClaimAssessment {
  fraudProbability: number;
  predictedPayout: number;
  anomalyScore: number;
  predictedCause: LossCause;
  causeConfidence: number;
  lane: TriageLane;
  priority: number;
  topFactors: RiskFactor[];
}

export interface TimelineEntry {
  type: 'FILED' | 'SCORED' | 'ASSIGNED' | 'STATUS_CHANGED' | 'INFO_PROVIDED' | 'DOCUMENT_ADDED';
  fromStatus: ClaimStatus | null;
  toStatus: ClaimStatus | null;
  note: string | null;
  actorName: string;
  at: string;
}

export interface ClaimDetail {
  summary: ClaimSummary;
  policy: PolicySummary;
  equipment: EquipmentView;
  description: string;
  reportedAt: string;
  approvedAmount: number | null;
  latitude: number | null;
  longitude: number | null;
  filedBy: string;
  assignedAdjuster: string | null;
  decisionReason: string | null;
  assessment: ClaimAssessment;
  timeline: TimelineEntry[];
  nextStatuses: ClaimStatus[];
}

export interface FileClaimRequest {
  policyId: number;
  equipmentId: number;
  cause: LossCause;
  incidentDate: string;
  description: string;
  estimatedLoss: number;
  latitude?: number | null;
  longitude?: number | null;
  harvestCritical: boolean;
}

export interface DecisionRequest {
  action: DecisionAction;
  amount?: number | null;
  note?: string | null;
  suspectedFraud?: boolean;
}

export interface ClaimDocument {
  id: number;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  uploadedBy: string;
  uploadedAt: string;
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}
