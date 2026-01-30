import api from '@/lib/api';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
  token_type: string;
}

/**
 * Authentication Service for PayU Digital Banking Platform
 *
 * SECURITY NOTICE: Token Storage
 * ================================
 * This service does NOT store JWT tokens client-side (localStorage/sessionStorage).
 * Tokens are managed exclusively via httpOnly cookies from the backend.
 *
 * Why httpOnly cookies?
 * - Prevents XSS attacks from stealing tokens
 * - Browser automatically includes cookies with requests
 * - HttpOnly flag prevents JavaScript access
 * - Secure flag ensures HTTPS-only transmission
 * - SameSite flag prevents CSRF attacks
 *
 * Token Flow:
 * 1. Login: Backend sets httpOnly cookies containing tokens
 * 2. Requests: Browser automatically includes cookies
 * 3. Refresh: Backend rotates tokens in cookies
 * 4. Logout: Backend clears cookies
 *
 * References:
 * - OWASP: https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html
 * - PCI-DSS: Requirement 8.2.4 - Secure authentication handling
 */
export class AuthService {
  private static instance: AuthService;
  // Session state only - no sensitive tokens stored here
  private authenticated: boolean = false;
  private userSession: { user: any; accountId: string } | null = null;

  private constructor() { }

  static getInstance(): AuthService {
    if (!AuthService.instance) {
      AuthService.instance = new AuthService();
    }
    return AuthService.instance;
  }

  /**
   * Authenticates user credentials.
   * Tokens are set by backend as httpOnly cookies - not stored client-side.
   *
   * @param credentials User login credentials
   * @returns Login response with token metadata (tokens are in cookies)
   */
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await api.post<LoginResponse>('/auth/login', credentials);
    const { access_token, refresh_token, expires_in, token_type } = response.data;

    // Mark as authenticated - actual tokens are in httpOnly cookies
    this.authenticated = true;

    return response.data;
  }

  /**
   * Logs out the user by clearing the session.
   * Backend will clear httpOnly cookies.
   */
  logout(): void {
    this.authenticated = false;
    this.userSession = null;
    // Note: Backend clears the httpOnly cookies
  }

  /**
   * Sets the current user session data (non-sensitive data only).
   * Tokens are NOT stored here - they're in httpOnly cookies.
   *
   * @param user User profile data
   * @param accountId Account identifier
   */
  setUserSession(user: any, accountId: string): void {
    this.userSession = { user, accountId };
  }

  /**
   * Gets the current user session if authenticated.
   */
  getUserSession() {
    return this.userSession;
  }

  /**
   * Checks if user is authenticated.
   * Actual authentication is validated by backend via httpOnly cookies.
   */
  isAuthenticated(): boolean {
    return this.authenticated;
  }

  /**
   * @deprecated Tokens are managed via httpOnly cookies by the backend.
   * Use isAuthenticated() to check auth state.
   */
  getAccessToken(): string | null {
    // Tokens are not accessible from JavaScript (httpOnly cookies)
    console.warn('getAccessToken() is deprecated. Tokens are managed via httpOnly cookies.');
    return null;
  }

  /**
   * @deprecated Tokens are managed via httpOnly cookies by the backend.
   * Token refresh is handled automatically by the backend.
   */
  getRefreshToken(): string | null {
    // Tokens are not accessible from JavaScript (httpOnly cookies)
    console.warn('getRefreshToken() is deprecated. Tokens are managed via httpOnly cookies.');
    return null;
  }

  /**
   * Refreshes the authentication token.
   * Backend handles token rotation via httpOnly cookies.
   *
   * @returns Promise resolving when refresh is complete
   */
  async refreshToken(): Promise<void> {
    // Backend automatically handles token refresh via cookies
    await api.post('/auth/refresh', {});
    // New tokens are set in httpOnly cookies by the backend
  }

  /**
   * Validates current session with backend.
   *
   * @returns Promise<boolean> true if session is valid
   */
  async validateSession(): Promise<boolean> {
    try {
      await api.get('/auth/validate');
      return true;
    } catch {
      this.authenticated = false;
      return false;
    }
  }
}

export default AuthService.getInstance();
