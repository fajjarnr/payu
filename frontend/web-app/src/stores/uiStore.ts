import { create } from 'zustand';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

interface Toast {
  id: string;
  message: string;
  type: ToastType;
  duration?: number;
}

interface UIState {
  isSidebarOpen: boolean;
  isLoading: boolean;
  toasts: Toast[];
  toggleSidebar: () => void;
  setSidebarOpen: (open: boolean) => void;
  setLoading: (loading: boolean) => void;
  addToast: (message: string, type: ToastType, duration?: number) => void;
  removeToast: (id: string) => void;
  clearToasts: () => void;
}

// BUG-FE-002: Track timeout IDs per toast to clear on removal
const toastTimeouts = new Map<string, ReturnType<typeof setTimeout>>();

export const useUIStore = create<UIState>((set) => ({
  isSidebarOpen: true,
  isLoading: false,
  toasts: [],

  toggleSidebar: () => set((state) => ({ isSidebarOpen: !state.isSidebarOpen })),

  setSidebarOpen: (open) => set({ isSidebarOpen: open }),

  setLoading: (loading) => set({ isLoading: loading }),

  addToast: (message, type, duration = 5000) => {
    const id = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID()
      : `${Date.now().toString(36)}-${Math.random().toString(36).substring(2, 10)}`;
    const toast = { id, message, type, duration };
    
    set((state) => ({ toasts: [...state.toasts, toast] }));

    if (duration > 0) {
      // BUG-FE-002: Store timeout ID so it can be cleared on manual removal
      const timeoutId = setTimeout(() => {
        toastTimeouts.delete(id);
        set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) }));
      }, duration);
      toastTimeouts.set(id, timeoutId);
    }
  },

  removeToast: (id) => {
    // BUG-FE-002: Clear stored timeout to prevent dangling state updates
    const timeoutId = toastTimeouts.get(id);
    if (timeoutId) {
      clearTimeout(timeoutId);
      toastTimeouts.delete(id);
    }
    set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) }));
  },

  clearToasts: () => {
    // BUG-FE-002: Clear all pending timeouts
    toastTimeouts.forEach((timeoutId) => clearTimeout(timeoutId));
    toastTimeouts.clear();
    set({ toasts: [] });
  }
}));
