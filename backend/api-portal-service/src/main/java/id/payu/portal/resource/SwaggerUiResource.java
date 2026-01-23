package id.payu.portal.resource;

import id.payu.portal.dto.ServiceInfo;
import id.payu.portal.dto.ServiceListResponse;
import id.payu.portal.service.ApiPortalService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class SwaggerUiResource {

    @Inject
    ApiPortalService apiPortalService;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response index() {
        ServiceListResponse response = apiPortalService.listServices().await().indefinitely();
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>PayU API Portal</title>\n");
        html.append("    <link rel=\"stylesheet\" type=\"text/css\" href=\"https://unpkg.com/swagger-ui-dist@5.10.5/swagger-ui.css\">\n");
        html.append("    <style>\n");
        html.append("        body { font-family: sans-serif; margin: 0; padding: 0; }\n");
        html.append("        .header { background: #2c3e50; color: white; padding: 1rem 2rem; }\n");
        html.append("        .header h1 { margin: 0; }\n");
        html.append("        .content { padding: 2rem; }\n");
        html.append("        .services-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 1rem; }\n");
        html.append("        .service-card { border: 1px solid #ddd; border-radius: 8px; padding: 1rem; cursor: pointer; transition: transform 0.2s; }\n");
        html.append("        .service-card:hover { transform: translateY(-2px); box-shadow: 0 4px 8px rgba(0,0,0,0.1); }\n");
        html.append("        .service-name { font-size: 1.2rem; font-weight: bold; margin-bottom: 0.5rem; }\n");
        html.append("        .service-status { display: inline-block; padding: 0.25rem 0.5rem; border-radius: 4px; font-size: 0.8rem; }\n");
        html.append("        .status-up { background: #2ecc71; color: white; }\n");
        html.append("        .status-down { background: #e74c3c; color: white; }\n");
        html.append("        .status-unknown { background: #f39c12; color: white; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"header\">\n");
        html.append("        <h1>PayU API Portal</h1>\n");
        html.append("        <p>Centralized API Documentation for All Services</p>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"content\">\n");
        html.append("        <h2>Available Services</h2>\n");
        html.append("        <div class=\"services-grid\">\n");
        
        for (ServiceInfo service : response.services()) {
            html.append("            <a href=\"/service/").append(service.id()).append("\" class=\"service-card\" style=\"text-decoration: none; color: inherit;\">\n");
            html.append("                <div class=\"service-name\">").append(service.name()).append("</div>\n");
            html.append("                <div>\n");
            html.append("                    <span class=\"service-status status-").append(service.status().toLowerCase()).append("\">").append(service.status()).append("</span>\n");
            html.append("                </div>\n");
            html.append("                <div style=\"margin-top: 0.5rem; color: #666;\">\n");
            html.append("                    ").append(service.url()).append("\n");
            html.append("                </div>\n");
            html.append("            </a>\n");
        }
        
        html.append("        </div>\n");
        html.append("        <div style=\"margin-top: 2rem;\">\n");
        html.append("            <h3>Aggregated OpenAPI Specification</h3>\n");
        html.append("            <p>Download all OpenAPI specifications:</p>\n");
        html.append("            <a href=\"/api/v1/portal/openapi\" style=\"display: inline-block; padding: 0.5rem 1rem; background: #3498db; color: white; text-decoration: none; border-radius: 4px;\">\n");
        html.append("                Download OpenAPI JSON\n");
        html.append("            </a>\n");
        html.append("        </div>\n");
        html.append("    </div>\n");
        html.append("</body>\n");
        html.append("</html>");
        
        return Response.ok(html.toString()).build();
    }

    @GET
    @Path("/service/{serviceId}")
    @Produces(MediaType.TEXT_HTML)
    public Response service(String serviceId) {
        ServiceListResponse response = apiPortalService.listServices().await().indefinitely();
        ServiceInfo service = response.services().stream()
            .filter(s -> s.id().equals(serviceId))
            .findFirst()
            .orElse(null);

        String openapiUrl = service != null 
            ? "/api/v1/portal/services/" + serviceId + "/openapi"
            : "";

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>").append(serviceId != null ? serviceId : "Service").append(" - PayU API Portal</title>\n");
        html.append("    <link rel=\"stylesheet\" type=\"text/css\" href=\"https://unpkg.com/swagger-ui-dist@5.10.5/swagger-ui.css\">\n");
        html.append("    <style>\n");
        html.append("        .header { background: #2c3e50; color: white; padding: 1rem 2rem; display: flex; justify-content: space-between; align-items: center; }\n");
        html.append("        .header h1 { margin: 0; }\n");
        html.append("        .back-link { color: white; text-decoration: none; padding: 0.5rem 1rem; background: rgba(255,255,255,0.1); border-radius: 4px; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"header\">\n");
        html.append("        <h1>").append(serviceId != null ? serviceId : "Service").append(" API</h1>\n");
        html.append("        <a href=\"/\" class=\"back-link\">← Back to Portal</a>\n");
        html.append("    </div>\n");
        html.append("    <div id=\"swagger-ui\"></div>\n");
        html.append("    <script src=\"https://unpkg.com/swagger-ui-dist@5.10.5/swagger-ui-bundle.js\"></script>\n");
        html.append("    <script>\n");
        html.append("        window.onload = function() {\n");
        html.append("            const ui = SwaggerUIBundle({\n");
        html.append("                url: '").append(openapiUrl).append("',\n");
        html.append("                dom_id: '#swagger-ui',\n");
        html.append("                deepLinking: true,\n");
        html.append("                presets: [SwaggerUIBundle.presets.apis, SwaggerUIBundle.SwaggerUIStandalonePreset],\n");
        html.append("                layout: \"BaseLayout\",\n");
        html.append("                defaultModelsExpandDepth: 1,\n");
        html.append("                defaultModelExpandDepth: 1,\n");
        html.append("                docExpansion: \"list\",\n");
        html.append("                filter: true,\n");
        html.append("                tryItOutEnabled: true\n");
        html.append("            });\n");
        html.append("        }\n");
        html.append("    </script>\n");
        html.append("</body>\n");
        html.append("</html>");
        
        return Response.ok(html.toString()).build();
    }
}
