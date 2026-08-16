from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose.exceptions import JWTError

from app.jwt_auth import verify_jwt

_bearer_scheme = HTTPBearer(auto_error=False)


async def require_auth(
    credentials: HTTPAuthorizationCredentials = Depends(_bearer_scheme),
) -> dict:
    """
    AI-AUTH-001: cryptographically verify the JWT signature against Keycloak
    JWKS before trusting any claim. Base64-decoding a payload is NOT auth.
    """
    if credentials is None:
        raise HTTPException(status_code=401, detail="Authentication required")

    try:
        return await verify_jwt(credentials.credentials)
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid token")
    except Exception:
        raise HTTPException(status_code=401, detail="Invalid token")