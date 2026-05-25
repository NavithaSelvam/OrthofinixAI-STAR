from fastapi import APIRouter, Depends, HTTPException
from app.models.schemas import UserInfo
from app.api.dependencies import get_current_user

router = APIRouter()

@router.get("/me", response_model=UserInfo)
def get_me(current_user: UserInfo = Depends(get_current_user)):
    """
    Get the currently logged-in user profile.
    Client should obtain JWT from Firebase Auth and send it in Authorization header.
    """
    return current_user

@router.post("/login")
def login_placeholder():
    """
    In a standard Firebase setup, login happens on the client (Android).
    The client then passes the ID Token to the backend.
    """
    return {"message": "Please use Firebase Client SDK to login and obtain an ID token."}
