from slowapi import Limiter
from slowapi.util import get_remote_address

# Single shared limiter instance (module-level) so the same in-memory state
# backs the health endpoints (main.py) and the KYC upload endpoints (kyc.py).
limiter = Limiter(key_func=get_remote_address)
