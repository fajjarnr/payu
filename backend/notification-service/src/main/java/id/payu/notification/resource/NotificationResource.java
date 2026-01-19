package id.payu.notification.resource;

import id.payu.notification.domain.Notification;
import id.payu.notification.dto.NotificationResponse;
import id.payu.notification.dto.SendNotificationRequest;
import id.payu.notification.service.NotificationService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * REST resource for notifications.
 */
@Path("/api/v1/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationResource {

    private static final Logger LOG = Logger.getLogger(NotificationResource.class);

    @Inject
    NotificationService notificationService;

    @POST
    public Response send(@Valid SendNotificationRequest request) {
        LOG.infof("Received notification request: channel=%s, recipient=%s",
                request.channel(), request.recipient());

        Notification notification = notificationService.send(request);
        return Response.status(Response.Status.CREATED)
                .entity(NotificationResponse.from(notification))
                .build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") UUID id) {
        return notificationService.getById(id)
                .map(n -> Response.ok(NotificationResponse.from(n)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Notification not found"))
                        .build());
    }

    @GET
    @Path("/user/{userId}")
    public List<NotificationResponse> getByUser(
            @PathParam("userId") String userId,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        return notificationService.getByUserId(userId, limit)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @POST
    @Path("/{id}/read")
    public Response markAsRead(@PathParam("id") UUID id) {
        notificationService.markAsRead(id);
        return Response.ok().entity(new SuccessResponse("Marked as read")).build();
    }

    record ErrorResponse(String message) {
    }

    record SuccessResponse(String message) {
    }
}
