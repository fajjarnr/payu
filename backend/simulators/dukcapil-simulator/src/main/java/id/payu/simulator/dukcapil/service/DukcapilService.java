package id.payu.simulator.dukcapil.service;

import id.payu.simulator.dukcapil.config.SimulatorConfig;
import id.payu.simulator.dukcapil.dto.*;
import id.payu.simulator.dukcapil.entity.Citizen;
import id.payu.simulator.dukcapil.entity.VerificationLog;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Random;
import java.util.UUID;

/**
 * Service for Dukcapil simulation operations.
 */
@ApplicationScoped
public class DukcapilService {

    private final Random random = new Random();

    @Inject
    SimulatorConfig config;

    /**
     * Verify NIK and compare with provided data.
     */
    @Transactional
    public VerifyNikResponse verifyNik(VerifyNikRequest request) {
        String requestId = generateRequestId();
        Log.infof("[%s] Processing NIK verification for: %s", requestId, maskNik(request.nik()));

        simulateLatency();

        if (shouldSimulateFailure()) {
            logVerification(requestId, request.nik(), 
                VerificationLog.VerificationType.NIK_VERIFICATION,
                VerificationLog.VerificationResult.ERROR, null, "Simulated failure");
            return VerifyNikResponse.error(requestId, "Simulated random failure");
        }

        // Validate NIK format (basic check)
        if (!isValidNikFormat(request.nik())) {
            logVerification(requestId, request.nik(),
                VerificationLog.VerificationType.NIK_VERIFICATION,
                VerificationLog.VerificationResult.FAILED, null, "Invalid NIK format");
            return VerifyNikResponse.invalid(requestId, request.nik());
        }

        Citizen citizen = Citizen.findByNik(request.nik());

        if (citizen == null) {
            logVerification(requestId, request.nik(),
                VerificationLog.VerificationType.NIK_VERIFICATION,
                VerificationLog.VerificationResult.NOT_FOUND, null, "NIK not found");
            return VerifyNikResponse.notFound(requestId, request.nik());
        }

        if (citizen.status == Citizen.CitizenStatus.BLOCKED) {
            logVerification(requestId, request.nik(),
                VerificationLog.VerificationType.NIK_VERIFICATION,
                VerificationLog.VerificationResult.BLOCKED, null, "NIK blocked");
            return VerifyNikResponse.blocked(requestId, citizen);
        }

        if (citizen.status == Citizen.CitizenStatus.INVALID) {
            logVerification(requestId, request.nik(),
                VerificationLog.VerificationType.NIK_VERIFICATION,
                VerificationLog.VerificationResult.FAILED, null, "Invalid NIK");
            return VerifyNikResponse.invalid(requestId, request.nik());
        }

        // Compare names (case-insensitive, normalized)
        boolean nameMatch = normalizeName(request.fullName())
                .equalsIgnoreCase(normalizeName(citizen.fullName));

        logVerification(requestId, request.nik(),
            VerificationLog.VerificationType.NIK_VERIFICATION,
            nameMatch ? VerificationLog.VerificationResult.SUCCESS : VerificationLog.VerificationResult.FAILED,
            null, nameMatch ? "Name matched" : "Name mismatch");

        return VerifyNikResponse.success(requestId, citizen, nameMatch);
    }

    /**
     * Match face between KTP photo and selfie.
     */
    @Transactional
    public FaceMatchResponse matchFace(FaceMatchRequest request) {
        String requestId = generateRequestId();
        Log.infof("[%s] Processing face matching for NIK: %s", requestId, maskNik(request.nik()));

        simulateLatency();

        if (shouldSimulateFailure()) {
            logVerification(requestId, request.nik(),
                VerificationLog.VerificationType.FACE_MATCHING,
                VerificationLog.VerificationResult.ERROR, null, "Simulated failure");
            return FaceMatchResponse.error(requestId, "Simulated random failure");
        }

        Citizen citizen = Citizen.findByNik(request.nik());

        if (citizen == null) {
            logVerification(requestId, request.nik(),
                VerificationLog.VerificationType.FACE_MATCHING,
                VerificationLog.VerificationResult.NOT_FOUND, null, "NIK not found");
            return FaceMatchResponse.nikNotFound(requestId, request.nik());
        }

        if (citizen.status == Citizen.CitizenStatus.BLOCKED) {
            logVerification(requestId, request.nik(),
                VerificationLog.VerificationType.FACE_MATCHING,
                VerificationLog.VerificationResult.BLOCKED, null, "NIK blocked");
            return FaceMatchResponse.blocked(requestId, request.nik());
        }

        // Simulate liveness detection
        boolean livenessDetected = simulateLivenessCheck(request.livenessCheck());
        if (request.livenessCheck() && !livenessDetected) {
            logVerification(requestId, request.nik(),
                VerificationLog.VerificationType.FACE_MATCHING,
                VerificationLog.VerificationResult.FAILED, 0, "Liveness failed");
            return FaceMatchResponse.livenesssFailed(requestId, request.nik());
        }

        // Simulate face matching score
        int matchScore = simulateFaceMatchScore(citizen);
        int threshold = config.faceMatch().threshold();
        boolean matched = matchScore >= threshold;

        logVerification(requestId, request.nik(),
            VerificationLog.VerificationType.FACE_MATCHING,
            matched ? VerificationLog.VerificationResult.SUCCESS : VerificationLog.VerificationResult.FAILED,
            matchScore, String.format("Score: %d, Threshold: %d", matchScore, threshold));

        return FaceMatchResponse.success(requestId, request.nik(), matchScore, threshold, livenessDetected);
    }

    /**
     * Get citizen data by NIK.
     */
    @Transactional
    public CitizenDataResponse getCitizenData(String nik) {
        String requestId = generateRequestId();
        Log.infof("[%s] Retrieving citizen data for NIK: %s", requestId, maskNik(nik));

        simulateLatency();

        if (shouldSimulateFailure()) {
            logVerification(requestId, nik,
                VerificationLog.VerificationType.DATA_RETRIEVAL,
                VerificationLog.VerificationResult.ERROR, null, "Simulated failure");
            return CitizenDataResponse.error(requestId, "Simulated random failure");
        }

        Citizen citizen = Citizen.findByNik(nik);

        if (citizen == null) {
            logVerification(requestId, nik,
                VerificationLog.VerificationType.DATA_RETRIEVAL,
                VerificationLog.VerificationResult.NOT_FOUND, null, "NIK not found");
            return CitizenDataResponse.notFound(requestId, nik);
        }

        if (citizen.status == Citizen.CitizenStatus.BLOCKED) {
            logVerification(requestId, nik,
                VerificationLog.VerificationType.DATA_RETRIEVAL,
                VerificationLog.VerificationResult.BLOCKED, null, "NIK blocked");
            return CitizenDataResponse.blocked(requestId, nik);
        }

        logVerification(requestId, nik,
            VerificationLog.VerificationType.DATA_RETRIEVAL,
            VerificationLog.VerificationResult.SUCCESS, null, "Data retrieved");

        return CitizenDataResponse.fromEntity(requestId, citizen);
    }

    // --- Helper Methods ---

    private String generateRequestId() {
        return "DUK-" + System.currentTimeMillis() + "-" + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String maskNik(String nik) {
        if (nik == null || nik.length() < 16) return "****";
        return nik.substring(0, 4) + "********" + nik.substring(12);
    }

    private boolean isValidNikFormat(String nik) {
        if (nik == null || nik.length() != 16) return false;
        // NIK must be all digits
        return nik.matches("^[0-9]{16}$");
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        // Remove extra spaces, convert to uppercase
        return name.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    private void simulateLatency() {
        int min = config.latency().min();
        int max = config.latency().max();
        int latency = min + random.nextInt(max - min + 1);
        try {
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean shouldSimulateFailure() {
        return random.nextInt(100) < config.failureRate();
    }

    private boolean simulateLivenessCheck(Boolean enabled) {
        if (enabled == null || !enabled) return true;
        // 95% success rate for liveness
        return random.nextInt(100) < 95;
    }

    private int simulateFaceMatchScore(Citizen citizen) {
        // Base score depends on citizen status
        int baseScore = switch (citizen.status) {
            case VALID -> 85;
            case INVALID -> 30;
            default -> 50;
        };
        
        // Add random variance
        int variance = config.faceMatch().variance();
        int randomVariance = random.nextInt(variance * 2 + 1) - variance;
        
        int score = baseScore + randomVariance;
        return Math.max(0, Math.min(100, score)); // Clamp to 0-100
    }

    private void logVerification(String requestId, String nik, 
                                  VerificationLog.VerificationType type,
                                  VerificationLog.VerificationResult result,
                                  Integer matchScore, String details) {
        VerificationLog log = new VerificationLog();
        log.requestId = requestId;
        log.nik = nik;
        log.verificationType = type;
        log.result = result;
        log.matchScore = matchScore;
        log.details = details;
        log.persist();
    }
}
