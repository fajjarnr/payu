package id.payu.billing.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.math.BigDecimal;

/**
 * REST client for wallet-service.
 */
@Path("/api/v1/wallets")
@RegisterRestClient(configKey = "wallet-service")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface WalletClient {

    @POST
    @Path("/{accountId}/reserve")
    ReserveResponse reserveBalance(
        @PathParam("accountId") String accountId,
        ReserveRequest request
    );

    @POST
    @Path("/reservations/{reservationId}/commit")
    void commitReservation(@PathParam("reservationId") String reservationId);

    @POST
    @Path("/reservations/{reservationId}/release")
    void releaseReservation(@PathParam("reservationId") String reservationId);

    record ReserveRequest(BigDecimal amount, String referenceId) {}

    record ReserveResponse(String reservationId, String accountId, String referenceId, String status) {}
}
