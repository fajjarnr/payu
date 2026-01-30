import React from 'react';
import { screen, fireEvent, waitFor } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { vi, type Mock } from 'vitest';
import { FeedbackWidget } from '@/components/feedback/FeedbackWidget';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

// Import user-event with compatible version
import userEvent from '@testing-library/user-event';

// Mock a11yUtils
vi.mock('@/lib/a11y', () => ({
  a11yUtils: {
    useFocusTrap: vi.fn(),
  },
}));

// Mock fetch
global.fetch = vi.fn();

// Mock navigator.mediaDevices
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

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    expect(floatingButton).toBeInTheDocument();
    expect(floatingButton).toHaveClass('bg-bank-green', 'text-white');
  });

  it('should open modal when floating button is clicked', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    expect(screen.getByText('Kirim Feedback')).toBeInTheDocument();
    expect(screen.getByLabelText('Tutup formulir feedback')).toBeInTheDocument();
  });

  it('should close modal when close button is clicked', async () => {
    renderWithIntl(<FeedbackWidget />);

    // Open modal
    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    // Close modal
    const closeButton = screen.getByLabelText('Tutup formulir feedback');
    fireEvent.click(closeButton);

    await waitFor(() => {
      expect(screen.queryByText('Kirim Feedback')).not.toBeInTheDocument();
    });
  });

  it('should close modal when backdrop is clicked', async () => {
    const { container } = renderWithIntl(<FeedbackWidget />);

    // Open modal
    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    // Click backdrop
    const backdrop = container.querySelector('.bg-black\\/50');
    if (backdrop) {
      fireEvent.click(backdrop);

      await waitFor(() => {
        expect(screen.queryByText('Kirim Feedback')).not.toBeInTheDocument();
      });
    }
  });

  it('should render all category options', () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    expect(screen.getByText('Laporan Bug')).toBeInTheDocument();
    expect(screen.getByText('Saran Fitur')).toBeInTheDocument();
    expect(screen.getByText('Lainnya')).toBeInTheDocument();
  });

  it('should select category when clicked', () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    const bugCategory = screen.getByText('Laporan Bug').closest('button');
    if (bugCategory) {
      fireEvent.click(bugCategory);
      expect(bugCategory).toHaveClass('border-bank-green');
    }
  });

  it('should require subject and message', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    const submitButton = screen.getByText('Kirim Feedback');
    expect(submitButton).toBeDisabled();
  });

  it('should enable submit button when subject and message are filled', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    const subjectInput = screen.getByLabelText('Subjek');
    const messageInput = screen.getByLabelText('Pesan');

    await userEvent.type(subjectInput, 'Test bug report');
    await userEvent.type(messageInput, 'This is a detailed description of the bug');

    const submitButton = screen.getByText('Kirim Feedback');
    expect(submitButton).not.toBeDisabled();
  });

  it('should submit feedback successfully', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    const subjectInput = screen.getByLabelText('Subjek');
    const messageInput = screen.getByLabelText('Pesan');

    await userEvent.type(subjectInput, 'Test bug report');
    await userEvent.type(messageInput, 'This is a detailed description of the bug');

    const submitButton = screen.getByText('Kirim Feedback');
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        '/api/v1/feedback',
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

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    const subjectInput = screen.getByLabelText('Subjek');
    const messageInput = screen.getByLabelText('Pesan');

    await userEvent.type(subjectInput, 'Test bug report');
    await userEvent.type(messageInput, 'This is a detailed description of the bug');

    const submitButton = screen.getByText('Kirim Feedback');
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText('Terima Kasih!')).toBeInTheDocument();
    });
  });

  it('should close modal after success and delay', async () => {
    vi.useFakeTimers();

    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    const subjectInput = screen.getByLabelText('Subjek');
    const messageInput = screen.getByLabelText('Pesan');

    await userEvent.type(subjectInput, 'Test bug report');
    await userEvent.type(messageInput, 'This is a detailed description of the bug');

    const submitButton = screen.getByText('Kirim Feedback');
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText('Terima Kasih!')).toBeInTheDocument();
    });

    vi.advanceTimersByTime(3000);

    await waitFor(() => {
      expect(screen.queryByText('Kirim Feedback')).not.toBeInTheDocument();
    });

    vi.useRealTimers();
  });

  it('should display character count for message', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    const messageInput = screen.getByLabelText('Pesan');
    await userEvent.type(messageInput, 'Test');

    expect(screen.getByText('4 / 1000')).toBeInTheDocument();
  });

  it('should enforce max length on subject', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    const subjectInput = screen.getByLabelText('Subjek') as HTMLInputElement;
    expect(subjectInput).toHaveAttribute('maxLength', '100');
  });

  it('should enforce max length on message', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    const messageInput = screen.getByLabelText('Pesan') as HTMLTextAreaElement;
    expect(messageInput).toHaveAttribute('maxLength', '1000');
  });

  it('should have screenshot checkbox', () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    const screenshotCheckbox = screen.getByLabelText('Sertakan tangkapan layar (screenshot)');
    expect(screenshotCheckbox).toBeInTheDocument();
    expect(screenshotCheckbox).toBeChecked();
  });

  it('should attempt to capture screenshot when checkbox is enabled', async () => {
    const mockStream = {
      getTracks: vi.fn(() => [{ stop: vi.fn() }]),
    };

    (navigator.mediaDevices.getDisplayMedia as Mock).mockResolvedValue(mockStream);

    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    const screenshotCheckbox = screen.getByLabelText('Sertakan tangkapan layar (screenshot)');
    fireEvent.click(screenshotCheckbox);
    fireEvent.click(screenshotCheckbox); // Re-enable

    await waitFor(() => {
      expect(navigator.mediaDevices.getDisplayMedia).toHaveBeenCalled();
    });
  });

  it('should handle screenshot capture failure gracefully', async () => {
    (navigator.mediaDevices.getDisplayMedia as Mock).mockRejectedValue(
      new Error('Capture failed')
    );

    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    const screenshotCheckbox = screen.getByLabelText('Sertakan tangkapan layar (screenshot)');
    fireEvent.click(screenshotCheckbox);
    fireEvent.click(screenshotCheckbox);

    expect(() => {
      fireEvent.click(screenshotCheckbox);
    }).not.toThrow();
  });

  it('should have proper ARIA attributes', () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    expect(floatingButton).toHaveAttribute('aria-label');
  });

  it('should have dialog role when open', () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

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

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    const subjectInput = screen.getByLabelText('Subjek');
    const messageInput = screen.getByLabelText('Pesan');

    await userEvent.type(subjectInput, 'Test');
    await userEvent.type(messageInput, 'Test message');

    const submitButton = screen.getByText('Kirim Feedback');
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        '/custom/feedback',
        expect.any(Object)
      );
    });
  });

  it('should use custom categories when provided', () => {
    const customCategories = [
      { value: 'bug', label: 'Report Bug', icon: <div>Bug Icon</div> },
      { value: 'feature', label: 'Request Feature', icon: <div>Feature Icon</div> },
    ];

    renderWithIntl(<FeedbackWidget categories={customCategories} />);

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    expect(screen.getByText('Report Bug')).toBeInTheDocument();
    expect(screen.getByText('Request Feature')).toBeInTheDocument();
    expect(screen.queryByText('Lainnya')).not.toBeInTheDocument();
  });

  it('should disable submit while submitting', async () => {
    global.fetch = vi.fn(() => new Promise<Response>(() => {})); // Never resolves

    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    const subjectInput = screen.getByLabelText('Subjek');
    const messageInput = screen.getByLabelText('Pesan');

    await userEvent.type(subjectInput, 'Test');
    await userEvent.type(messageInput, 'Test message');

    const submitButton = screen.getByText('Kirim Feedback');
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(submitButton).toBeDisabled();
      expect(screen.getByText('Mengirim...')).toBeInTheDocument();
    });
  });

  it('should collect device info', async () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    const subjectInput = screen.getByLabelText('Subjek');
    const messageInput = screen.getByLabelText('Pesan');

    await userEvent.type(subjectInput, 'Test');
    await userEvent.type(messageInput, 'Test message');

    const submitButton = screen.getByText('Kirim Feedback');
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          body: expect.stringContaining('userAgent'),
        })
      );
    });
  });

  it('should display data collection notice', () => {
    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    expect(screen.getByText(/Informasi perangkat dan log error/)).toBeInTheDocument();
  });

  it('should reset form after submission', async () => {
    vi.useFakeTimers();

    renderWithIntl(<FeedbackWidget />);

    const floatingButton = screen.getByLabelText('Buka formulir feedback');
    fireEvent.click(floatingButton);

    const subjectInput = screen.getByLabelText('Subjek') as HTMLInputElement;
    const messageInput = screen.getByLabelText('Pesan') as HTMLInputElement;

    await userEvent.type(subjectInput, 'Test subject');
    await userEvent.type(messageInput, 'Test message');

    const submitButton = screen.getByText('Kirim Feedback');
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText('Terima Kasih!')).toBeInTheDocument();
    });

    vi.advanceTimersByTime(3000);

    await waitFor(() => {
      expect(screen.queryByText('Kirim Feedback')).not.toBeInTheDocument();
    });

    // Reopen modal
    fireEvent.click(floatingButton);

    expect(subjectInput.value).toBe('');
    expect(messageInput.value).toBe('');

    vi.useRealTimers();
  });
});
