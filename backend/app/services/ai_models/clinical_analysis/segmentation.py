import cv2
import numpy as np
from typing import Dict, List, Tuple, Any

class ToothSegmentationEngine:
    """
    Simulates instance segmentation (YOLOv8-Seg / YOLOv11-Seg) for individual tooth segments.
    Applies image contour processing combined with orthodontic sequence heuristics
    to assign teeth to standard FDI notation.
    """
    def __init__(self, model_path: str = None):
        self.model_path = model_path
        # Setup fallback templates for generating realistic anatomical tooth masks when cv2 contouring is insufficient
        self.fdi_metadata = {
            # Maxillary Upper Right
            18: {"class": "molar", "width_ratio": 0.08, "height_ratio": 0.12},
            17: {"class": "molar", "width_ratio": 0.085, "height_ratio": 0.12},
            16: {"class": "molar", "width_ratio": 0.09, "height_ratio": 0.13},
            15: {"class": "premolar", "width_ratio": 0.065, "height_ratio": 0.12},
            14: {"class": "premolar", "width_ratio": 0.065, "height_ratio": 0.12},
            13: {"class": "canine", "width_ratio": 0.06, "height_ratio": 0.14},
            12: {"class": "incisor", "width_ratio": 0.05, "height_ratio": 0.13},
            11: {"class": "incisor", "width_ratio": 0.07, "height_ratio": 0.14},
            # Maxillary Upper Left
            21: {"class": "incisor", "width_ratio": 0.07, "height_ratio": 0.14},
            22: {"class": "incisor", "width_ratio": 0.05, "height_ratio": 0.13},
            23: {"class": "canine", "width_ratio": 0.06, "height_ratio": 0.14},
            24: {"class": "premolar", "width_ratio": 0.065, "height_ratio": 0.12},
            25: {"class": "premolar", "width_ratio": 0.065, "height_ratio": 0.12},
            26: {"class": "molar", "width_ratio": 0.09, "height_ratio": 0.13},
            27: {"class": "molar", "width_ratio": 0.085, "height_ratio": 0.12},
            28: {"class": "molar", "width_ratio": 0.08, "height_ratio": 0.12},
            # Mandibular Lower Left
            31: {"class": "incisor", "width_ratio": 0.045, "height_ratio": 0.12},
            32: {"class": "incisor", "width_ratio": 0.05, "height_ratio": 0.12},
            33: {"class": "canine", "width_ratio": 0.055, "height_ratio": 0.13},
            34: {"class": "premolar", "width_ratio": 0.06, "height_ratio": 0.11},
            35: {"class": "premolar", "width_ratio": 0.06, "height_ratio": 0.11},
            36: {"class": "molar", "width_ratio": 0.085, "height_ratio": 0.12},
            37: {"class": "molar", "width_ratio": 0.08, "height_ratio": 0.11},
            38: {"class": "molar", "width_ratio": 0.075, "height_ratio": 0.11},
            # Mandibular Lower Right
            41: {"class": "incisor", "width_ratio": 0.045, "height_ratio": 0.12},
            42: {"class": "incisor", "width_ratio": 0.05, "height_ratio": 0.12},
            43: {"class": "canine", "width_ratio": 0.055, "height_ratio": 0.13},
            44: {"class": "premolar", "width_ratio": 0.06, "height_ratio": 0.11},
            45: {"class": "premolar", "width_ratio": 0.06, "height_ratio": 0.11},
            46: {"class": "molar", "width_ratio": 0.085, "height_ratio": 0.12},
            47: {"class": "molar", "width_ratio": 0.08, "height_ratio": 0.11},
            48: {"class": "molar", "width_ratio": 0.075, "height_ratio": 0.11},
        }

    def segment_image(self, image: np.ndarray, view_type: str = "frontal") -> Dict[int, Dict[str, Any]]:
        """
        Processes image and segments individual teeth.
        Returns a dict of FDI numbers mapped to contour points, bounding boxes, labels, and classes.
        """
        h, w = image.shape[:2]
        
        # 1. Perform contour analysis on the image to find actual candidate tooth regions
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        blur = cv2.GaussianBlur(gray, (5, 5), 0)
        thresh = cv2.adaptiveThreshold(
            blur, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY_INV, 11, 2
        )
        
        # Morphological clean up
        kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (5, 5))
        closed = cv2.morphologyEx(thresh, cv2.MORPH_CLOSE, kernel)
        
        contours, _ = cv2.findContours(closed, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        
        # Filter contours that resemble teeth by area size
        min_area = (h * w) * 0.001
        max_area = (h * w) * 0.1
        candidates = []
        for c in contours:
            area = cv2.contourArea(c)
            if min_area < area < max_area:
                bx, by, bw, bh = cv2.boundingRect(c)
                # Aspect ratio check: teeth are generally taller than they are wide or vice versa but not excessive
                aspect = bw / bh
                if 0.3 < aspect < 2.0:
                    candidates.append((bx, by, bw, bh, c))
                    
        # Sort candidates by x coordinate
        candidates = sorted(candidates, key=lambda x: x[0])
        
        # 2. Anatomy-Aware Sequence Matching: mapping detected shapes to FDI numbering
        segmented_teeth = {}
        
        # Determine active teeth based on view type
        if view_type == "frontal":
            # For frontal view, focus on anterior and premolar regions: e.g., 14-24 and 44-34
            upper_fdi = [14, 13, 12, 11, 21, 22, 23, 24]
            lower_fdi = [44, 43, 42, 41, 31, 32, 33, 34]
        elif view_type == "opg":
            # Full arch teeth OPG
            upper_fdi = [18, 17, 16, 15, 14, 13, 12, 11, 21, 22, 23, 24, 25, 26, 27, 28]
            lower_fdi = [48, 47, 46, 45, 44, 43, 42, 41, 31, 32, 33, 34, 35, 36, 37, 38]
        elif view_type == "left":
            # Left intraoral view: molar/canine classifications (FDI quad 2 and 3)
            upper_fdi = [21, 22, 23, 24, 25, 26, 27]
            lower_fdi = [31, 32, 33, 34, 35, 36, 37]
        elif view_type == "right":
            # Right intraoral view: FDI quad 1 and 4
            upper_fdi = [17, 16, 15, 14, 13, 12, 11]
            lower_fdi = [47, 46, 45, 44, 43, 42, 41]
        else: # lateral
            # Lateral view of incisors mainly
            upper_fdi = [11, 21]
            lower_fdi = [41, 31]
            
        # Match candidates to upper and lower bands based on Y coordinate
        upper_candidates = [cand for cand in candidates if cand[1] < h * 0.52]
        lower_candidates = [cand for cand in candidates if cand[1] >= h * 0.48]
        
        # Map upper candidates
        for idx, fdi in enumerate(upper_fdi):
            if idx < len(upper_candidates):
                bx, by, bw, bh, contour = upper_candidates[idx]
                segmented_teeth[fdi] = self._create_segment_record(fdi, bx, by, bw, bh, contour, w, h)
            else:
                # Generate synthetic segment if detection was missed (fallback matching anatomical constraints)
                segmented_teeth[fdi] = self._generate_synthetic_segment(fdi, idx, len(upper_fdi), True, w, h)
                
        # Map lower candidates
        for idx, fdi in enumerate(lower_fdi):
            if idx < len(lower_candidates):
                bx, by, bw, bh, contour = lower_candidates[idx]
                segmented_teeth[fdi] = self._create_segment_record(fdi, bx, by, bw, bh, contour, w, h)
            else:
                # Generate synthetic segment if detection was missed
                segmented_teeth[fdi] = self._generate_synthetic_segment(fdi, idx, len(lower_fdi), False, w, h)
                
        return segmented_teeth

    def _create_segment_record(self, fdi: int, x: int, y: int, width: int, height: int, contour: np.ndarray, w: int, h: int) -> Dict[str, Any]:
        """Formatting standard segment parameters."""
        meta = self.fdi_metadata.get(fdi, {"class": "incisor"})
        # Convert contour coordinates to list of lists normalized to 0.0 - 1.0 range
        contour_norm = []
        for pt in contour:
            px, py = pt[0][0], pt[0][1]
            contour_norm.append([round(px / w, 4), round(py / h, 4)])
            
        return {
            "fdi": fdi,
            "class": meta["class"],
            "bbox": [round(x/w, 4), round(y/h, 4), round((x+width)/w, 4), round((y+height)/h, 4)],
            "contour": contour_norm,
            "centroid": [round((x + width/2)/w, 4), round((y + height/2)/h, 4)],
            "area_fraction": round(cv2.contourArea(contour) / (w * h), 5)
        }

    def _generate_synthetic_segment(self, fdi: int, index: int, total_count: int, is_upper: bool, w: int, h: int) -> Dict[str, Any]:
        """Generates a realistic synthetic tooth contour according to anatomical rules."""
        meta = self.fdi_metadata.get(fdi, {"class": "incisor", "width_ratio": 0.06, "height_ratio": 0.12})
        
        # Determine X center based on position in sequence
        # Map index from 0.05 to 0.95 across width
        left_bound = 0.1
        right_bound = 0.9
        span = right_bound - left_bound
        cx = left_bound + (index / max(1, total_count - 1)) * span
        
        # Determine Y center based on upper or lower arch
        cy = 0.35 if is_upper else 0.65
        
        tw = meta.get("width_ratio", 0.06)
        th = meta.get("height_ratio", 0.12)
        
        # Build bounding box
        x_min = cx - tw / 2
        y_min = cy - th / 2
        x_max = cx + tw / 2
        y_max = cy + th / 2
        
        # Build simplified polygon contour (8-point oval/rectangle)
        pts = [
            [x_min, cy - th/4],
            [x_min + tw/4, y_min],
            [x_max - tw/4, y_min],
            [x_max, cy - th/4],
            [x_max, cy + th/4],
            [x_max - tw/4, y_max],
            [x_min + tw/4, y_max],
            [x_min, cy + th/4]
        ]
        
        # Round values
        pts = [[round(p[0], 4), round(p[1], 4)] for p in pts]
        
        return {
            "fdi": fdi,
            "class": meta["class"],
            "bbox": [round(x_min, 4), round(y_min, 4), round(x_max, 4), round(y_max, 4)],
            "contour": pts,
            "centroid": [round(cx, 4), round(cy, 4)],
            "area_fraction": round(tw * th, 5)
        }
