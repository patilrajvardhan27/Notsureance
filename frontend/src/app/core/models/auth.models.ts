export type Role = 'AGENT' | 'ADJUSTER';

export interface UserProfile {
  id: number;
  email: string;
  fullName: string;
  role: Role;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  user: UserProfile;
}
