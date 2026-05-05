CREATE ALIAS IF NOT EXISTS pg_advisory_xact_lock AS '
void pgAdvisoryXactLock(long id) {}
';

CREATE ALIAS IF NOT EXISTS hashtext AS '
int hashText(String s) { return s == null ? 0 : s.hashCode(); }
';
