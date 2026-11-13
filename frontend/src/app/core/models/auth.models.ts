export type Role = 'AGENT' | 'ADJUSTER' | 'MANAGER';

export interface UserProfile {
  id: number;
  email: string;
  fullName: string;
  role: Role;
}
