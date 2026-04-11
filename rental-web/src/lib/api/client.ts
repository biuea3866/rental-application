import type { ApiResponse, ApiError } from "./types";
import { getAccessToken, clearTokens } from "@/lib/auth/token";

// ========================================
// API 모드 설정
// ========================================

const API_MODE = process.env.NEXT_PUBLIC_API_MODE || "stub";
const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

// ========================================
// API 클라이언트 인터페이스
// ========================================

export interface ApiClient {
  get<T>(endpoint: string, options?: RequestOptions): Promise<ApiResponse<T>>;
  post<T>(
    endpoint: string,
    body?: unknown,
    options?: RequestOptions
  ): Promise<ApiResponse<T>>;
  put<T>(
    endpoint: string,
    body?: unknown,
    options?: RequestOptions
  ): Promise<ApiResponse<T>>;
  patch<T>(
    endpoint: string,
    body?: unknown,
    options?: RequestOptions
  ): Promise<ApiResponse<T>>;
  delete<T>(
    endpoint: string,
    options?: RequestOptions
  ): Promise<ApiResponse<T>>;
}

export interface RequestOptions {
  headers?: Record<string, string>;
  params?: Record<string, string>;
  requiresAuth?: boolean;
}

// ========================================
// API 에러 클래스
// ========================================

export class ApiRequestError extends Error {
  constructor(
    public status: number,
    public apiError: ApiError
  ) {
    super(apiError.message);
    this.name = "ApiRequestError";
  }
}

// ========================================
// Real API 클라이언트
// ========================================

class RealApiClient implements ApiClient {
  private async request<T>(
    method: string,
    endpoint: string,
    body?: unknown,
    options: RequestOptions = {}
  ): Promise<ApiResponse<T>> {
    const { headers = {}, params, requiresAuth = true } = options;

    const url = new URL(`${BASE_URL}${endpoint}`);
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        url.searchParams.append(key, value);
      });
    }

    const requestHeaders: Record<string, string> = {
      "Content-Type": "application/json",
      ...headers,
    };

    if (requiresAuth) {
      const token = getAccessToken();
      if (token) {
        requestHeaders["Authorization"] = `Bearer ${token}`;
      }
    }

    const response = await fetch(url.toString(), {
      method,
      headers: requestHeaders,
      body: body ? JSON.stringify(body) : undefined,
    });

    if (response.status === 401) {
      clearTokens();
      if (typeof window !== "undefined") {
        window.location.href = "/login";
      }
      throw new ApiRequestError(401, {
        code: "UNAUTHORIZED",
        message: "인증이 만료되었습니다. 다시 로그인해주세요.",
      });
    }

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({
        code: "UNKNOWN_ERROR",
        message: "알 수 없는 오류가 발생했습니다.",
      }));
      throw new ApiRequestError(response.status, errorData as ApiError);
    }

    return response.json();
  }

  async get<T>(
    endpoint: string,
    options?: RequestOptions
  ): Promise<ApiResponse<T>> {
    return this.request<T>("GET", endpoint, undefined, options);
  }

  async post<T>(
    endpoint: string,
    body?: unknown,
    options?: RequestOptions
  ): Promise<ApiResponse<T>> {
    return this.request<T>("POST", endpoint, body, options);
  }

  async put<T>(
    endpoint: string,
    body?: unknown,
    options?: RequestOptions
  ): Promise<ApiResponse<T>> {
    return this.request<T>("PUT", endpoint, body, options);
  }

  async patch<T>(
    endpoint: string,
    body?: unknown,
    options?: RequestOptions
  ): Promise<ApiResponse<T>> {
    return this.request<T>("PATCH", endpoint, body, options);
  }

  async delete<T>(
    endpoint: string,
    options?: RequestOptions
  ): Promise<ApiResponse<T>> {
    return this.request<T>("DELETE", endpoint, undefined, options);
  }
}

// ========================================
// Stub API 클라이언트
// ========================================

class StubApiClient implements ApiClient {
  private handlers: Map<
    string,
    (endpoint: string, body?: unknown) => Promise<unknown>
  > = new Map();

  registerHandler(
    pattern: string,
    handler: (endpoint: string, body?: unknown) => Promise<unknown>
  ) {
    this.handlers.set(pattern, handler);
  }

  private findHandler(
    endpoint: string
  ): ((endpoint: string, body?: unknown) => Promise<unknown>) | undefined {
    for (const [pattern, handler] of this.handlers) {
      if (endpoint.startsWith(pattern) || endpoint.includes(pattern)) {
        return handler;
      }
    }
    return undefined;
  }

  private async handleRequest<T>(
    endpoint: string,
    body?: unknown
  ): Promise<ApiResponse<T>> {
    // 인위적 지연 시뮬레이션
    await new Promise((resolve) => setTimeout(resolve, 200 + Math.random() * 300));

    const handler = this.findHandler(endpoint);
    if (handler) {
      const data = await handler(endpoint, body);
      return {
        success: true,
        data: data as T,
        timestamp: new Date().toISOString(),
      };
    }

    return {
      success: true,
      data: {} as T,
      message: `Stub: No handler for ${endpoint}`,
      timestamp: new Date().toISOString(),
    };
  }

  async get<T>(
    endpoint: string,
    _options?: RequestOptions
  ): Promise<ApiResponse<T>> {
    return this.handleRequest<T>(endpoint);
  }

  async post<T>(
    endpoint: string,
    body?: unknown,
    _options?: RequestOptions
  ): Promise<ApiResponse<T>> {
    return this.handleRequest<T>(endpoint, body);
  }

  async put<T>(
    endpoint: string,
    body?: unknown,
    _options?: RequestOptions
  ): Promise<ApiResponse<T>> {
    return this.handleRequest<T>(endpoint, body);
  }

  async patch<T>(
    endpoint: string,
    body?: unknown,
    _options?: RequestOptions
  ): Promise<ApiResponse<T>> {
    return this.handleRequest<T>(endpoint, body);
  }

  async delete<T>(
    endpoint: string,
    _options?: RequestOptions
  ): Promise<ApiResponse<T>> {
    return this.handleRequest<T>(endpoint);
  }
}

// ========================================
// 클라이언트 팩토리
// ========================================

let clientInstance: ApiClient | null = null;

export function getApiClient(): ApiClient {
  if (clientInstance) return clientInstance;

  if (API_MODE === "stub") {
    const stubClient = new StubApiClient();
    // 스텁 핸들러 등록은 mocks/handlers.ts에서 수행
    clientInstance = stubClient;
  } else {
    clientInstance = new RealApiClient();
  }

  return clientInstance;
}

export function getStubClient(): StubApiClient | null {
  if (API_MODE === "stub" && clientInstance instanceof StubApiClient) {
    return clientInstance;
  }
  return null;
}

/** API 모드 반환 */
export function getApiMode(): "stub" | "real" {
  return API_MODE as "stub" | "real";
}

export { API_MODE, BASE_URL };
