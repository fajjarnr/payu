package id.payu.wallet.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.wallet.domain.model.EntryType;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.model.TransactionType;
import id.payu.wallet.domain.model.WalletTransaction;
import id.payu.wallet.dto.BalanceResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WalletMoneySerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializesWalletMoneyAsStrings() throws Exception {
        BalanceResponse balance = BalanceResponse.builder()
                .accountId("account-1")
                .balance(new BigDecimal("9007199254740993.1234"))
                .availableBalance(new BigDecimal("10.0000"))
                .reservedBalance(new BigDecimal("1.2500"))
                .currency("IDR")
                .build();
        WalletTransaction transaction = WalletTransaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.DEBIT)
                .amount(new BigDecimal("9007199254740993.1234"))
                .balanceAfter(new BigDecimal("10.0000"))
                .build();
        LedgerEntry ledgerEntry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .entryType(EntryType.DEBIT)
                .amount(new BigDecimal("9007199254740993.1234"))
                .balanceAfter(new BigDecimal("10.0000"))
                .build();

        assertThat(mapper.readTree(mapper.writeValueAsString(balance)).get("balance").isTextual()).isTrue();
        assertThat(mapper.readTree(mapper.writeValueAsString(transaction)).get("amount").isTextual()).isTrue();
        assertThat(mapper.readTree(mapper.writeValueAsString(ledgerEntry)).get("amount").isTextual()).isTrue();
    }
}
