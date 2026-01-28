package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.FxRateInfo;
import id.payu.wallet.domain.model.Pocket;
import id.payu.wallet.domain.port.out.FxRateProviderPort;
import id.payu.wallet.domain.port.out.PocketPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PocketServiceTest {

    @Mock
    private PocketPersistencePort pocketPersistencePort;

    @Mock
    private FxRateProviderPort fxRateProviderPort;

    @InjectMocks
    private PocketService pocketService;

    private Pocket testPocket;
    private FxRateInfo testFxRate;

    @BeforeEach
    void setUp() {
        testPocket = Pocket.builder()
                .id(UUID.randomUUID())
                .accountId("ACC-001")
                .name("Travel Pocket")
                .description("For travel expenses")
                .currency("USD")
                .balance(new BigDecimal("1000"))
                .status(Pocket.PocketStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testFxRate = new FxRateInfo(
                UUID.randomUUID(),
                "USD",
                "IDR",
                new BigDecimal("15500"),
                new BigDecimal("0.0000645"),
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1)
        );
    }

    @Test
    @DisplayName("Should create new pocket")
    void shouldCreateNewPocket() {
        when(pocketPersistencePort.save(any(Pocket.class))).thenAnswer(inv -> inv.getArgument(0));

        Pocket result = pocketService.createPocket("ACC-001", "Travel Pocket", "For travel expenses", "USD");

        assertThat(result).isNotNull();
        assertThat(result.getAccountId()).isEqualTo("ACC-001");
        assertThat(result.getName()).isEqualTo("Travel Pocket");
        assertThat(result.getDescription()).isEqualTo("For travel expenses");
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getStatus()).isEqualTo(Pocket.PocketStatus.ACTIVE);
        verify(pocketPersistencePort).save(any(Pocket.class));
    }

    @Test
    @DisplayName("Should get pocket by ID")
    void shouldGetPocketById() {
        when(pocketPersistencePort.findById(testPocket.getId())).thenReturn(Optional.of(testPocket));

        Optional<Pocket> result = pocketService.getPocketById(testPocket.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(testPocket.getId());
        verify(pocketPersistencePort).findById(testPocket.getId());
    }

    @Test
    @DisplayName("Should return empty when pocket not found by ID")
    void shouldReturnEmptyWhenPocketNotFoundById() {
        UUID pocketId = UUID.randomUUID();
        when(pocketPersistencePort.findById(pocketId)).thenReturn(Optional.empty());

        Optional<Pocket> result = pocketService.getPocketById(pocketId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should get pockets by account ID")
    void shouldGetPocketsByAccountId() {
        when(pocketPersistencePort.findByAccountId("ACC-001")).thenReturn(List.of(testPocket));

        List<Pocket> result = pocketService.getPocketsByAccountId("ACC-001");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountId()).isEqualTo("ACC-001");
        verify(pocketPersistencePort).findByAccountId("ACC-001");
    }

    @Test
    @DisplayName("Should get pockets by account ID and currency")
    void shouldGetPocketsByAccountIdAndCurrency() {
        when(pocketPersistencePort.findByAccountIdAndCurrency("ACC-001", "USD")).thenReturn(List.of(testPocket));

        List<Pocket> result = pocketService.getPocketsByAccountIdAndCurrency("ACC-001", "USD");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCurrency()).isEqualTo("USD");
        verify(pocketPersistencePort).findByAccountIdAndCurrency("ACC-001", "USD");
    }

    @Test
    @DisplayName("Should credit pocket")
    void shouldCreditPocket() {
        when(pocketPersistencePort.findById(testPocket.getId())).thenReturn(Optional.of(testPocket));
        when(pocketPersistencePort.save(any(Pocket.class))).thenAnswer(inv -> inv.getArgument(0));

        pocketService.creditPocket(testPocket.getId(), new BigDecimal("500"), "REF-001");

        assertThat(testPocket.getBalance()).isEqualByComparingTo(new BigDecimal("1500"));
        verify(pocketPersistencePort).save(testPocket);
    }

    @Test
    @DisplayName("Should throw exception when crediting non-existent pocket")
    void shouldThrowExceptionWhenCreditingNonExistentPocket() {
        UUID pocketId = UUID.randomUUID();
        when(pocketPersistencePort.findById(pocketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pocketService.creditPocket(pocketId, new BigDecimal("500"), "REF-001"))
                .isInstanceOf(PocketNotFoundException.class)
                .hasMessageContaining(pocketId.toString());
    }

    @Test
    @DisplayName("Should debit pocket")
    void shouldDebitPocket() {
        when(pocketPersistencePort.findById(testPocket.getId())).thenReturn(Optional.of(testPocket));
        when(pocketPersistencePort.save(any(Pocket.class))).thenAnswer(inv -> inv.getArgument(0));

        pocketService.debitPocket(testPocket.getId(), new BigDecimal("300"), "REF-001");

        assertThat(testPocket.getBalance()).isEqualByComparingTo(new BigDecimal("700"));
        verify(pocketPersistencePort).save(testPocket);
    }

    @Test
    @DisplayName("Should throw exception when debiting non-existent pocket")
    void shouldThrowExceptionWhenDebitingNonExistentPocket() {
        UUID pocketId = UUID.randomUUID();
        when(pocketPersistencePort.findById(pocketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pocketService.debitPocket(pocketId, new BigDecimal("300"), "REF-001"))
                .isInstanceOf(PocketNotFoundException.class);
    }

    @Test
    @DisplayName("Should freeze pocket")
    void shouldFreezePocket() {
        when(pocketPersistencePort.findById(testPocket.getId())).thenReturn(Optional.of(testPocket));
        when(pocketPersistencePort.save(any(Pocket.class))).thenAnswer(inv -> inv.getArgument(0));

        pocketService.freezePocket(testPocket.getId());

        assertThat(testPocket.getStatus()).isEqualTo(Pocket.PocketStatus.FROZEN);
        verify(pocketPersistencePort).save(testPocket);
    }

    @Test
    @DisplayName("Should throw exception when freezing non-existent pocket")
    void shouldThrowExceptionWhenFreezingNonExistentPocket() {
        UUID pocketId = UUID.randomUUID();
        when(pocketPersistencePort.findById(pocketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pocketService.freezePocket(pocketId))
                .isInstanceOf(PocketNotFoundException.class);
    }

    @Test
    @DisplayName("Should unfreeze pocket")
    void shouldUnfreezePocket() {
        testPocket.setStatus(Pocket.PocketStatus.FROZEN);
        when(pocketPersistencePort.findById(testPocket.getId())).thenReturn(Optional.of(testPocket));
        when(pocketPersistencePort.save(any(Pocket.class))).thenAnswer(inv -> inv.getArgument(0));

        pocketService.unfreezePocket(testPocket.getId());

        assertThat(testPocket.getStatus()).isEqualTo(Pocket.PocketStatus.ACTIVE);
        verify(pocketPersistencePort).save(testPocket);
    }

    @Test
    @DisplayName("Should close pocket")
    void shouldClosePocket() {
        testPocket.setBalance(BigDecimal.ZERO);
        when(pocketPersistencePort.findById(testPocket.getId())).thenReturn(Optional.of(testPocket));
        when(pocketPersistencePort.save(any(Pocket.class))).thenAnswer(inv -> inv.getArgument(0));

        pocketService.closePocket(testPocket.getId());

        assertThat(testPocket.getStatus()).isEqualTo(Pocket.PocketStatus.CLOSED);
        verify(pocketPersistencePort).save(testPocket);
    }

    @Test
    @DisplayName("Should throw exception when closing non-existent pocket")
    void shouldThrowExceptionWhenClosingNonExistentPocket() {
        UUID pocketId = UUID.randomUUID();
        when(pocketPersistencePort.findById(pocketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pocketService.closePocket(pocketId))
                .isInstanceOf(PocketNotFoundException.class);
    }

    @Test
    @DisplayName("Should get total balance in same currency")
    void shouldGetTotalBalanceInSameCurrency() {
        Pocket idrPocket1 = Pocket.builder()
                .id(UUID.randomUUID())
                .accountId("ACC-001")
                .name("IDR Pocket 1")
                .currency("IDR")
                .balance(new BigDecimal("5000000"))
                .status(Pocket.PocketStatus.ACTIVE)
                .build();

        Pocket idrPocket2 = Pocket.builder()
                .id(UUID.randomUUID())
                .accountId("ACC-001")
                .name("IDR Pocket 2")
                .currency("IDR")
                .balance(new BigDecimal("3000000"))
                .status(Pocket.PocketStatus.ACTIVE)
                .build();

        when(pocketPersistencePort.findByAccountId("ACC-001")).thenReturn(List.of(idrPocket1, idrPocket2));

        BigDecimal result = pocketService.getTotalBalanceInCurrency("ACC-001", "IDR");

        assertThat(result).isEqualByComparingTo(new BigDecimal("8000000"));
        verify(pocketPersistencePort).findByAccountId("ACC-001");
        verify(fxRateProviderPort, never()).getCurrentRate(any(), any());
    }

    @Test
    @DisplayName("Should get total balance with currency conversion")
    void shouldGetTotalBalanceWithCurrencyConversion() {
        Pocket usdPocket = Pocket.builder()
                .id(UUID.randomUUID())
                .accountId("ACC-001")
                .name("USD Pocket")
                .currency("USD")
                .balance(new BigDecimal("100"))
                .status(Pocket.PocketStatus.ACTIVE)
                .build();

        when(pocketPersistencePort.findByAccountId("ACC-001")).thenReturn(List.of(usdPocket));
        when(fxRateProviderPort.getCurrentRate("USD", "IDR")).thenReturn(Optional.of(testFxRate));

        BigDecimal result = pocketService.getTotalBalanceInCurrency("ACC-001", "IDR");

        // 100 USD * 15500 = 1,550,000 IDR
        assertThat(result).isEqualByComparingTo(new BigDecimal("1550000"));
        verify(fxRateProviderPort).getCurrentRate("USD", "IDR");
    }

    @Test
    @DisplayName("Should throw exception when FX rate not available")
    void shouldThrowExceptionWhenFxRateNotAvailable() {
        Pocket usdPocket = Pocket.builder()
                .id(UUID.randomUUID())
                .accountId("ACC-001")
                .name("USD Pocket")
                .currency("USD")
                .balance(new BigDecimal("100"))
                .status(Pocket.PocketStatus.ACTIVE)
                .build();

        when(pocketPersistencePort.findByAccountId("ACC-001")).thenReturn(List.of(usdPocket));
        when(fxRateProviderPort.getCurrentRate("USD", "IDR")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pocketService.getTotalBalanceInCurrency("ACC-001", "IDR"))
                .isInstanceOf(FxRateNotFoundException.class)
                .hasMessageContaining("No FX rate available");
    }

    @Test
    @DisplayName("Should get all active pockets")
    void shouldGetAllActivePockets() {
        when(pocketPersistencePort.findAllActive()).thenReturn(List.of(testPocket));

        List<Pocket> result = pocketService.getAllActivePockets();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(Pocket.PocketStatus.ACTIVE);
        verify(pocketPersistencePort).findAllActive();
    }
}
