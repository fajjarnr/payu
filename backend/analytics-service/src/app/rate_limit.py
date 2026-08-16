from slowapi import Limiter
from slowapi.util import get_remote_address

# ANA-RATE-001: single shared limiter instance (module-level) so health
# endpoints (main.py) and the fraud endpoint (analytics.py) use the same
# in-memory state — mirror of the kyc-service pattern.
limiter = Limiter(key_func=get_remote_address)