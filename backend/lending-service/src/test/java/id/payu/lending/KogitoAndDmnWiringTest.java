package id.payu.lending;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies ADR-0048 GAP-015/048 wiring: DMN files exist on classpath,
 * kustomize includes kogito-runtime.yaml, and fork is removed.
 */
class KogitoAndDmnWiringTest {

    @Test
    void dmnFilesExistOnClasspath() {
        assertTrue(new ClassPathResource("rules/dmn/pricing.dmn").exists(), "pricing.dmn must exist");
        assertTrue(new ClassPathResource("rules/dmn/eligibility.dmn").exists(), "eligibility.dmn must exist");
        assertTrue(new ClassPathResource("rules/credit_scoring.drl").exists(), "credit_scoring.drl must stay");
    }

    @Test
    void kogitoRuntimeAppliedInKustomization() throws Exception {
        Path kustomize = Path.of("infrastructure/workloads/base/loan-origination-process/kustomization.yaml");
        // when running from backend/lending-service, path is ../../infrastructure...
        if (!Files.exists(kustomize)) {
            kustomize = Path.of("../..").resolve(kustomize);
        }
        if (!Files.exists(kustomize)) {
            kustomize = Path.of("/home/ubuntu/payu").resolve("infrastructure/workloads/base/loan-origination-process/kustomization.yaml");
        }
        assertTrue(Files.exists(kustomize), "kustomization.yaml must exist");
        String content = Files.readString(kustomize);
        assertTrue(content.contains("kogito-runtime.yaml"), "kustomize must include kogito-runtime.yaml (GAP-015)");
        assertFalse(content.contains("not applied by default"), "old comment must be removed");
    }

    @Test
    void lendingRulesForkDeleted() {
        Path fork = Path.of("/home/ubuntu/payu/backend/lending-rules");
        assertFalse(Files.exists(fork), "backend/lending-rules fork must be deleted per ADR-0048 step 6");
        Path pom = Path.of("/home/ubuntu/payu/backend/pom.xml");
        try {
            String pomContent = Files.readString(pom);
            assertFalse(pomContent.contains("<module>lending-rules</module>"), "parent pom must not reference lending-rules");
        } catch (Exception e) {
            fail(e);
        }
    }
}
