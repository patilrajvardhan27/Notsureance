export type EquipmentCategory =
  | 'TRACTOR'
  | 'COMBINE'
  | 'SPRAYER'
  | 'PLANTER'
  | 'BALER'
  | 'IRRIGATION_PIVOT'
  | 'GRAIN_DRYER'
  | 'UTILITY_VEHICLE';

export const EQUIPMENT_CATEGORIES: { value: EquipmentCategory; label: string }[] = [
  { value: 'TRACTOR', label: 'Tractor' },
  { value: 'COMBINE', label: 'Combine harvester' },
  { value: 'SPRAYER', label: 'Self-propelled sprayer' },
  { value: 'PLANTER', label: 'Planter' },
  { value: 'BALER', label: 'Baler' },
  { value: 'IRRIGATION_PIVOT', label: 'Center pivot irrigation' },
  { value: 'GRAIN_DRYER', label: 'Grain dryer' },
  { value: 'UTILITY_VEHICLE', label: 'Utility vehicle' },
];

export interface PolicySummary {
  id: number;
  policyNumber: string;
  farmName: string;
  holderName: string;
  stateCode: string;
  county: string;
  acreage: number;
  coverageLimit: number;
  deductible: number;
  effectiveDate: string;
  expiryDate: string;
}

export interface EquipmentView {
  id: number;
  category: EquipmentCategory;
  categoryLabel: string;
  displayName: string;
  serialNumber: string;
  insuredValue: number;
  engineHours: number;
  gpsTracked: boolean;
}
