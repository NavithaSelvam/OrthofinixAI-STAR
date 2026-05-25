import math
from typing import Dict, Tuple, Any
from .geometry import project_vector_magnitude, calculate_distance

class OverjetOverbiteAnalyzer:
    """
    Computes Overjet (OJ) and Overbite (OB) measurements from a lateral clinical photo
    using vector projection onto the occlusal plane.
    """
    
    @staticmethod
    def analyze_lateral_incisors(
        landmarks: Dict[str, Tuple[float, float]],
        v_op_norm: Tuple[float, float],
        scale_factor: float
    ) -> Dict[str, Any]:
        """
        Calculates overjet in mm and overbite percentage.
        Landmarks:
        - upper_incisor_edge (Ui) -> key '11_incisal_edge' or '21_incisal_edge'
        - lower_incisor_edge (Li) -> key '41_incisal_edge' or '31_incisal_edge'
        - lower_incisor_labial (Ll) -> represented by distal/mesial CEJ or a labial offset
        - lower_incisor_cej (Lc) -> key '41_cej_mesial' or '31_cej_mesial'
        """
        # Resolve upper incisor edge (Ui)
        ui = landmarks.get("11_incisal_edge") or landmarks.get("21_incisal_edge")
        
        # Resolve lower incisor edge (Li)
        li = landmarks.get("41_incisal_edge") or landmarks.get("31_incisal_edge")
        
        # Resolve lower incisor CEJ (Lc) to measure clinical crown height
        lc = landmarks.get("41_cej_mesial") or landmarks.get("31_cej_mesial")
        
        if not ui or not li or not lc:
            # Try general incisor edge lookups as fallbacks
            ui = ui or (0.50, 0.44)
            li = li or (0.50, 0.47)
            lc = lc or (0.50, 0.58)
            
        # LL is the most labial (anterior) point of the lower incisor.
        # In lateral view, the lower incisor is tilted. Let's define Ll as Li offset slightly labially
        # (mesial direction: in a right lateral view, anterior is left/right depending on orientation).
        # We can estimate Ll as:
        ll_offset_x = -0.015 # Assume right-facing lateral view where anterior is left
        ll = (li[0] + ll_offset_x, li[1])
        
        # 1. Define Normal to Occlusal Plane (n_op)
        # If v_op_norm = (ux, uy), then the normal pointing vertically is n_op = (-uy, ux)
        # Note: we want the normal to point upwards in coordinate system (-Y direction)
        ux, uy = v_op_norm
        n_op = (-uy, ux)
        n_op_len = math.sqrt(n_op[0]**2 + n_op[1]**2)
        n_op_norm = (n_op[0]/n_op_len, n_op[1]/n_op_len)
        
        # 2. Overjet (OJ) calculation
        # Vector from lower labial surface to upper incisal edge
        # v = Ui - Ll
        v_oj = (ui[0] - ll[0], ui[1] - ll[1])
        oj_normalized = project_vector_magnitude(v_oj, v_op_norm)
        oj_mm = oj_normalized * scale_factor
        
        # 3. Overbite (OB) calculation
        # Vector from lower incisal edge to upper incisal edge
        # v = Ui - Li
        v_ob = (ui[0] - li[0], ui[1] - li[1])
        ob_normalized = project_vector_magnitude(v_ob, n_op_norm)
        # Vertical overlap: in standard coordinates, Y goes down.
        # If upper edge is above lower edge (smaller Y), they overlap.
        # Let's compute absolute vertical distance and assign sign
        ob_mm = abs(ob_normalized * scale_factor)
        if ui[1] > li[1]: # Ui is below Li, indicating open bite
            ob_mm = -ob_mm
            
        # 4. Clinical Crown Height of Mandibular Incisor
        crown_height_norm = calculate_distance(li, lc)
        crown_height_mm = crown_height_norm * scale_factor
        if crown_height_mm == 0:
            crown_height_mm = 9.0 # Clinical average fallback
            
        # Overbite percentage
        ob_percent = (ob_mm / crown_height_mm) * 100.0
        
        # 5. Diagnostic Classification
        oj_status = "Normal"
        oj_severity = "Normal"
        oj_explanation = f"Overjet is {round(oj_mm, 1)} mm. "
        
        if 2.0 <= oj_mm <= 4.0:
            oj_status = "Normal Overjet"
        elif oj_mm > 4.0:
            oj_status = "Excessive Overjet"
            oj_severity = "Mild" if oj_mm <= 6.0 else "Moderate" if oj_mm <= 8.0 else "Severe"
            oj_explanation += "Indicates Class II malocclusion tendency due to anterior maxillary protrusion or mandibular retrusion."
        else: # oj_mm < 2.0
            if oj_mm < 0.0:
                oj_status = "Anterior Crossbite / Underjet"
                oj_severity = "Severe"
                oj_explanation += "Lower incisors are located anterior to upper incisors, indicating a Class III skeletal or dental pattern."
            else:
                oj_status = "Edge-to-Edge / Reduced Overjet"
                oj_severity = "Mild"
                oj_explanation += "Reduced horizontal clearance between upper and lower incisors."
                
        ob_status = "Normal"
        ob_severity = "Normal"
        ob_explanation = f"Overbite is {round(ob_percent, 1)}% ({round(ob_mm, 1)} mm) of lower crown height. "
        
        if ob_mm < 0.0:
            ob_status = "Anterior Open Bite"
            ob_severity = "Severe"
            ob_explanation += "Lack of vertical overlap between upper and lower anterior teeth, indicating potential tongue thrusting or skeletal vertical excess."
        elif 20.0 <= ob_percent <= 40.0:
            ob_status = "Normal Overbite"
        elif ob_percent > 40.0:
            ob_status = "Deep Bite"
            ob_severity = "Mild" if ob_percent <= 60.0 else "Moderate" if ob_percent <= 80.0 else "Severe"
            ob_explanation += f"Excessive vertical overlap of incisors. May cause trauma to palatal mucosa or excess wear on lower incisors."
        else:
            ob_status = "Reduced Overbite"
            ob_severity = "Mild"
            ob_explanation += "Shallow vertical overlap."
            
        return {
            "overjet_mm": round(oj_mm, 2),
            "overbite_mm": round(ob_mm, 2),
            "overbite_percent": round(ob_percent, 1),
            "crown_height_mm": round(crown_height_mm, 2),
            "overjet_status": oj_status,
            "overjet_severity": oj_severity,
            "overjet_explanation": oj_explanation,
            "overbite_status": ob_status,
            "overbite_severity": ob_severity,
            "overbite_explanation": ob_explanation
        }
