import {
  get,
  post,
  put,
  BASE_URL,
  ApiError,
  handleAuthSessionExpired,
  type ApiResponse,
} from './request';

export interface AiGenerateRequest {
  description?: string;
  moduleKey?: string;
  moduleName?: string;
  businessType?: string;
  model?: string;
  provider?: 'openai' | 'claude' | string;
  apiKey?: string;
  baseUrl?: string;
  prompt?: string | unknown;
  input?: string | unknown;
  system?: string | unknown;
  messages?: Array<{
    role?: 'system' | 'user' | 'assistant' | string;
    content?: string | Array<{ type?: string; text?: string; [key: string]: unknown }>;
    [key: string]: unknown;
  }>;
  metadata?: Record<string, unknown>;
  module?: Record<string, unknown>;
}

export interface GeneratedFile {
  path: string;
  type: string;
  content: string;
}

export interface AiGenerateResponse {
  taskNo: string;
  status: string;
  schema: Record<string, unknown>;
  files: GeneratedFile[];
}

export interface AiGenerateStreamProgress {
  message: string;
  phase?: string;
  progress?: number;
  status?: 'pending' | 'running' | 'passed' | 'failed' | string;
}

export type AiGenerateStreamEvent =
  | { type: 'progress'; data: AiGenerateStreamProgress }
  | { type: 'done'; data: AiGenerateResponse };

export interface AiClarificationResponse {
  needsClarification: boolean;
  qualityScore: number;
  requirementsSnapshot: Record<string, unknown>;
  questions: Array<{
    id: string;
    question: string;
    reason: string;
    examples: string[];
  }>;
  warnings: string[];
}

export interface AiPreviewResponse {
  taskNo: string;
  status: string;
  moduleKey: string;
  moduleName: string;
  schema?: Record<string, unknown>;
  metadata?: Record<string, unknown> | null;
  files: GeneratedFile[];
  validation?: {
    passed: boolean;
    score: number;
    issues: Array<{
      level: 'error' | 'warning' | string;
      code: string;
      message: string;
    }>;
  };
}

export interface AiGenerationTaskSummary {
  taskNo: string;
  moduleKey: string;
  moduleName: string;
  status: string;
  smokeTestStatus?: 'PASSED' | 'FAILED' | string | null;
  smokeTestTime?: string | null;
  createTime?: string;
  updateTime?: string;
}

export interface AiApplyPlan {
  taskNo: string;
  moduleKey: string;
  moduleName: string;
  canApply: boolean;
  riskScore: number;
  riskLevel: 'low' | 'medium' | 'high' | string;
  operations: Array<{
    category: string;
    action: string;
    target: string;
    description: string;
    riskLevel: 'low' | 'medium' | 'high' | string;
  }>;
  warnings: string[];
}

export interface AiModuleVersionSummary {
  moduleKey: string;
  moduleName: string;
  versionNo: number;
  taskNo: string;
  schemaHash: string;
  current: boolean;
  createTime?: string;
}

export interface AiProviderConfig {
  provider: 'template' | 'openai' | 'claude' | string;
  model: string;
  baseUrl: string;
  hasApiKey: boolean;
  maskedApiKey: string;
}

export interface AiProviderConfigRequest {
  provider: string;
  model?: string;
  baseUrl?: string;
  apiKey?: string;
  clearApiKey?: boolean;
}

export function generateAiModule(payload: AiGenerateRequest) {
  return post<AiGenerateResponse>('/admin/ai/modules/generate', payload);
}

export async function generateAiModuleStream(
  payload: AiGenerateRequest,
  onEvent?: (event: AiGenerateStreamEvent) => void
) {
  const response = await fetch(`${BASE_URL}/admin/ai/modules/generate/stream`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      Accept: 'text/event-stream',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok || !response.body) {
    const error = await parseErrorResponse(response);
    const code = error.code || response.status;
    if (handleAuthSessionExpired(code)) {
      throw new ApiError('登录已过期，请重新登录', code);
    }
    throw new ApiError(error.msg || `服务器错误 (${response.status})`, code);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let result: AiGenerateResponse | null = null;

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const frames = buffer.split(/\r?\n\r?\n/);
      buffer = frames.pop() || '';
      for (const frame of frames) {
        result = handleSseFrame(frame, onEvent) || result;
      }
    }

    buffer += decoder.decode();
    if (buffer.trim()) {
      result = handleSseFrame(buffer, onEvent) || result;
    }
  } finally {
    reader.releaseLock();
  }

  if (!result) {
    throw new ApiError('生成接口未返回结果', 500);
  }
  return result;
}

function handleSseFrame(
  frame: string,
  onEvent?: (event: AiGenerateStreamEvent) => void
): AiGenerateResponse | null {
  const lines = frame.split(/\r?\n/);
  let eventName = 'message';
  const dataLines: string[] = [];

  for (const line of lines) {
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim();
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart());
    }
  }

  if (dataLines.length === 0) return null;
  const parsed = JSON.parse(dataLines.join('\n')) as
    | ApiResponse<AiGenerateResponse>
    | AiGenerateStreamProgress;

  if (eventName === 'progress') {
    onEvent?.({
      type: 'progress',
      data: parsed as AiGenerateStreamProgress,
    });
    return null;
  }

  const data = parsed as ApiResponse<AiGenerateResponse>;

  if (eventName === 'error' || data.code !== 0) {
    if (handleAuthSessionExpired(data.code)) {
      throw new ApiError('登录已过期，请重新登录', data.code);
    }
    throw new ApiError(data.msg || '生成失败', data.code || 500);
  }

  if (eventName === 'done') {
    const generated = data.data as AiGenerateResponse;
    onEvent?.({ type: 'done', data: generated });
    return generated;
  }

  return null;
}

async function parseErrorResponse(response: Response): Promise<Partial<ApiResponse>> {
  try {
    const text = await response.text();
    if (!text) return {};
    return JSON.parse(text) as Partial<ApiResponse>;
  } catch {
    return {};
  }
}

export function clarifyAiModule(payload: AiGenerateRequest) {
  return post<AiClarificationResponse>('/admin/ai/modules/clarify', payload);
}

export function getAiProviderConfig() {
  return get<AiProviderConfig>('/admin/ai/modules/provider-config');
}

export function saveAiProviderConfig(payload: AiProviderConfigRequest) {
  return put<AiProviderConfig>('/admin/ai/modules/provider-config', payload);
}

export function listAiModuleTasks(limit = 20) {
  return get<AiGenerationTaskSummary[]>('/admin/ai/modules/tasks', { limit });
}

export function previewAiModule(taskNo: string) {
  return get<AiPreviewResponse>(`/admin/ai/modules/${taskNo}/preview`);
}

export function planAiModuleApply(taskNo: string) {
  return get<AiApplyPlan>(`/admin/ai/modules/${taskNo}/plan`);
}

export function applyAiModule(taskNo: string) {
  return post<Record<string, unknown>>(`/admin/ai/modules/${taskNo}/apply`);
}

export function rollbackAiModule(taskNo: string) {
  return post<Record<string, unknown>>(`/admin/ai/modules/${taskNo}/rollback`);
}

export function listAiModuleVersions(moduleKey: string) {
  return get<AiModuleVersionSummary[]>(`/admin/ai/modules/${moduleKey}/versions`);
}

export function getAiModuleDesignerMetadata(moduleKey: string) {
  return get<Record<string, unknown>>(`/admin/ai/modules/${moduleKey}/designer`);
}

export function saveAiModuleDesignerMetadata(moduleKey: string, metadata: Record<string, unknown>) {
  return put<Record<string, unknown>>(`/admin/ai/modules/${moduleKey}/designer`, metadata);
}

export function restoreAiModuleVersion(moduleKey: string, versionNo: number) {
  return post<Record<string, unknown>>(
    `/admin/ai/modules/${moduleKey}/versions/${versionNo}/restore`
  );
}
