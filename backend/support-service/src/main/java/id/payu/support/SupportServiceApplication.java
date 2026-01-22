package id.payu.support;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.jboss.logging.Logger;

@QuarkusMain
public class SupportServiceApplication implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(SupportServiceApplication.class);

    @Override
    public int run(String... args) {
        LOG.info("Starting PayU Support Service...");
        Quarkus.waitForExit();
        return 0;
    }
}
