package id.payu.lending.contract;

import com.google.protobuf.Descriptors;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GRPC-022: wire-contract snapshot for lending's WalletService.proto subset
 * (RepayLoan + Credit only). Field numbers must match the canonical wallet
 * proto so a rename/renumber in either copy breaks the build, not the wire.
 */
class WalletProtoContractTest {

    private Map<String, Integer> fields(String message) {
        Descriptors.FileDescriptor file = id.payu.wallet.grpc.CreditRequest.getDescriptor().getFile();
        Descriptors.Descriptor descriptor = file.findMessageTypeByName(message);
        assertThat(descriptor).as("message %s must exist", message).isNotNull();
        return descriptor.getFields().stream()
                .collect(Collectors.toMap(Descriptors.FieldDescriptor::getName,
                        Descriptors.FieldDescriptor::getNumber));
    }

    @Test
    void creditRequestFieldNumbersAreStable() {
        assertThat(fields("CreditRequest"))
                .containsEntry("wallet_id", 1)
                .containsEntry("account_id", 2)
                .containsEntry("amount", 3)
                .containsEntry("reference_id", 4)
                .containsEntry("description", 5);
    }

    @Test
    void loanRepaymentRequestFieldNumbersAreStable() {
        assertThat(fields("LoanRepaymentRequest"))
                .containsEntry("wallet_id", 1)
                .containsEntry("account_id", 2)
                .containsEntry("amount", 3)
                .containsEntry("reference_id", 4)
                .containsEntry("loan_id", 5)
                .containsEntry("description", 6);
    }
}
