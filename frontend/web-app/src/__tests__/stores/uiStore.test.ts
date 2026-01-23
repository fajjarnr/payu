import { renderHook, act } from '@testing-library/react';
import { vi } from 'vitest';
import { useUIStore } from '@/stores';

describe('useUIStore', () => {
  beforeEach(() => {
    useUIStore.getState().clearToasts();
  });

  test('should initialize with default UI state', () => {
    const { result } = renderHook(() => useUIStore());

    expect(result.current.isSidebarOpen).toBe(true);
    expect(result.current.isLoading).toBe(false);
    expect(result.current.toasts).toEqual([]);
  });

  test('should toggle sidebar', () => {
    const { result } = renderHook(() => useUIStore());

    expect(result.current.isSidebarOpen).toBe(true);

    act(() => {
      result.current.toggleSidebar();
    });

    expect(result.current.isSidebarOpen).toBe(false);

    act(() => {
      result.current.toggleSidebar();
    });

    expect(result.current.isSidebarOpen).toBe(true);
  });

  test('should set sidebar open state', () => {
    const { result } = renderHook(() => useUIStore());

    act(() => {
      result.current.setSidebarOpen(false);
    });

    expect(result.current.isSidebarOpen).toBe(false);

    act(() => {
      result.current.setSidebarOpen(true);
    });

    expect(result.current.isSidebarOpen).toBe(true);
  });

  test('should set loading state', () => {
    const { result } = renderHook(() => useUIStore());

    expect(result.current.isLoading).toBe(false);

    act(() => {
      result.current.setLoading(true);
    });

    expect(result.current.isLoading).toBe(true);

    act(() => {
      result.current.setLoading(false);
    });

    expect(result.current.isLoading).toBe(false);
  });

  test('should add toast', () => {
    const { result } = renderHook(() => useUIStore());

    act(() => {
      result.current.addToast('Test message', 'success');
    });

    expect(result.current.toasts).toHaveLength(1);
    expect(result.current.toasts[0].message).toBe('Test message');
    expect(result.current.toasts[0].type).toBe('success');
    expect(result.current.toasts[0].id).toMatch(/^toast-/);
  });

  test('should add toast with custom duration', () => {
    const { result } = renderHook(() => useUIStore());
    vi.useFakeTimers();

    act(() => {
      result.current.addToast('Test message', 'error', 10000);
    });

    expect(result.current.toasts).toHaveLength(1);

    act(() => {
      vi.advanceTimersByTime(10000);
    });

    expect(result.current.toasts).toHaveLength(0);

    vi.useRealTimers();
  });

  test('should remove toast', () => {
    const { result } = renderHook(() => useUIStore());

    act(() => {
      result.current.addToast('Test message', 'success');
    });

    const toastId = result.current.toasts[0].id;
    expect(result.current.toasts).toHaveLength(1);

    act(() => {
      result.current.removeToast(toastId);
    });

    expect(result.current.toasts).toHaveLength(0);
  });

  test('should clear all toasts', () => {
    const { result } = renderHook(() => useUIStore());

    act(() => {
      result.current.addToast('Test 1', 'success');
      result.current.addToast('Test 2', 'error');
      result.current.addToast('Test 3', 'warning');
    });

    expect(result.current.toasts).toHaveLength(3);

    act(() => {
      result.current.clearToasts();
    });

    expect(result.current.toasts).toHaveLength(0);
  });

  test('should auto-remove toast after duration', () => {
    const { result } = renderHook(() => useUIStore());
    vi.useFakeTimers();

    act(() => {
      result.current.addToast('Auto-remove message', 'info', 5000);
    });

    expect(result.current.toasts).toHaveLength(1);

    act(() => {
      vi.advanceTimersByTime(5000);
    });

    expect(result.current.toasts).toHaveLength(0);

    vi.useRealTimers();
  });
});
