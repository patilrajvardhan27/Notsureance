import { ClaimStatus, LossCause, TriageLane } from './claim.models';

export interface ClassificationMetrics {
  accuracy: number;
  precision: number;
  recall: number;
  f1: number;
  rocAuc: number;
  truePositives: number;
  falsePositives: number;
  trueNegatives: number;
  falseNegatives: number;
}

export interface ModelReport {
  trainedAt: string;
  syntheticSamples: number;
  feedbackSamples: number;
  fraudHoldout: ClassificationMetrics;
  fraudCrossValidatedAuc: number;
  payoutHoldout: { meanAbsoluteError: number; rootMeanSquaredError: number; rSquared: number };
  causeTextAccuracy: number;
  fraudFeatureWeights: { feature: string; weight: number }[];
}

export interface Segment {
  name: 'Steady' | 'Watch' | 'Elevated';
  recommendedPlay: string;
  policyCount: number;
  averageClaims: number;
  averageFraudScore: number;
  gpsShare: number;
  policies: { id: number; policyNumber: string; farmName: string }[];
}

export interface SimilarClaim {
  id: number;
  claimNumber: string;
  cause: LossCause;
  status: ClaimStatus;
  estimatedLoss: number;
  approvedAmount: number | null;
  similarity: number;
}

export interface Neighborhood {
  neighbors: SimilarClaim[];
  approvalRate: number;
  medianPayoutRatio: number;
}

export interface DashboardStats {
  claimsByStatus: Record<ClaimStatus, number>;
  openByLane: Record<TriageLane, number>;
  openClaims: number;
  totalClaims: number;
  approvedAmount: number;
  fastTrackShare: number;
  newLeads: number;
}
