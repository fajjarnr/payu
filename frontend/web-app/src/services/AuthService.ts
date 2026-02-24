import type { User } from '@/types';

export interface LoginRequest {
  username: string;
  password: string;
}

/**
 * BFF login response — tokens are in httpOnly cookies, never in the payload.
 */
export interface LoginResponse {
  success: boolean;
  data: {
    user: User;
    /** Seconds until the accessToken cookie expires — safe to expose, not the token itself */
    expiresIn?: number;
  };
}

interface UserSession {
  user: User;
  accountId: string;
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
  private userSession: UserSession | null = null;

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
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(credentials),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Login failed');
    }

    const data: LoginResponse = await res.json();
    this.authenticated = true;
    return data;
  }

  /**
   * Logs out the user by clearing the session.
   * Backend will clear httpOnly cookies.
   */
  async logout(): Promise<void> {
    await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' })
      .catch(() => { /* best-effort */ });
    this.authenticated = false;
    this.userSession = null;
  }

  /**
   * Sets the current user session data (non-sensitive data only).
   * Tokens are NOT stored here - they're in httpOnly cookies.
   *
   * @param user User profile data
   * @param accountId Account identifier
   */
  setUserSession(user: User, accountId: string): void {
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
   * @returns Promise resolving with expiresIn (seconds) for the new access token
   */
  async refreshToken(): Promise<{ expiresIn?: number }> {
    const res = await fetch('/api/auth/refresh', {
      method: 'POST',
      credentials: 'include',
    });
    if (!res.ok) throw new Error('Token refresh failed');
    return res.json();
  }

  /**
   * Validates current session with backend.
   *
   * @returns Promise<boolean> true if session is valid
   */
  async validateSession(): Promise<boolean> {
    try {
      const { default: api } = await import('@/lib/api');
      await api.get('/auth/validate');
      return true;
    } catch {
      this.authenticated = false;
      return false;
    }
  }

  // === Biometric Authentication (FE-GAP-011) ===

  /** GET /biometric/challenge — Get biometric challenge */
  async getBiometricChallenge(): Promise<BiometricChallenge> {
    const { default: api } = await import('@/lib/api');
    const response = await api.get('/biometric/challenge');
    return response.data;
  }

  /** POST /biometric/register — Register biometric credential */
  async registerBiometric(request: BiometricRegistration): Promise<BiometricRegistrationResult> {
    const { default: api } = await import('@/lib/api');
    const response = await api.post('/biometric/register', request);
    return response.data;
  }

  /** POST /biometric/authenticate — Authenticate with biometric */
  async authenticateBiometric(request: BiometricAuthRequest): Promise<{ success: boolean }> {
    const { default: api } = await import('@/lib/api');
    const response = await api.post('/biometric/authenticate', request);
    return response.data;
  }

  /** GET /biometric/registrations/{username} — Get user's biometric registrations */
  async getBiometricRegistrations(username: string): Promise<BiometricRegistrationInfo[]> {
    const { default: api } = await import('@/lib/api');
    const response = await api.get(`/biometric/registrations/${username}`);
    return response.data;
  }

  /** DELETE /biometric/registrations/{registrationId} — Revoke biometric registration */
  async revokeBiometricRegistration(registrationId: string): Promise<void> {
    const { default: api } = await import('@/lib/api');
    await api.delete(`/biometric/registrations/${registrationId}`);
  }
}

// === Biometric Types ===

export interface BiometricChallenge {
  challengeId: string;
  challenge: string;
  timeout: number;
  rpId: string;
}

export interface BiometricRegistration {
  username: string;
  challengeId: string;
  credential: string; // base64 encoded attestation
  deviceName: string;
}

export interface BiometricRegistrationResult {
  registrationId: string;
  username: string;
  deviceName: string;
  createdAt: string;
}

export interface BiometricAuthRequest {
  username: string;
  challengeId: string;
  credential: string; // base64 encoded assertion
}

export interface BiometricRegistrationInfo {
  registrationId: string;
  username: string;
  deviceName: string;
  lastUsedAt: string;
  createdAt: string;
}

export default AuthService.getInstance();
