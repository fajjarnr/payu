from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.config import get_settings

_bearer_scheme = HTTPBearer(auto_error=False)
_settings = get_settings()


async def require_auth(
    credentials: HTTPAuthorizationCredentials = Depends(_bearer_scheme),
) -> dict:
    import jwt

    if credentials is None:
        raise HTTPException(status_code=401, detail="Authentication required")

    try:
        return jwt.decode(
            credentials.credentials,
            _settings.secret_key,
            algorithms=[_settings.algorithm],
            options={"verify_exp": True},
        )
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token expired")
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=401, detail="Invalid token")