// WEB-003: the form only renders after hydration; per-request CSP nonces need
// dynamic rendering, so opt this public route out of the static cache.
export const dynamic = 'force-dynamic';

import ForgotPasswordPage from './forgot-password-form';

export default ForgotPasswordPage;
