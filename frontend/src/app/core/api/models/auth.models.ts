export type RoleName = 'BUYER_TENANT' | 'OWNER' | 'AGENT' | 'ADMIN';

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  phoneNumber: string;
  role: RoleName;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  expiresIn?: number;
  username?: string;
  roles?: string[];
}
