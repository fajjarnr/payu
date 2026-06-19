package id.payu.account.adapter.persistence.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BeneficiaryTest {

    @Test
    void shouldCreateBeneficiaryWithBuilder() {
        UUID id = UUID.randomUUID();
        String bankCode = "BCA";
        String accountNumber = "1234567890";
        String accountName = "John Doe";
        String nickname = "My BCA AccountEntity";

        BeneficiaryEntity beneficiary = BeneficiaryEntity.builder()
                .id(id)
                .bankCode(bankCode)
                .accountNumber(accountNumber)
                .accountName(accountName)
                .nickname(nickname)
                .status(BeneficiaryStatus.ACTIVE)
                .build();

        assertEquals(id, beneficiary.getId());
        assertEquals(bankCode, beneficiary.getBankCode());
        assertEquals(accountNumber, beneficiary.getAccountNumber());
        assertEquals(accountName, beneficiary.getAccountName());
        assertEquals(nickname, beneficiary.getNickname());
        assertEquals(BeneficiaryStatus.ACTIVE, beneficiary.getStatus());
    }

    @Test
    void shouldAllowNullNickname() {
        BeneficiaryEntity beneficiary = BeneficiaryEntity.builder()
                .bankCode("BCA")
                .accountNumber("1234567890")
                .accountName("John Doe")
                .nickname(null)
                .build();

        assertNull(beneficiary.getNickname());
    }

    @Test
    void shouldSetVerifiedAt() {
        LocalDateTime verifiedAt = LocalDateTime.now();

        BeneficiaryEntity beneficiary = BeneficiaryEntity.builder()
                .bankCode("BCA")
                .accountNumber("1234567890")
                .accountName("John Doe")
                .verifiedAt(verifiedAt)
                .build();

        assertEquals(verifiedAt, beneficiary.getVerifiedAt());
    }

    @Test
    void shouldHaveAllStatuses() {
        assertNotNull(BeneficiaryStatus.ACTIVE);
        assertNotNull(BeneficiaryStatus.INACTIVE);
        assertNotNull(BeneficiaryStatus.DELETED);
    }
}
