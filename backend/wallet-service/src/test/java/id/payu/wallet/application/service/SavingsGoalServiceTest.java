package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.Pocket;
import id.payu.wallet.domain.model.SavingsGoal;
import id.payu.wallet.domain.model.SavingsGoalStatus;
import id.payu.wallet.domain.port.in.PocketUseCase;
import id.payu.wallet.domain.port.out.SavingsGoalPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * SAVINGS-UUID-001 regression: createSavingsGoal must not crash when the
 * wallet accountId is a PayU string identifier (e.g. ACC-12345678) instead
 * of a pure UUID.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SavingsGoalService (SAVINGS-UUID-001)")
class SavingsGoalServiceTest {

    @Mock
    private SavingsGoalPersistencePort savingsGoalPersistencePort;

    @Mock
    private PocketUseCase pocketUseCase;

    @InjectMocks
    private SavingsGoalService savingsGoalService;

    private UUID pocketId;
    private Pocket pocket;

    @BeforeEach
    void setUp() {
        savingsGoalService = new SavingsGoalService(
                savingsGoalPersistencePort, pocketUseCase, Clock.systemUTC());
        pocketId = UUID.randomUUID();
        pocket = new Pocket();
        pocket.setId(pocketId);
    }

    @Test
    @DisplayName("creates goal when accountId is a non-UUID PayU identifier (ACC-...)")
    void createsGoalWhenAccountIdIsNonUuidIdentifier() {
        pocket.setAccountId("ACC-12345678");
        when(pocketUseCase.getPocketById(pocketId)).thenReturn(Optional.of(pocket));
        when(savingsGoalPersistencePort.save(any(SavingsGoal.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SavingsGoal goal = savingsGoalService.createSavingsGoal(
                pocketId, "ACC-12345678", "Trip to Bali", null,
                new BigDecimal("5000000"), LocalDate.of(2026, 12, 31), null, null);

        assertNotNull(goal);
    }

    @Test
    @DisplayName("creates goal when accountId is a sender-style identifier")
    void createsGoalWhenAccountIdIsSenderIdentifier() {
        pocket.setAccountId("sender-6b715675");
        when(pocketUseCase.getPocketById(pocketId)).thenReturn(Optional.of(pocket));
        when(savingsGoalPersistencePort.save(any(SavingsGoal.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SavingsGoal goal = savingsGoalService.createSavingsGoal(
                pocketId, "sender-6b715675", "Emergency Fund", null,
                new BigDecimal("10000000"), LocalDate.of(2027, 1, 1), null, null);

        assertNotNull(goal);
    }
}
