package id.payu.portal.architecture;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Architecture conformance tests for the API Portal Service.
 *
 * Validates hexagonal architecture layering without relying on ArchUnit
 * (which requires Spring Boot test infra).
 */
@QuarkusTest
@DisplayName("Architecture Conformance Tests")
@Tag("architecture")
class ArchitectureTest {

    // ────────── Package Structure ──────────

    @Test
    @DisplayName("should follow Hexagonal Architecture package conventions")
    void testPackageStructure() {
        String basePackage = "src/main/java/id/payu/portal";

        assertTrue(pathExists(basePackage + "/adapter/web"),
            "Missing adapter/web package (inbound adapters)");
        assertTrue(pathExists(basePackage + "/application/service"),
            "Missing application/service package (use cases)");
        assertTrue(pathExists(basePackage + "/dto"),
            "Missing dto package (data transfer objects)");
        assertTrue(pathExists(basePackage + "/config"),
            "Missing config package (configuration)");
    }

    @Test
    @DisplayName("should have matching test mirror structure")
    void testTestMirrorStructure() {
        String testBase = "src/test/java/id/payu/portal";
        assertTrue(pathExists(testBase + "/adapter/web"),
            "Missing test adapter/web package");
        assertTrue(pathExists(testBase + "/application/service"),
            "Missing test application/service package");
    }

    // ────────── DTO Conventions ──────────

    @Test
    @DisplayName("should use Java records for DTOs")
    @SuppressWarnings("java:S5960") // Assertions in architecture tests are expected
    void testDtosUseRecords() throws IOException {
        Path dtoDir = Paths.get("src/main/java/id/payu/portal/dto");
        List<String> nonRecordClasses = Files.list(dtoDir)
            .filter(p -> p.toString().endsWith(".java"))
            .map(this::readFileContent)
            .filter(content -> !content.contains("public record "))
            .toList();

        assertTrue(nonRecordClasses.isEmpty(),
            "All DTOs should use Java records. Non-record files: " + String.join(", ", nonRecordClasses));
    }

    @Test
    @DisplayName("should not use float/double in DTOs for financial fields")
    void testNoPrimitiveTypesForMoney() throws IOException {
        Path dtoDir = Paths.get("src/main/java/id/payu/portal/dto");
        List<String> violations = Files.list(dtoDir)
            .filter(p -> p.toString().endsWith(".java"))
            .filter(p -> {
                String content = readFileContent(p);
                return content.contains("float ") || content.contains("double ");
            })
            .map(p -> p.getFileName().toString())
            .toList();

        assertTrue(violations.isEmpty(),
            "DTOs must not use float/double for financial fields: " + String.join(", ", violations));
    }

    @Test
    @DisplayName("should use BigDecimal for all monetary amounts")
    void testBigDecimalUsageForMoney() throws IOException {
        Path dtoDir = Paths.get("src/main/java/id/payu/portal/dto");
        java.util.Set<String> filesWithMoney = Files.list(dtoDir)
            .filter(p -> {
                String content = readFileContent(p);
                return content.contains("amount") || content.contains("value");
            })
            .map(p -> p.getFileName().toString())
            .collect(java.util.stream.Collectors.toSet());

        // Verify these files use BigDecimal, not float/double
        filesWithMoney.forEach(fileName -> {
            String content = readFileContent(Paths.get("src/main/java/id/payu/portal/dto/" + fileName));
            assertTrue(content.contains("BigDecimal"),
                fileName + " should use BigDecimal for monetary fields");
        });
    }

    // ────────── Controller Conventions ──────────

    @Test
    @DisplayName("should have SwaggerUI endpoints publicly accessible")
    void testSwaggerUiPermitAll() throws IOException {
        Path swaggerFile = Paths.get("src/main/java/id/payu/portal/adapter/web/SwaggerUiResource.java");
        String content = readFileContent(swaggerFile);

        assertTrue(content.contains("@PermitAll"),
            "SwaggerUiResource should be @PermitAll for public access");
    }

    @Test
    @DisplayName("should have API portal endpoints require authentication")
    void testApiEndpointsAuthenticated() throws IOException {
        Path portalFile = Paths.get("src/main/java/id/payu/portal/adapter/web/ApiPortalResource.java");
        Path sandboxFile = Paths.get("src/main/java/id/payu/portal/adapter/web/SandboxResource.java");

        String portalContent = readFileContent(portalFile);
        String sandboxContent = readFileContent(sandboxFile);

        assertTrue(portalContent.contains("@Authenticated"),
            "ApiPortalResource should be @Authenticated");
        assertTrue(sandboxContent.contains("@Authenticated"),
            "SandboxResource should be @Authenticated");
    }

    // ────────── Config Conventions ──────────

    @Test
    @DisplayName("should use SmallRye ConfigMapping for configuration")
    void testConfigMappingUsage() throws IOException {
        Path configDir = Paths.get("src/main/java/id/payu/portal/config");
        List<String> violations = Files.list(configDir)
            .filter(p -> {
                String name = p.getFileName().toString();
                return name.endsWith("Config.java");
            })
            .filter(p -> !readFileContent(p).contains("@ConfigMapping"))
            .map(p -> p.getFileName().toString())
            .toList();

        assertTrue(violations.isEmpty(),
            "Config classes should use @ConfigMapping: " + String.join(", ", violations));
    }

    // ────────── Helpers ──────────

    private boolean pathExists(String relativePath) {
        return Files.exists(Paths.get(relativePath));
    }

    private String readFileContent(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            fail("Failed to read file: " + path + " - " + e.getMessage());
            return "";
        }
    }
}
