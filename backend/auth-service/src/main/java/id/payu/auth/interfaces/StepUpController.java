package id.payu.auth.interfaces;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import java.util.*;

// ponytail: minimal Step-Up 2/4 per ADR-0028 — Redis TTL 180s + payload_digest SHA256, full 2-phase prepare/execute in transaction-service next
@Path("/internal/v1/auth/step-up")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StepUpController {

    private static final Logger LOG = Logger.getLogger(StepUpController.class);

    // in-memory stub for challenge store (ponytail: replace with Redis 180s via quarkus-redis)
    private static final Map<String, String> CHALLENGES = new java.util.concurrent.ConcurrentHashMap<>();

    public static record ChallengeRequest(String userId, String payloadDigest) {}
    public static record ChallengeResponse(String challengeId, long ttlSeconds) {}
    public static record VerifyRequest(String challengeId, String pin, String payloadDigest) {}
    public static record VerifyResponse(boolean verified) {}

    @POST
    @Path("/challenge")
    public Response challenge(ChallengeRequest req) {
        if (req.userId() == null || req.payloadDigest() == null) return Response.status(400).build();
        String id = UUID.randomUUID().toString();
        CHALLENGES.put(id, req.payloadDigest());
        LOG.infof("StepUp challenge %s for user %s", id, req.userId());
        return Response.ok(new ChallengeResponse(id, 180)).build();
    }

    @POST
    @Path("/verify")
    public Response verify(VerifyRequest req) {
        String expected = CHALLENGES.get(req.challengeId());
        if (expected == null) return Response.status(404).entity(new VerifyResponse(false)).build();
        boolean ok = expected.equals(req.payloadDigest());
        if (ok) CHALLENGES.remove(req.challengeId());
        LOG.infof("StepUp verify %s %s", req.challengeId(), ok ? "ok" : "fail");
        return Response.ok(new VerifyResponse(ok)).build();
    }
}
