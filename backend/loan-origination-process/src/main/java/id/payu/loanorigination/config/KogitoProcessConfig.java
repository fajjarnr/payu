package id.payu.loanorigination.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KogitoProcessConfig {

    private static final Logger log = LoggerFactory.getLogger(KogitoProcessConfig.class);

    // WorkItemHandlers are auto-discovered by jBPM via @Component beans
    // named after the BPMN task name (e.g., "CreditScoring", "Disbursement").
    // User tasks lifecycle managed via ws-human-task (configured in application.yml).
}
