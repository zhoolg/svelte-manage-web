import { get } from './request';

export interface RuntimeInfo {
  application: string;
  environment: string;
  springBootVersion: string;
  javaVersion: string;
  os: string;
  startedAt: string;
  uptimeSeconds: number;
}

export interface ResourceInfo {
  processors: number;
  systemCpuLoad: number;
  processCpuLoad: number;
  heapUsedBytes: number;
  heapMaxBytes: number;
  diskUsedBytes: number;
  diskTotalBytes: number;
}

export interface HealthComponent {
  status: 'UP' | 'WARN' | 'DOWN' | string;
  message: string;
  latencyMs: number;
  details: Record<string, unknown>;
}

export interface AiFactoryInfo {
  status: 'UP' | 'WARN' | 'DOWN' | string;
  enabledDynamicModules: number;
  message: string;
}

export interface SecurityInfo {
  activeSessions: number;
  recentLoginFailures: number;
  lockedIpCount: number;
  lastAdminLoginTime?: string | null;
}

export interface StatusCheck {
  key: string;
  label: string;
  status: 'UP' | 'WARN' | 'DOWN' | string;
  message: string;
  latencyMs: number;
}

export interface SystemStatus {
  status: 'UP' | 'WARN' | 'DOWN' | string;
  checkedAt: string;
  runtime: RuntimeInfo;
  resources: ResourceInfo;
  database: HealthComponent;
  redis: HealthComponent;
  aiFactory: AiFactoryInfo;
  security?: SecurityInfo;
  checks: StatusCheck[];
}

export const systemApi = {
  getStatus: () => get<SystemStatus>('/admin/system/status'),
};
