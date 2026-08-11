package id.payu.notification.adapter.web;

import id.payu.notification.domain.Notification;
import id.payu.notification.domain.RecipientMasker;
import id.payu.notification.dto.NotificationResponse;
import id.payu.notification.dto.SendNotificationRequest;
import id.payu.notification.application.service.NotificationService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * REST resource for notifications.
 *
 * <p>Provides endpoints for sending and managing notifications across multiple channels
 * including Push, SMS, Email, and In-App notifications.</p>
 */
@Path("/api/v1/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Notifications", description = "NotificationEntity management APIs for sending and tracking notifications via Push, SMS, Email, and In-App channels")
@SecurityRequirement(name = "bearerAuth")
// BUG-SECURITY-029 FIX: Per-endpoint ownership check required
    @Authenticated
public class NotificationResource {

    private static final Logger LOG = Logger.getLogger(NotificationResource.class);

    @Inject
    NotificationService notificationService;

    /**
     * Send a notification to a user.
     *
     * <p>Supports multiple channels: PUSH, SMS, EMAIL, IN_APP.
     * Notifications are queued for asynchronous delivery with automatic retry on failure.</p>
     *
     * @param request the notification request containing channel, recipient, and content
     * @return the created notification with tracking ID
     */
    @POST
    @Operation(
        summary = "Send notification",
        description = """
            Sends a notification to a user through the specified channel.

            **Supported Channels:**
            - `PUSH`: Mobile push notification (requires device token)
            - `SMS`: SMS message (requires phone number)
            - `EMAIL`: Email message (requires email address)
            - `IN_APP`: In-app notification (stored in database)

            **Templates:**
            Optionally use a pre-configured template by providing `templateId` and `data`.
            The data should be a JSON string with template variables.

            **Rate Limiting:** 100 requests per minute per user

            **Delivery:**
            Notifications are delivered asynchronously. Check status using the returned notification ID.
            """
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "201",
            description = "NotificationEntity created and queued for delivery",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = NotificationResponse.class)
            ),
            headers = {
                @Header(
                    name = "Location",
                    description = "URL to retrieve the notification status",
                    schema = @Schema(type = SchemaType.STRING)
                )
            }
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request - missing required fields or invalid channel",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "401",
            description = "Authentication required - missing or invalid JWT token"
        ),
        @APIResponse(
            responseCode = "403",
            description = "Forbidden - insufficient permissions to send notifications"
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error - failed to queue notification",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @RequestBody(
        description = "NotificationEntity request with channel, recipient, and content",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = SendNotificationRequest.class)
        ),
        required = true
    )
    public Response send(@Valid SendNotificationRequest request,
                         @HeaderParam("X-Idempotency-Key") String idempotencyKey) {
        LOG.infof("Received notification request: channel=%s, recipient=%s",
                request.channel(), RecipientMasker.mask(request.recipient()));

        Notification notification = notificationService.send(request, idempotencyKey);
        return Response.status(Response.Status.CREATED)
                .entity(NotificationResponse.from(notification))
                .build();
    }

    /**
     * List all recent notifications (paginated).
     *
     * @param limit maximum number of notifications to return (default: 20, max: 100)
     * @return list of recent notifications
     */
    @GET
    @Operation(
        summary = "List all recent notifications",
        description = """
            Retrieves all recent notifications across users, ordered by creation date
            (newest first). Supports pagination via the limit parameter.
            """
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "List of notifications retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    type = SchemaType.ARRAY,
                    implementation = NotificationResponse.class
                )
            )
        ),
        @APIResponse(
            responseCode = "401",
            description = "Authentication required"
        )
    })
    public List<NotificationResponse> listAll(
        @Parameter(
            description = "Maximum number of notifications to return (default: 20, max: 100)",
            schema = @Schema(minimum = "1", maximum = "100", defaultValue = "20"),
            example = "20"
        )
        @QueryParam("limit") @DefaultValue("20") int limit) {
        return notificationService.getAllNotifications(limit)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    /**
     * Get notification details by ID.
     *
     * @param id the notification UUID
     * @return the notification details or 404 if not found
     */
    @GET
    @Path("/{id}")
    @Operation(
        summary = "Get notification by ID",
        description = """
            Retrieves detailed information about a specific notification including
            delivery status, timestamps, and failure reasons if applicable.

            **NotificationEntity Statuses:**
            - `PENDING`: Queued for delivery
            - `SENDING`: Currently being sent
            - `SENT`: Successfully sent to provider
            - `DELIVERED`: Confirmed delivery to recipient
            - `READ`: Read by the recipient (in-app only)
            - `FAILED`: Delivery failed (check failureReason)
            """
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "NotificationEntity found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = NotificationResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "401",
            description = "Authentication required"
        ),
        @APIResponse(
            responseCode = "403",
            description = "Forbidden - not authorized to access this notification"
        ),
        @APIResponse(
            responseCode = "404",
            description = "NotificationEntity not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public Response getById(
        @Parameter(
            description = "NotificationEntity UUID",
            required = true,
            example = "123e4567-e89b-12d3-a456-426614174000"
        )
        @PathParam("id") UUID id) {
        return notificationService.getById(id)
                .map(n -> Response.ok(NotificationResponse.from(n)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("NotificationEntity not found"))
                        .build());
    }

    /**
     * Get all notifications for a user.
     *
     * @param userId the user ID to fetch notifications for
     * @param limit maximum number of notifications to return (default: 20)
     * @return list of notifications for the user
     */
    @GET
    @Path("/user/{userId}")
    @Operation(
        summary = "Get notifications for a user",
        description = """
            Retrieves all notifications for a specific user, ordered by creation date
            (newest first). Useful for displaying notification history in a UI.

            **Pagination:**
            Use the `limit` parameter to control the number of results.
            Default is 20, maximum is 100.
            """
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "List of notifications retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    type = SchemaType.ARRAY,
                    implementation = NotificationResponse.class
                )
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid limit parameter (must be between 1 and 100)"
        ),
        @APIResponse(
            responseCode = "401",
            description = "Authentication required"
        ),
        @APIResponse(
            responseCode = "403",
            description = "Forbidden - not authorized to access this user's notifications"
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public List<NotificationResponse> getByUser(
        @Parameter(
            description = "User ID to fetch notifications for",
            required = true,
            example = "user-123"
        )
        @PathParam("userId") String userId,
        @Parameter(
            description = "Maximum number of notifications to return (default: 20, max: 100)",
            schema = @Schema(minimum = "1", maximum = "100", defaultValue = "20"),
            example = "20"
        )
        @QueryParam("limit") @DefaultValue("20") int limit) {
        return notificationService.getByUserId(userId, limit)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    /**
     * Mark a notification as read.
     *
     * <p>Only applicable to IN_APP notifications. Other channels ignore this operation.</p>
     *
     * @param id the notification UUID to mark as read
     * @return success confirmation
     */
    @POST
    @Path("/{id}/read")
    @Operation(
        summary = "Mark notification as read",
        description = """
            Marks a notification as read. Only applicable to IN_APP notifications.

            **Behavior:**
            - For IN_APP notifications: Updates the `readAt` timestamp
            - For other channels: No-op (operation succeeds but does nothing)

            **Idempotency:**
            This operation is idempotent. Marking an already-read notification
            will return success without errors.
            """
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "NotificationEntity marked as read successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = SuccessResponse.class),
                example = "{\"message\": \"Marked as read\"}"
            )
        ),
        @APIResponse(
            responseCode = "401",
            description = "Authentication required"
        ),
        @APIResponse(
            responseCode = "403",
            description = "Forbidden - not authorized to modify this notification"
        ),
        @APIResponse(
            responseCode = "404",
            description = "NotificationEntity not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public Response markAsRead(
        @Parameter(
            description = "NotificationEntity UUID to mark as read",
            required = true,
            example = "123e4567-e89b-12d3-a456-426614174000"
        )
        @PathParam("id") UUID id) {
        notificationService.markAsRead(id);
        return Response.ok().entity(new SuccessResponse("Marked as read")).build();
    }

    /**
     * Error response schema.
     */
    @Schema(description = "Error response returned when a request fails")
    record ErrorResponse(
        @Schema(
            description = "Human-readable error message",
            example = "NotificationEntity not found"
        )
        String message
    ) {}

    /**
     * Success response schema for simple operations.
     */
    @Schema(description = "Success response for operations that don't return data")
    record SuccessResponse(
        @Schema(
            description = "Success message",
            example = "Marked as read"
        )
        String message
    ) {}
}
