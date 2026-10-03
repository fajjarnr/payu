import React from 'react';
import { screen, fireEvent, waitFor, act } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { vi, type Mock } from 'vitest';
import { FeedbackWidget } from '@/components/feedback/FeedbackWidget';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

import userEvent from '@testing-library/user-event';

vi.mock('@/lib/a11y', () => ({
  a11yUtils: {
    useFocusTrap: vi.fn(),
  },
}));

global.fetch = vi.fn();

Object.defineProperty(navigator, 'mediaDevices', {
  writable: true,
  value: {
    getDisplayMedia: vi.fn(),
  },
});

expect.extend(toHaveNoViolations);

describe('FeedbackWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    global.fetch = vi.fn(() =>
      Promise.resolve({
        ok: true,
        json: async () => ({ success: true }),
      } as Response)
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should render floating button when closed', () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    expect(floatingButton).toBeInTheDocument();
    expect(floatingButton).toHaveClass('bg-bank-green', 'text-white');
  });

  it('should open modal when floating button is clicked', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    expect(screen.getByText('Kirim Feedback')).toBeInTheDocument();
  });

  it('should close modal when close button is clicked', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    const closeButtons = screen.getAllByRole('button', { name: 'Close' });
    expect(closeButtons.length).toBeGreaterThan(0);
    await act(async () => {
      fireEvent.click(closeButtons[0]);
    });

    // antd Modal plays a leave animation before unmounting; jsdom never
    // finishes CSS transitions on its own, so complete it explicitly,
    // retrying until the leave transition has started.
    for (let i = 0; i < 10; i++) {
      let gone = false;
      await act(async () => {
        const dialog = screen.queryByRole('dialog');
        if (dialog) {
          fireEvent.transitionEnd(dialog);
          fireEvent.animationEnd(dialog);
        }
        await new Promise((r) => setTimeout(r, 200));
        gone = screen.queryByRole('dialog') === null;
      });
      if (gone) break;
    }
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('should close modal when backdrop is clicked', async () => {
    const { container } = renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    const backdrop = container.querySelector('.bg-black\\/50');
    if (backdrop) {
      await act(async () => {
        fireEvent.click(backdrop);
      });

      await waitFor(() => {
        expect(screen.queryByRole('heading', { name: 'Kirim Feedback' })).not.toBeInTheDocument();
      });
    }
  });

  it('should render all category options', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    expect(screen.getByText('Laporan Bug')).toBeInTheDocument();
    expect(screen.getByText('Saran Fitur')).toBeInTheDocument();
    expect(screen.getByText('Lainnya')).toBeInTheDocument();
  });

  it('should select category when clicked', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    const bugCategory = screen.getByText('Laporan Bug').closest('button');
    if (bugCategory) {
      await act(async () => {
        fireEvent.click(bugCategory);
      });
      expect(bugCategory).toHaveClass('border-bank-green');
    }
  });

  it('should require subject and message', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    const submitButton = screen.getByRole('button', { name: 'Kirim Sekarang' });
    expect(submitButton).toBeDisabled();
  });

  it('should enable submit button when subject and message are filled', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    const subjectInput = screen.getByLabelText('Subjek');
    const messageInput = screen.getByLabelText('Pesan');

    await act(async () => {
      await userEvent.type(subjectInput, 'Test bug report');
    });
    await act(async () => {
      await userEvent.type(messageInput, 'This is a detailed description of the bug');
    });

    const submitButton = screen.getByRole('button', { name: 'Kirim Sekarang' });
    expect(submitButton).not.toBeDisabled();
  });

  it('should submit feedback successfully', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    const subjectInput = screen.getByLabelText('Subjek');
    const messageInput = screen.getByLabelText('Pesan');

    await act(async () => {
      await userEvent.type(subjectInput, 'Test bug report');
    });
    await act(async () => {
      await userEvent.type(messageInput, 'This is a detailed description of the bug');
    });

    const submitButton = screen.getByRole('button', { name: 'Kirim Sekarang' });
    await act(async () => {
      fireEvent.click(submitButton);
    });

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/feedback'),
        expect.objectContaining({
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: expect.stringContaining('Test bug report'),
        })
      );
    });
  });

  it('should show success message after submission', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    const subjectInput = screen.getByLabelText('Subjek');
    const messageInput = screen.getByLabelText('Pesan');

    await act(async () => {
      await userEvent.type(subjectInput, 'Test bug report');
    });
    await act(async () => {
      await userEvent.type(messageInput, 'This is a detailed description of the bug');
    });

    const submitButton = screen.getByRole('button', { name: 'Kirim Sekarang' });
    await act(async () => {
      fireEvent.click(submitButton);
    });

    await waitFor(() => {
      expect(screen.getByText('Terima Kasih!')).toBeInTheDocument();
    });
  });

  it('should clear the success timeout when unmounted', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const setTimeoutSpy = vi.spyOn(globalThis, 'setTimeout');
    const clearTimeoutSpy = vi.spyOn(globalThis, 'clearTimeout');
    const { unmount } = renderWithIntl(<FeedbackWidget />);

    await act(async () => {
      fireEvent.click(screen.getByLabelText('Kirim Feedback'));
    });

    fireEvent.change(screen.getByLabelText('Subjek'), { target: { value: 'Test' } });
    fireEvent.change(screen.getByLabelText('Pesan'), { target: { value: 'Test message' } });
    fireEvent.click(screen.getByRole('button', { name: 'Kirim Sekarang' }));

    await waitFor(() => {
      expect(screen.getByText('Terima Kasih!')).toBeInTheDocument();
    });
    const successTimeoutIndex = setTimeoutSpy.mock.calls.findIndex(([, delay]) => delay === 3000);
    const successTimeoutId = setTimeoutSpy.mock.results[successTimeoutIndex]?.value;
    expect(successTimeoutId).toBeDefined();

    unmount();

    expect(clearTimeoutSpy).toHaveBeenCalledWith(successTimeoutId);
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  it('should close modal after success and delay', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });

    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    const subjectInput = screen.getByLabelText('Subjek');
    const messageInput = screen.getByLabelText('Pesan');

    await act(async () => {
      await userEvent.type(subjectInput, 'Test bug report');
    });
    await act(async () => {
      await userEvent.type(messageInput, 'This is a detailed description of the bug');
    });

    const submitButton = screen.getByRole('button', { name: 'Kirim Sekarang' });
    await act(async () => {
      fireEvent.click(submitButton);
    });

    await waitFor(() => {
      expect(screen.getByText('Terima Kasih!')).toBeInTheDocument();
    });

    await act(async () => {
      vi.advanceTimersByTime(3000);
    });

    await waitFor(() => {
      expect(screen.queryByRole('heading', { name: 'Kirim Feedback' })).not.toBeInTheDocument();
    });

    vi.useRealTimers();
  });

  it('should display character count for message', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    const messageInput = screen.getByLabelText('Pesan');
    await act(async () => {
      await userEvent.type(messageInput, 'Test');
    });

    expect(screen.getByText('4 / 1000')).toBeInTheDocument();
  });

  it('should enforce max length on subject', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    const subjectInput = screen.getByLabelText('Subjek') as HTMLInputElement;
    expect(subjectInput).toHaveAttribute('maxLength', '100');
  });

  it('should enforce max length on message', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    const messageInput = screen.getByLabelText('Pesan') as HTMLTextAreaElement;
    expect(messageInput).toHaveAttribute('maxLength', '1000');
  });

  it('should have screenshot checkbox', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    const screenshotCheckbox = screen.getByLabelText('Sertakan tangkapan layar otomatis');
    expect(screenshotCheckbox).toBeInTheDocument();
    expect(screenshotCheckbox).toBeChecked();
  });

  it('should attempt to capture screenshot when checkbox is enabled', async () => {
    const mockStream = {
      getTracks: vi.fn(() => [{ stop: vi.fn() }]),
    };

    (navigator.mediaDevices.getDisplayMedia as Mock).mockResolvedValue(mockStream);

    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    const screenshotCheckbox = screen.getByLabelText('Sertakan tangkapan layar otomatis');
    await act(async () => {
      fireEvent.click(screenshotCheckbox);
    });
    await act(async () => {
      fireEvent.click(screenshotCheckbox);
    });

    await waitFor(() => {
      expect(navigator.mediaDevices.getDisplayMedia).toHaveBeenCalled();
    });
  });

  it('should handle screenshot capture failure gracefully', async () => {
    (navigator.mediaDevices.getDisplayMedia as Mock).mockRejectedValue(
      new Error('Capture failed')
    );

    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    const screenshotCheckbox = screen.getByLabelText('Sertakan tangkapan layar otomatis');
    await act(async () => {
      fireEvent.click(screenshotCheckbox);
    });
    await act(async () => {
      fireEvent.click(screenshotCheckbox);
    });

    expect(() => {
      fireEvent.click(screenshotCheckbox);
    }).not.toThrow();
  });

  it('should have proper ARIA attributes', () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    expect(floatingButton).toHaveAttribute('aria-label');
  });

  it('should have dialog role when open', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    const dialog = screen.getByRole('dialog');
    expect(dialog).toBeInTheDocument();
    expect(dialog).toHaveAttribute('aria-modal', 'true');
  });

  it('should have no accessibility violations', async () => {
    const { container } = renderWithIntl(<FeedbackWidget />);

    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });

  it('should use custom API endpoint when provided', async () => {
    renderWithIntl(<FeedbackWidget apiEndpoint="/custom/feedback" />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    const subjectInput = screen.getByLabelText('Subjek');
    const messageInput = screen.getByLabelText('Pesan');

    await act(async () => {
      await userEvent.type(subjectInput, 'Test');
    });
    await act(async () => {
      await userEvent.type(messageInput, 'Test message');
    });

    const submitButton = screen.getByRole('button', { name: 'Kirim Sekarang' });
    await act(async () => {
      fireEvent.click(submitButton);
    });

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/custom/feedback'),
        expect.any(Object)
      );
    });
  });

  it('should use custom categories when provided', async () => {
    const customCategories = [
      { value: 'bug', label: 'Report Bug', icon: <div>Bug Icon</div> },
      { value: 'feature', label: 'Request Feature', icon: <div>Feature Icon</div> },
    ];

    renderWithIntl(<FeedbackWidget categories={customCategories} />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    expect(screen.getByText('Report Bug')).toBeInTheDocument();
    expect(screen.getByText('Request Feature')).toBeInTheDocument();
    expect(screen.queryByText('Lainnya')).not.toBeInTheDocument();
  });

  it('should disable submit while submitting', async () => {
    global.fetch = vi.fn(() => new Promise<Response>(() => {})); // Never resolves

    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    const subjectInput = screen.getByLabelText('Subjek');
    const messageInput = screen.getByLabelText('Pesan');

    await act(async () => {
      await userEvent.type(subjectInput, 'Test');
    });
    await act(async () => {
      await userEvent.type(messageInput, 'Test message');
    });

    const submitButton = screen.getByRole('button', { name: 'Kirim Sekarang' });
    await act(async () => {
      fireEvent.click(submitButton);
    });

    await waitFor(() => {
      expect(submitButton).toBeDisabled();
      expect(screen.getByText('Mengirim Data...')).toBeInTheDocument();
    });
  });

  it('should collect device info', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    const subjectInput = screen.getByLabelText('Subjek');
    const messageInput = screen.getByLabelText('Pesan');

    await act(async () => {
      await userEvent.type(subjectInput, 'Test');
    });
    await act(async () => {
      await userEvent.type(messageInput, 'Test message');
    });

    const submitButton = screen.getByRole('button', { name: 'Kirim Sekarang' });
    await act(async () => {
      fireEvent.click(submitButton);
    });

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/feedback'),
        expect.objectContaining({
          body: expect.stringContaining('userAgent'),
        })
      );
    });
  });

  it('should display data collection notice', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(floatingButton);
    });

    expect(screen.getByText(/Informasi perangkat dan log error/)).toBeInTheDocument();
  });

  it('should reset form after submission', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });

    const { rerender } = renderWithIntl(<FeedbackWidget />); // eslint-disable-line @typescript-eslint/no-unused-vars

    const floatingButton = screen.getByLabelText('Kirim Feedback');
    fireEvent.click(floatingButton);

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    const subjectInput = screen.getByLabelText('Subjek') as HTMLInputElement;
    const messageInput = screen.getByLabelText('Pesan') as HTMLTextAreaElement;

    await act(async () => {
      await userEvent.type(subjectInput, 'Test subject');
    });
    await act(async () => {
      await userEvent.type(messageInput, 'Test message');
    });

    const submitButton = screen.getByRole('button', { name: 'Kirim Sekarang' });
    await act(async () => {
      fireEvent.click(submitButton);
    });

    await waitFor(() => {
      expect(screen.getByText('Terima Kasih!')).toBeInTheDocument();
    });

    act(() => {
      vi.advanceTimersByTime(3000);
    });


    const newFloatingButton = screen.getByLabelText('Kirim Feedback');
    await act(async () => {
      fireEvent.click(newFloatingButton);
    });

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    const newSubjectInput = screen.getByLabelText('Subjek') as HTMLInputElement;
    const newMessageInput = screen.getByLabelText('Pesan') as HTMLTextAreaElement;

    expect(newSubjectInput.value).toBe('');
    expect(newMessageInput.value).toBe('');

    vi.useRealTimers();
  });
});
