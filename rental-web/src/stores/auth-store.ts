import { create } from "zustand";
import type { User, AuthTokens } from "@/lib/api/types";
import {
  saveTokens,
  clearTokens,
  hasValidTokens,
  getAccessToken,
  refreshAccessToken,
} from "@/lib/auth/token";

// ========================================
// Auth Store 타입
// ========================================

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
}

interface AuthActions {
  setUser: (user: User) => void;
  setTokens: (tokens: AuthTokens) => void;
  login: (user: User, tokens: AuthTokens) => void;
  logout: () => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  checkAuth: () => Promise<void>;
}

export type AuthStore = AuthState & AuthActions;

// ========================================
// Auth Store 생성
// ========================================

export const useAuthStore = create<AuthStore>((set) => ({
  // State
  user: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,

  // Actions
  setUser: (user: User) =>
    set({ user, isAuthenticated: true, error: null }),

  setTokens: (tokens: AuthTokens) => {
    saveTokens(tokens);
  },

  login: (user: User, tokens: AuthTokens) => {
    saveTokens(tokens);
    set({ user, isAuthenticated: true, isLoading: false, error: null });
  },

  logout: () => {
    clearTokens();
    set({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
    });
  },

  setLoading: (isLoading: boolean) => set({ isLoading }),

  setError: (error: string | null) => set({ error }),

  checkAuth: async () => {
    set({ isLoading: true });

    if (!hasValidTokens()) {
      set({ isAuthenticated: false, isLoading: false, user: null });
      return;
    }

    const token = getAccessToken();
    if (!token) {
      // Access Token 만료 → Refresh 시도
      const newTokens = await refreshAccessToken();
      if (!newTokens) {
        set({ isAuthenticated: false, isLoading: false, user: null });
        return;
      }
    }

    // 토큰이 유효하면 인증 상태 유지 (유저 정보는 별도 API로 조회)
    set({ isAuthenticated: true, isLoading: false });
  },
}));
