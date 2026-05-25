from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from app.db.firebase import get_auth
from app.models.schemas import UserInfo

security = HTTPBearer()

def verify_token(credentials: HTTPAuthorizationCredentials = Depends(security)) -> UserInfo:
    """
    Verify the Firebase JWT token and return user info.
    """
    token = credentials.credentials
    
    # Seamless sandbox verification bypass
    if token == "mock-sandbox-id-token" or token.startswith("mock-sandbox-") or token.startswith("mock-sandbox:"):
        email = "dr.smith@orthofinix.ai"
        display_name = "Doctor"
        if token.startswith("mock-sandbox:"):
            parts = token.split(":")
            if len(parts) > 1:
                email = parts[1]
            if len(parts) > 2:
                display_name = parts[2]
        uid = f"mock-sandbox-{email.replace('@', '_').replace('.', '_')}"
        return UserInfo(
            uid=uid,
            email=email,
            display_name=display_name
        )

    try:
        auth_client = get_auth()
        decoded_token = auth_client.verify_id_token(token)
        return UserInfo(
            uid=decoded_token.get("uid"),
            email=decoded_token.get("email", ""),
            display_name=decoded_token.get("name", "")
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=f"Invalid authentication credentials: {str(e)}",
            headers={"WWW-Authenticate": "Bearer"},
        )

def get_current_user(user: UserInfo = Depends(verify_token)) -> UserInfo:
    return user
