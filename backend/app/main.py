from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.config import settings
from app.db.firebase import init_firebase
from app.db.sqlalchemy_db import init_sqlalchemy
from app.api.routes import auth, patients, cases, ai, analysis
from app.api.routes import summit_auth, summit_analysis

init_firebase()
init_sqlalchemy()

app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    description="Orthodontic AI Analysis Backend API",
)

# CORS Configuration for frontend (if any web client is used)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], # Update for production security
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

from fastapi.staticfiles import StaticFiles

# Include Routers
app.include_router(auth.router, prefix="/api/auth", tags=["Authentication"])
app.include_router(patients.router, prefix="/api/patients", tags=["Patients"])
app.include_router(cases.router, prefix="/api/cases", tags=["Cases"])
app.include_router(ai.router, prefix="/api/ai", tags=["AI Analysis"])
app.include_router(analysis.router, tags=["Legacy Analysis"])
app.include_router(summit_auth.router, prefix="/auth", tags=["Summit Auth"])
app.include_router(summit_analysis.router, prefix="/analysis", tags=["Summit Analysis"])

# Mount static folder for persistent clinical image retrieval
app.mount("/uploads", StaticFiles(directory="uploads"), name="uploads")

@app.get("/")
def root():
    return {"message": "Welcome to the OrthofinixAi Backend", "status": "active"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
