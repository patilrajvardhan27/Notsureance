import { EquipmentCategory } from './policy.models';

export interface QuoteRequest {
  category: EquipmentCategory;
  equipmentValue: number;
  modelYear: number;
  stateCode: string;
  deductible: number;
  gpsTracked: boolean;
  safetyTraining: boolean;
  otherInsuredUnits: number;
  claimFreeYears: number;
  bundleFarmPolicy: boolean;
}

export interface QuoteResult {
  basePremium: number;
  annualPremium: number;
  monthlyPremium: number;
  adjustments: { label: string; amount: number }[];
  suggestions: { action: string; annualSavings: number }[];
}

export interface LeadRequest {
  quote: QuoteRequest;
  fullName: string;
  email: string;
  phone?: string;
  consentToContact: boolean;
}

export interface LeadReceipt {
  leadId: number;
  assignedAgent: string | null;
  quote: QuoteResult;
}

export type LeadStatus = 'NEW' | 'CONTACTED' | 'CONVERTED' | 'LOST';

export interface Lead {
  id: number;
  fullName: string;
  email: string;
  phone: string | null;
  stateCode: string;
  category: string;
  equipmentValue: number;
  annualPremium: number;
  status: LeadStatus;
  createdAt: string;
}
