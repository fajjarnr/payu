package id.payu.partner.resource;

import id.payu.partner.dto.PartnerDTO;
import id.payu.partner.service.PartnerService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/partners")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PartnerResource {

    @Inject
    PartnerService partnerService;

    @GET
    public List<PartnerDTO> getAllPartners() {
        return partnerService.getAllPartners();
    }

    @GET
    @Path("/{id}")
    public Response getPartnerById(@PathParam("id") Long id) {
        PartnerDTO partner = partnerService.getPartnerById(id);
        if (partner == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(partner).build();
    }

    @POST
    public Response createPartner(@Valid PartnerDTO partnerDTO) {
        try {
            PartnerDTO createdPartner = partnerService.createPartner(partnerDTO);
            return Response.status(Response.Status.CREATED).entity(createdPartner).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response updatePartner(@PathParam("id") Long id, @Valid PartnerDTO partnerDTO) {
        PartnerDTO updatedPartner = partnerService.updatePartner(id, partnerDTO);
        if (updatedPartner == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(updatedPartner).build();
    }

    @POST
    @Path("/{id}/keys/regenerate")
    public Response regenerateKeys(@PathParam("id") Long id) {
        PartnerDTO partner = partnerService.regenerateKeys(id);
        if (partner == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(partner).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deletePartner(@PathParam("id") Long id) {
        boolean deleted = partnerService.deletePartner(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}
