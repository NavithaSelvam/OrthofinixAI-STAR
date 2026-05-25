from fastapi import APIRouter, UploadFile, File, HTTPException, Depends
from typing import List
from app.models.schemas import PredictionResponse, AnalysisRecordCreate, AnalysisRecordResponse
from app.services.ai_engine import ai_engine
from app.db.firebase import save_analysis_record, get_analysis_history, get_analysis_by_id
import uuid
import os

router = APIRouter(prefix="/analysis")

# Ensure uploads directory exists
UPLOAD_DIR = "uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

@router.post("/predict", response_model=PredictionResponse)
async def predict_image(file: UploadFile = File(...)):
    """
    Accepts an image upload and returns AI prediction, confidence score, and recommendations.
    """
    if not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="File must be an image.")
        
    try:
        # Read the image bytes
        image_bytes = await file.read()
        
        # Save the file locally to get a URL/path
        file_extension = file.filename.split(".")[-1] if "." in file.filename else "jpg"
        unique_filename = f"{uuid.uuid4()}.{file_extension}"
        file_path = os.path.join(UPLOAD_DIR, unique_filename)
        
        with open(file_path, "wb") as buffer:
            buffer.write(image_bytes)
            
        # Process the image with the AI engine
        result = ai_engine.analyze_image(image_bytes)
        
        # We can also return the image_url in a real scenario by modifying PredictionResponse
        # But per schema, PredictionResponse has prediction, confidence_score, recommendations.
        # We will append the local file path to details so the client can use it for /save-analysis
        if "details" not in result:
            result["details"] = {}
        result["details"]["image_url"] = f"/uploads/{unique_filename}"
        
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/save-analysis", response_model=AnalysisRecordResponse)
async def save_analysis(record: AnalysisRecordCreate):
    """
    Saves the complete analysis record (patient info + AI prediction + image URL) to Firestore.
    """
    try:
        saved_record = save_analysis_record(record.dict())
        return saved_record
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to save record: {str(e)}")

@router.get("/history", response_model=List[AnalysisRecordResponse])
async def get_history():
    """
    Retrieves all past analysis records from Firestore.
    """
    try:
        history = get_analysis_history()
        return history
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch history: {str(e)}")

@router.get("/{record_id}", response_model=AnalysisRecordResponse)
async def get_analysis(record_id: str):
    """
    Retrieves a specific analysis record by its ID.
    """
    try:
        record = get_analysis_by_id(record_id)
        if not record:
            raise HTTPException(status_code=404, detail="Analysis record not found.")
        return record
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch record: {str(e)}")
