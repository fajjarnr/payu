package id.payu.billing.resource;

import id.payu.billing.domain.BillerType;
import id.payu.billing.dto.BillerDto;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * REST resource for biller information.
 */
@Path("/api/v1/billers")
@Produces(MediaType.APPLICATION_JSON)
public class BillerResource {

    @GET
    public List<BillerDto> listBillers(
            @QueryParam("category") String category) {
        
        return Arrays.stream(BillerType.values())
            .filter(type -> category == null || type.getCategory().equalsIgnoreCase(category))
            .map(BillerDto::from)
            .toList();
    }

    @GET
    @Path("/{code}")
    public Response getBiller(@PathParam("code") String code) {
        Optional<BillerType> found = Arrays.stream(BillerType.values())
            .filter(type -> type.getCode().equalsIgnoreCase(code))
            .findFirst();

        return found
            .map(type -> Response.ok(BillerDto.from(type)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Biller not found: " + code))
                .build());
    }

    @GET
    @Path("/categories")
    public List<String> listCategories() {
        return Arrays.stream(BillerType.values())
            .map(BillerType::getCategory)
            .distinct()
            .toList();
    }

    record ErrorResponse(String message) {}
}
