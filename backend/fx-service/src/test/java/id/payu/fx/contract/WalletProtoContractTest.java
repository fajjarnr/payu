package id.payu.fx.contract;

import com.google.protobuf.Descriptors;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GRPC-022: wire-contract snapshot guard for the wallet proto. Each service
 * carries its own copy of WalletService.proto; this test pins the field
 * numbers of the shared money-path messages so an accidental rename/renumber
 * in any copy breaks the build instead of silently corrupting calls.
 */
class WalletProtoContractTest {

    private Map<String, Integer> fields(String message) {
        Descriptors.FileDescriptor file = id.payu.wallet.grpc.DebitRequest.getDescriptor().getFile();
        Descriptors.Descriptor descriptor = file.findMessageTypeByName(message);
        assertThat(descriptor).as("message %s must exist", message).isNotNull();
        return descriptor.getFields().stream()
                .collect(Collectors.toMap(Descriptors.FieldDescriptor::getName,
                        Descriptors.FieldDescriptor::getNumber));
    }

    @Test
    void debitRequestFieldNumbersAreStable() {
        assertThat(fields("DebitRequest"))
                .containsEntry("wallet_id", 1)
                .containsEntry("account_id", 2)
                .containsEntry("amount", 3)
                .containsEntry("reference_id", 4)
                .containsEntry("description", 5);
    }

    @Test
    void reserveBalanceRequestFieldNumbersAreStable() {
        assertThat(fields("ReserveBalanceRequest"))
                .containsEntry("wallet_id", 1)
                .containsEntry("account_id", 2)
                .containsEntry("amount", 3)
                .containsEntry("reference_id", 4)
                .containsEntry("description", 5);
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
    void transferRequestFieldNumbersAreStable() {
        assertThat(fields("TransferRequest"))
                .containsEntry("from_wallet_id", 1)
                .containsEntry("to_wallet_id", 2)
                .containsEntry("from_account_id", 3)
                .containsEntry("to_account_id", 4)
                .containsEntry("amount", 5)
                .containsEntry("reference_id", 6)
                .containsEntry("description", 7);
    }

    @Test
    void getBalanceRequestFieldNumbersAreStable() {
        assertThat(fields("GetBalanceRequest"))
                .containsEntry("wallet_id", 1)
                .containsEntry("account_id", 2);
    }
}
