export type AdminRole = 'BUYER_TENANT' | 'OWNER' | 'AGENT' | 'MAINTENANCE_STAFF' | 'ADMIN';

export interface AdminUserResponse {
  id: number;
  username: string;
  email: string;
  phone: string | null;
  roles: string[];
  enabled: boolean;
  seededAdmin: boolean;
  authProvider: string;
}
