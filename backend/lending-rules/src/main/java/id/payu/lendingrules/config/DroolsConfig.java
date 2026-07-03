package id.payu.lendingrules.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.runtime.KieContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;

@Configuration
public class DroolsConfig {
    private static final Logger log = LoggerFactory.getLogger(DroolsConfig.class);

    @Bean
    public KieContainer kieContainer() {
        log.info("Initializing Drools KieContainer from classpath:rules/");
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            Resource[] resources = resolver.getResources("classpath*:rules/**/*.drl");
            log.info("Found {} DRL file(s)", resources.length);
            for (Resource res : resources) {
                log.info("Loading: {}", res.getFilename());
                kfs.write("src/main/resources/rules/" + res.getFilename(),
                        ks.getResources().newInputStreamResource(res.getInputStream()));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load DRL rules from classpath", e);
        }

        KieBuilder kb = ks.newKieBuilder(kfs);
        kb.buildAll();
        if (kb.getResults().hasMessages(org.kie.api.builder.Message.Level.ERROR)) {
            throw new IllegalStateException("Drools compile errors:\n" + kb.getResults());
        }

        KieContainer kc = ks.newKieContainer(kb.getKieModule().getReleaseId());
        log.info("KieContainer initialized successfully");
        return kc;
    }
}
