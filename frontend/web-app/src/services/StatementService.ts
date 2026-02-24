import api, { isAxiosError } from '@/lib/api';

/**
 * Statement Service Types
 * Based on backend StatementResponse DTO
 */

// XBUG-001 Fix: Changed 'READY' to 'COMPLETED' to match backend StatementStatus enum.
// Backend returns COMPLETED, not READY — frontend was stuck in infinite polling loop.
export type StatementStatus = 'GENERATING' | 'COMPLETED' | 'FAILED';

export interface Statement {
  id: string;
  customerId: string;
  accountNumber: string;
  statementPeriod: string;
  openingBalance: number;
  closingBalance: number;
  totalCredits: number;
  totalDebits: number;
  transactionCount: number;
  status: StatementStatus;
  generatedAt: string;
  createdAt: string;
  periodFormatted: string;
  openingBalanceFormatted: string;
  closingBalanceFormatted: string;
  totalCreditsFormatted: string;
  totalDebitsFormatted: string;
  downloadUrl: string;
}

export interface StatementGenerationRequest {
  // XBUG-005 Fix: Added customerId for backend ownership validation.
  // Without this, users could generate statements for other accounts.
  customerId: string;
  accountNumber: string;
  year: number;
  month: number;
}

export interface StatementsListResponse {
  content: Statement[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ApiResponseType<T> {
  success: boolean;
  data: T;
  message?: string;
  code?: string;
}

/**
 * Statement Period Type
 */
export type PeriodType = 'monthly' | 'quarterly' | 'annually';

/**
 * Statement Format Type
 */
export type StatementFormat = 'PDF' | 'CSV';

/**
 * Statement Service for PayU Digital Banking Platform
 *
 * SECURITY NOTICE:
 * - All API calls are authenticated via JWT tokens (managed by api.ts)
 * - Users can only access their own statements (enforced by backend)
 * - PDF downloads are handled as blob responses
 */
export class StatementService {
  private static instance: StatementService;

  private constructor() {}

  static getInstance(): StatementService {
    if (!StatementService.instance) {
      StatementService.instance = new StatementService();
    }
    return StatementService.instance;
  }

  /**
   * Generate a new statement for a specific period
   * @param request Statement generation request
   * @returns Promise with statement response
   */
  async generateStatement(request: StatementGenerationRequest): Promise<Statement> {
    const response = await api.post<ApiResponseType<Statement>>('/statements/generate', request);
    return response.data.data;
  }

  /**
   * Get statement by ID
   * @param id Statement UUID
   * @returns Promise with statement details
   */
  async getStatement(id: string): Promise<Statement> {
    const response = await api.get<ApiResponseType<Statement>>(`/statements/${id}`);
    return response.data.data;
  }

  /**
   * List all statements for the current user with pagination
   * @param page Page number (default: 0)
   * @param size Page size (default: 12)
   * @returns Promise with paginated statements
   */
  async listStatements(page: number = 0, size: number = 12): Promise<StatementsListResponse> {
    const response = await api.get<ApiResponseType<StatementsListResponse>>('/statements', {
      params: { page, size, sort: 'statementPeriod,desc' }
    });
    return response.data.data;
  }

  /**
   * Get the latest statement for the current user
   * @returns Promise with statement or null if not found
   */
  async getLatestStatement(): Promise<Statement | null> {
    try {
      const response = await api.get<ApiResponseType<Statement>>('/statements/latest');
      return response.data.data;
    } catch (error) {
      // Return null if 404 (no statements found)
      if (isAxiosError(error) && error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  }

  /**
   * Download statement as PDF
   * @param id Statement UUID
   * @returns Promise with blob data
   */
  async downloadStatement(id: string): Promise<Blob> {
    const response = await api.get(`/statements/${id}/download`, {
      responseType: 'blob'
    });
    return response.data;
  }

  /**
   * Download statement with filename handling
   * @param id Statement UUID
   * @param filename Optional custom filename
   */
  async downloadStatementWithFilename(id: string, filename?: string): Promise<void> {
    try {
      const blob = await this.downloadStatement(id);

      // Get statement details for filename if not provided
      let finalFilename = filename;
      if (!finalFilename) {
        const statement = await this.getStatement(id);
        finalFilename = `statement_${statement.periodFormatted.replace(/\s+/g, '_').toLowerCase()}.pdf`;
      }

      // Create download link and trigger download
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = finalFilename;
      document.body.appendChild(link);
      link.click();

      // Cleanup
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Download failed:', error);
      throw error;
    }
  }

  /**
   * Generate and download statement in one call
   * @param request Statement generation request
   * @param pollInterval Polling interval in ms (default: 2000)
   * @param maxAttempts Maximum polling attempts (default: 15)
   * @returns Promise that resolves when download is ready
   */
  async generateAndDownload(
    request: StatementGenerationRequest,
    pollInterval: number = 2000,
    maxAttempts: number = 15
  ): Promise<void> {
    // Generate statement
    const statement = await this.generateStatement(request);

    // Poll until statement is ready
    let attempts = 0;
    while (attempts < maxAttempts) {
      await new Promise(resolve => setTimeout(resolve, pollInterval));

      const updatedStatement = await this.getStatement(statement.id);

      if (updatedStatement.status === 'COMPLETED') {
        // Download when ready
        await this.downloadStatementWithFilename(statement.id);
        return;
      }

      if (updatedStatement.status === 'FAILED') {
        throw new Error('Statement generation failed');
      }

      attempts++;
    }

    throw new Error('Statement generation timeout');
  }

  /**
   * Format period type to display text (Indonesian)
   */
  formatPeriodType(periodType: PeriodType): string {
    const formats: Record<PeriodType, string> = {
      monthly: 'Bulanan',
      quarterly: 'Kuartalan',
      annually: 'Tahunan'
    };
    return formats[periodType] || periodType;
  }

  /**
   * Format statement status to display text (Indonesian)
   */
  formatStatementStatus(status: StatementStatus): string {
    const statuses: Record<StatementStatus, string> = {
      GENERATING: 'Sedang Dibuat',
      COMPLETED: 'Siap Diunduh',
      FAILED: 'Gagal'
    };
    return statuses[status] || status;
  }

  /**
   * Get status badge color class
   */
  getStatusColor(status: StatementStatus): string {
    const colors: Record<StatementStatus, string> = {
      GENERATING: 'bg-warning/10 text-warning border-warning/20',
      COMPLETED: 'bg-primary/10 text-primary border-primary/20',
      FAILED: 'bg-destructive/10 text-destructive border-destructive/20'
    };
    return colors[status] || 'bg-muted/10 text-muted-foreground border-border';
  }
}

export default StatementService.getInstance();
