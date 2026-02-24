package id.payu.statement.adapter.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * S3-compatible storage adapter for statement PDFs.
 *
 * BUG-BE-050 Fix: Replaces local /tmp storage with S3 bucket.
 * Supports both AWS S3 and MinIO (S3-compatible) via endpoint override.
 *
 * Configuration:
 *   statement.storage.type=s3          (default, vs "local" for dev)
 *   statement.storage.s3.bucket=payu-statements
 *   statement.storage.s3.region=ap-southeast-1
 *   statement.storage.s3.endpoint=     (optional, for MinIO)
 *   statement.storage.s3.access-key=   (for MinIO; AWS uses IAM roles)
 *   statement.storage.s3.secret-key=   (for MinIO; AWS uses IAM roles)
 */
@Slf4j
@Component
public class S3StorageAdapter {

    @Value("${statement.storage.type:s3}")
    private String storageType;

    @Value("${statement.storage.s3.bucket:payu-statements}")
    private String bucketName;

    @Value("${statement.storage.s3.region:ap-southeast-1}")
    private String region;

    @Value("${statement.storage.s3.endpoint:}")
    private String endpoint;

    @Value("${statement.storage.s3.access-key:}")
    private String accessKey;

    @Value("${statement.storage.s3.secret-key:}")
    private String secretKey;

    @Value("${statement.storage.s3.prefix:statements/}")
    private String keyPrefix;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        if (!"s3".equals(storageType)) {
            log.info("S3 storage disabled (storage.type={}). Using local fallback.", storageType);
            return;
        }

        var builder = S3Client.builder().region(Region.of(region));

        // Support MinIO or custom S3-compatible endpoint
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                   .forcePathStyle(true); // Required for MinIO
            log.info("S3 client configured with custom endpoint: {}", endpoint);
        }

        // Use explicit credentials for MinIO, or rely on IAM for AWS
        if (accessKey != null && !accessKey.isBlank()) {
            builder.credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
                )
            );
        }

        this.s3Client = builder.build();
        log.info("S3 storage initialized: bucket={}, region={}, prefix={}", bucketName, region, keyPrefix);
    }

    /**
     * Upload PDF bytes to S3 and return the S3 key.
     *
     * @param statementId unique statement ID
     * @param pdfBytes    PDF file content
     * @return S3 object key (e.g., "statements/statement_uuid.pdf")
     */
    public String uploadPdf(UUID statementId, byte[] pdfBytes) {
        String objectKey = keyPrefix + "statement_" + statementId + ".pdf";

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType("application/pdf")
                .contentLength((long) pdfBytes.length)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(pdfBytes));

        log.info("PDF uploaded to S3: bucket={}, key={}, size={}",
                bucketName, objectKey, pdfBytes.length);

        return "s3://" + bucketName + "/" + objectKey;
    }

    /**
     * Download PDF bytes from S3.
     *
     * @param s3Path S3 path (s3://bucket/key or just key)
     * @return PDF file content
     */
    public byte[] downloadPdf(String s3Path) throws IOException {
        String objectKey = extractObjectKey(s3Path);

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        byte[] bytes = s3Client.getObject(getRequest).readAllBytes();
        log.info("PDF downloaded from S3: key={}, size={}", objectKey, bytes.length);
        return bytes;
    }

    /**
     * Check if S3 storage is enabled.
     */
    public boolean isEnabled() {
        return "s3".equals(storageType) && s3Client != null;
    }

    /**
     * Extract object key from S3 path format.
     */
    private String extractObjectKey(String s3Path) {
        if (s3Path.startsWith("s3://")) {
            // s3://bucket/key → extract key part
            String withoutScheme = s3Path.substring(5); // remove "s3://"
            int slashIndex = withoutScheme.indexOf('/');
            return slashIndex >= 0 ? withoutScheme.substring(slashIndex + 1) : withoutScheme;
        }
        return s3Path;
    }
}
