package id.payu.gateway.filter;

import id.payu.gateway.config.GatewayConfig;
import io.quarkus.logging.Log;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

/**
 * Filter to compress response bodies using gzip.
 * Checks client's Accept-Encoding header and compresses if supported.
 */
@Provider
@ApplicationScoped
@Priority(Priorities.ENTITY_CODER)
public class ResponseCompressionFilter implements ContainerResponseFilter {

    private static final String GZIP = "gzip";
    private static final Set<String> COMPRESSIBLE_TYPES = Set.of(
        "application/json",
        "application/xml",
        "text/html",
        "text/plain",
        "text/css",
        "text/javascript",
        "application/javascript"
    );

    @Inject
    GatewayConfig config;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (!config.compression().enabled()) {
            return;
        }

        // Skip if response already has encoding
        if (responseContext.getHeaders().containsKey(HttpHeaders.CONTENT_ENCODING)) {
            return;
        }

        // Check if client accepts gzip
        String acceptEncoding = requestContext.getHeaderString(HttpHeaders.ACCEPT_ENCODING);
        if (acceptEncoding == null || !acceptEncoding.contains(GZIP)) {
            return;
        }

        // Check content type
        String contentType = responseContext.getMediaType() != null ? responseContext.getMediaType().toString() : "";
        if (!isCompressibleType(contentType)) {
            return;
        }

        // Check response size
        Object entity = responseContext.getEntity();
        if (entity == null) {
            return;
        }

        byte[] responseBytes;
        if (entity instanceof String) {
            responseBytes = ((String) entity).getBytes();
        } else if (entity instanceof byte[]) {
            responseBytes = (byte[]) entity;
        } else {
            // For other types, skip compression
            return;
        }

        // Only compress if size exceeds minimum threshold
        if (responseBytes.length < config.compression().minSize()) {
            return;
        }

        // Compress response
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {

            gzipStream.write(responseBytes);
            gzipStream.finish();

            byte[] compressedBytes = byteStream.toByteArray();

            // Only use compressed version if it's actually smaller
            if (compressedBytes.length < responseBytes.length) {
                responseContext.setEntity(compressedBytes);
                responseContext.getHeaders().putSingle(HttpHeaders.CONTENT_ENCODING, GZIP);
                responseContext.getHeaders().putSingle(HttpHeaders.CONTENT_LENGTH, compressedBytes.length);

                double reduction = (1 - (double) compressedBytes.length / responseBytes.length) * 100;
                Log.debug(String.format("Compressed response: %d -> %d bytes (%.1f%% reduction)",
                    responseBytes.length, compressedBytes.length, reduction));
            }
        } catch (Exception e) {
            Log.warnf(e, "Compression failed, sending uncompressed response");
        }
    }

    private boolean isCompressibleType(String contentType) {
        return COMPRESSIBLE_TYPES.stream()
            .anyMatch(contentType::startsWith);
    }
}
