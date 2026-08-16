package id.payu.rules.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RulesEngineService path resolution (RULES-COLLISION-001)")
class RulesEngineServicePathTest {

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Test
    @DisplayName("relativePath preserves subfolder structure for file URLs")
    void preservesSubfolderPathForFileUrl() throws Exception {
        ClassPathResource resource = new ClassPathResource("rules/credit/loan.drl");
        assertThat(RulesEngineService.relativePath(resource)).isEqualTo("credit/loan.drl");
    }

    @Test
    @DisplayName("relativePath preserves subfolder structure for nested rules")
    void preservesNestedPath() throws Exception {
        ClassPathResource resource = new ClassPathResource("rules/fraud/risk/rules.drl");
        assertThat(RulesEngineService.relativePath(resource)).isEqualTo("fraud/risk/rules.drl");
    }

    @Test
    @DisplayName("relativePath falls back to filename when no /rules/ segment exists")
    void fallsBackToFilenameWhenNoRulesSegment() throws Exception {
        ClassPathResource resource = new ClassPathResource("rules-only.drl");
        assertThat(RulesEngineService.relativePath(resource)).isEqualTo("rules-only.drl");
    }

    @Test
    @DisplayName("classpath scan finds DRLs in different subfolders")
    void classpathScanFindsSubfolderDials() throws Exception {
        org.springframework.core.io.Resource[] resources =
                resolver.getResources("classpath*:rules/**/*.drl");

        boolean sawCredit = false;
        boolean sawFraud = false;
        for (org.springframework.core.io.Resource r : resources) {
            String path = RulesEngineService.relativePath(r);
            if ("credit/loan.drl".equals(path)) {
                sawCredit = true;
            }
            if ("fraud/risk/rules.drl".equals(path)) {
                sawFraud = true;
            }
        }
        assertThat(sawCredit).isTrue();
        assertThat(sawFraud).isTrue();
    }
}
