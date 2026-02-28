package id.payu.simulator.va;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * Virtual Account Simulator - Simulates bank callbacks for VA payment confirmation.
 *
 * <p>This simulator provides deterministic behavior for testing VA payment flows:
 * <ul>
 *   <li>VA number validation</li>
 *   <li>Payment confirmation callbacks</li>
 *   <li>Deterministic responses for consistent testing</li>
 * </ul>
 *
 * <p>Part of E-15 IMP-042: Virtual Account Payment Collection
 *
 * @author PayU Platform Engineering
 * @since 1.0.0
 */
@QuarkusMain
public class VaSimulatorApplication {

    public static void main(String[] args) {
        Quarkus.run(args);
    }
}
