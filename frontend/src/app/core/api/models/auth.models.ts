export type RoleName = 'BUYER_TENANT' | 'OWNER' | 'AGENT' | 'MAINTENANCE_STAFF' | 'ADMIN';

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  phone: string;
  role: RoleName;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  tokenType?: string;
}

export interface UserSummaryResponse {
  id: number;
  username: string;
  email: string;
  roles: RoleName[];
}
