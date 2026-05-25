from typing import List

class ClinicalRecommendationEngine:
    """
    Rule-based recommendation engine that intakes scores from ABO, Andrews Keys, 
    and other clinical analysis modules to formulate a treatment plan.
    """
    
    @staticmethod
    def generate_recommendations(abo_data: dict, andrews_data: dict) -> List[str]:
        recommendations = []
        
        # 1. Finishing Focus: ABO OGS Criteria
        abo_score = abo_data.get("score", 0)
        if abo_score > 30:
            recommendations.append("Major detailing required. Verify marginal ridge leveling and buccolingual inclination on molars.")
        elif abo_score > 15:
            recommendations.append("Moderate detailing. Refine interproximal contacts and check for overjet discrepancies.")
        else:
            recommendations.append("Alignment meets clinical standards. Schedule debonding after final functional verification.")

        # 2. Andrews Six Keys for Finishing
        keys = andrews_data.get("details", [])
        for key_data in keys:
            if key_data["score"] < 0.8:
                if "Molar" in key_data["key"]:
                    recommendations.append("Key 1 Deviation: Adjust molar relationship for tight Class I finishing.")
                elif "Rotations" in key_data["key"]:
                    recommendations.append("Key 4 Deviation: Correct residual rotations using artistic bends in .019x.025 SS wire.")
                
        # 3. Clinical Mechanics
        recommendations.append("Recommend 'Settling' elastics (Box/Triangle) for 2 weeks prior to appliance removal.")

        return recommendations
