package id.payu.rules.service;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;

/**
 * Thread-safe service to execute Drools rules compiled from the classpath.
 */
public class RulesEngineService {
    private static final Logger log = LoggerFactory.getLogger(RulesEngineService.class);
    private final KieContainer kieContainer;

    public RulesEngineService() {
        log.info("Initializing RulesEngineService...");
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            // Scan for all DRL files under classpath*:rules/
            Resource[] resources = resolver.getResources("classpath*:rules/**/*.drl");
            log.info("Found {} rule file(s) in classpath*:rules/", resources.length);
            for (Resource resource : resources) {
                log.info("Loading DRL rule file: {}", resource.getFilename());
                kieFileSystem.write("src/main/resources/rules/" + resource.getFilename(),
                        ResourceFactory.newInputStreamResource(resource.getInputStream()));
            }
        } catch (IOException e) {
            log.error("Failed to read DRL rule files from classpath", e);
            throw new RuntimeException("Failed to read DRL rule files from classpath", e);
        }

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        if (kieBuilder.getResults().hasMessages(org.kie.api.builder.Message.Level.ERROR)) {
            String errors = kieBuilder.getResults().toString();
            log.error("Drools rule compilation failed:\n{}", errors);
            throw new IllegalStateException("Drools compilation failed: " + errors);
        }

        KieModule kieModule = kieBuilder.getKieModule();
        this.kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());
        log.info("RulesEngineService initialized successfully.");
    }

    /**
     * Executes rules against the provided facts.
     *
     * @param facts the facts to insert into the session
     */
    public void fireRules(Object... facts) {
        KieSession kieSession = kieContainer.newKieSession();
        try {
            for (Object fact : facts) {
                if (fact != null) {
                    kieSession.insert(fact);
                }
            }
            int rulesFired = kieSession.fireAllRules();
            log.debug("Successfully fired {} rule(s).", rulesFired);
        } finally {
            kieSession.dispose();
        }
    }
}
