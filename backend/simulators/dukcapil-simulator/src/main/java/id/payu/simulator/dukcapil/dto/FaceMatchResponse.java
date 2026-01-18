package id.payu.simulator.dukcapil.dto;

/**
 * Response DTO for face matching.
 */
public record FaceMatchResponse(
    String requestId,
    String nik,
    boolean matched,
    int matchScore,
    int threshold,
    boolean livenessDetected,
    String status,
    String responseCode,
    String responseMessage
) {
    public static FaceMatchResponse success(String requestId, String nik, int score, int threshold, boolean liveness) {
        boolean matched = score >= threshold;
        return new FaceMatchResponse(
            requestId,
            nik,
            matched,
            score,
            threshold,
            liveness,
            matched ? "MATCHED" : "NOT_MATCHED",
            matched ? "00" : "51",
            matched 
                ? String.format("Face matched with %d%% confidence", score)
                : String.format("Face not matched. Score: %d%%, Threshold: %d%%", score, threshold)
        );
    }

    public static FaceMatchResponse livenesssFailed(String requestId, String nik) {
        return new FaceMatchResponse(
            requestId,
            nik,
            false,
            0,
            0,
            false,
            "LIVENESS_FAILED",
            "52",
            "Liveness detection failed - possible spoofing attempt"
        );
    }

    public static FaceMatchResponse nikNotFound(String requestId, String nik) {
        return new FaceMatchResponse(
            requestId,
            nik,
            false,
            0,
            0,
            false,
            "NOT_FOUND",
            "14",
            "NIK not found in database"
        );
    }

    public static FaceMatchResponse blocked(String requestId, String nik) {
        return new FaceMatchResponse(
            requestId,
            nik,
            false,
            0,
            0,
            false,
            "BLOCKED",
            "62",
            "NIK is blocked or flagged"
        );
    }

    public static FaceMatchResponse error(String requestId, String message) {
        return new FaceMatchResponse(
            requestId,
            null,
            false,
            0,
            0,
            false,
            "ERROR",
            "96",
            message
        );
    }
}
