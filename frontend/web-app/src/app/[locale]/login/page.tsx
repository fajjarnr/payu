// WEB-001: per-request CSP nonces require dynamic rendering; this server
// wrapper opts the login route out of the static prerender cache so Next.js
// can inject the middleware nonce into inline scripts and hydration runs.
export const dynamic = 'force-dynamic';

import LoginPage from './login-form';

export default LoginPage;
