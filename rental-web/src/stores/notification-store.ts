import { create } from "zustand";

// ========================================
// Notification Store 타입
// ========================================

interface NotificationState {
  unreadCount: number;
}

interface NotificationActions {
  setUnreadCount: (count: number) => void;
  decrementUnreadCount: () => void;
  clearUnreadCount: () => void;
}

export type NotificationStore = NotificationState & NotificationActions;

// ========================================
// Notification Store 생성
// ========================================

export const useNotificationStore = create<NotificationStore>((set) => ({
  // State
  unreadCount: 0,

  // Actions
  setUnreadCount: (count: number) => set({ unreadCount: count }),

  decrementUnreadCount: () =>
    set((state) => ({
      unreadCount: Math.max(0, state.unreadCount - 1),
    })),

  clearUnreadCount: () => set({ unreadCount: 0 }),
}));
