package id.payu.backoffice;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.jboss.logging.Logger;

@QuarkusMain
public class BackofficeServiceApplication implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(BackofficeServiceApplication.class);

    @Override
    public int run(String... args) {
        LOG.info("Starting PayU Backoffice Service...");
        Quarkus.waitForExit();
        return 0;
    }
}
