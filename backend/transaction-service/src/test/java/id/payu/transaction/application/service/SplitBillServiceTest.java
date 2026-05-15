package id.payu.transaction.application.service;

import id.payu.transaction.adapter.persistence.entity.SplitBillEntity;
import id.payu.transaction.adapter.persistence.entity.SplitBillParticipantEntity;
import id.payu.transaction.domain.port.out.SplitBillPersistencePort;
import id.payu.transaction.domain.port.out.SplitBillEventPublisherPort;
import id.payu.transaction.dto.AddParticipantRequest;
import id.payu.transaction.dto.CreateSplitBillRequest;
import id.payu.transaction.dto.MakePaymentRequest;
import id.payu.transaction.dto.SplitBillResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SplitBillServiceTest {

    @Mock
    private SplitBillPersistencePort persistencePort;

    @Mock
    private SplitBillEventPublisherPort eventPublisher;

    @InjectMocks
    private SplitBillService splitBillService;

    private UUID accountId;
    private UUID splitBillId;
    private UUID participantId;
    private CreateSplitBillRequest createRequest;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        splitBillId = UUID.randomUUID();
        participantId = UUID.randomUUID();

        createRequest = CreateSplitBillRequest.builder()
                .creatorAccountId(accountId)
                .totalAmount(new BigDecimal("1000.00"))
                .currency("IDR")
                .title("Makan Bersama")
                .description("Makan di restoran Padang")
                .splitType(SplitBillEntity.SplitType.EQUAL)
                .dueDate(Instant.now().plusSeconds(86400 * 7))
                .participants(List.of(
                        CreateSplitBillRequest.ParticipantRequest.builder()
                                .accountId(accountId)
                                .accountNumber("1234567890")
                                .accountName("John Doe")
                                .isCreator(true)
                                .build(),
                        CreateSplitBillRequest.ParticipantRequest.builder()
                                .accountId(UUID.randomUUID())
                                .accountNumber("0987654321")
                                .accountName("Jane Smith")
                                .build()
                ))
                .build();
    }

    @Test
    void createSplitBill_ShouldCreateWithEqualSplit() {
        when(persistencePort.save(any(SplitBillEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(persistencePort.findParticipantsBySplitBillId(any())).thenReturn(List.of());

        SplitBillResponse response = splitBillService.createSplitBill(createRequest);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Makan Bersama");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("1000.00");
        assertThat(response.getCurrency()).isEqualTo("IDR");
        assertThat(response.getSplitType()).isEqualTo(SplitBillEntity.SplitType.EQUAL.name());
        assertThat(response.getStatus()).isEqualTo(SplitBillEntity.SplitStatus.DRAFT.name());

        verify(persistencePort).save(any(SplitBillEntity.class));
        verify(eventPublisher).publishSplitBillCreated(any(SplitBillEntity.class));
    }

    @Test
    void createSplitBill_WithMultipleParticipants_ShouldSplitEqually() {
        when(persistencePort.save(any(SplitBillEntity.class))).thenAnswer(invocation -> {
            SplitBillEntity bill = invocation.getArgument(0);
            bill.setId(splitBillId);
            return bill;
        });
        when(persistencePort.findParticipantsBySplitBillId(splitBillId)).thenAnswer(invocation -> {
            // Return the participants that would have been saved
            return List.of(
                    SplitBillParticipantEntity.builder()
                            .id(UUID.randomUUID())
                            .splitBillId(splitBillId)
                            .accountId(createRequest.getParticipants().get(0).getAccountId())
                            .accountNumber(createRequest.getParticipants().get(0).getAccountNumber())
                            .accountName(createRequest.getParticipants().get(0).getAccountName())
                            .amountOwed(new BigDecimal("500.00"))
                            .amountPaid(new BigDecimal("0.00"))
                            .status(SplitBillParticipantEntity.ParticipantStatus.PENDING)
                            .build(),
                    SplitBillParticipantEntity.builder()
                            .id(UUID.randomUUID())
                            .splitBillId(splitBillId)
                            .accountId(createRequest.getParticipants().get(1).getAccountId())
                            .accountNumber(createRequest.getParticipants().get(1).getAccountNumber())
                            .accountName(createRequest.getParticipants().get(1).getAccountName())
                            .amountOwed(new BigDecimal("500.00"))
                            .amountPaid(new BigDecimal("0.00"))
                            .status(SplitBillParticipantEntity.ParticipantStatus.PENDING)
                            .build()
            );
        });

        SplitBillResponse response = splitBillService.createSplitBill(createRequest);

        ArgumentCaptor<SplitBillEntity> captor = ArgumentCaptor.forClass(SplitBillEntity.class);
        verify(persistencePort).save(captor.capture());

        SplitBillEntity savedSplitBill = captor.getValue();
        assertThat(savedSplitBill.getParticipants()).hasSize(2);
        assertThat(savedSplitBill.getParticipants().get(0).getAmountOwed())
                .isEqualByComparingTo("500.00");
        assertThat(savedSplitBill.getParticipants().get(1).getAmountOwed())
                .isEqualByComparingTo("500.00");
    }

    @Test
    void getSplitBill_WhenExists_ShouldReturnSplitBill() {
        SplitBillEntity splitBill = SplitBillEntity.builder()
                .id(splitBillId)
                .referenceNumber("SPL123456")
                .creatorAccountId(accountId)
                .totalAmount(new BigDecimal("1000.00"))
                .currency("IDR")
                .title("Makan Bersama")
                .splitType(SplitBillEntity.SplitType.EQUAL)
                .status(SplitBillEntity.SplitStatus.ACTIVE)
                .participants(List.of(
                        SplitBillParticipantEntity.builder()
                                .id(participantId)
                                .accountId(accountId)
                                .accountNumber("1234567890")
                                .accountName("John Doe")
                                .amountOwed(new BigDecimal("500.00"))
                                .amountPaid(new BigDecimal("250.00"))
                                .status(SplitBillParticipantEntity.ParticipantStatus.PARTIALLY_PAID)
                                .build()
                ))
                .build();

        when(persistencePort.findById(splitBillId)).thenReturn(Optional.of(splitBill));
        when(persistencePort.findParticipantsBySplitBillId(splitBillId)).thenReturn(splitBill.getParticipants());

        SplitBillResponse response = splitBillService.getSplitBill(splitBillId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(splitBillId);
        assertThat(response.getReferenceNumber()).isEqualTo("SPL123456");
        assertThat(response.getTotalPaid()).isEqualByComparingTo("250.00");
        assertThat(response.getRemainingAmount()).isEqualByComparingTo("750.00");
    }

    @Test
    void getSplitBill_WhenNotExists_ShouldThrowException() {
        when(persistencePort.findById(splitBillId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> splitBillService.getSplitBill(splitBillId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Split bill not found");
    }

    @Test
    void activateSplitBill_WhenInDraft_ShouldActivateAndPublishEvent() {
        SplitBillEntity splitBill = SplitBillEntity.builder()
                .id(splitBillId)
                .splitType(SplitBillEntity.SplitType.EQUAL)
                .totalAmount(new BigDecimal("1000.00"))
                .currency("IDR")
                .status(SplitBillEntity.SplitStatus.DRAFT)
                .build();

        when(persistencePort.findById(splitBillId)).thenReturn(Optional.of(splitBill));
        when(persistencePort.save(any(SplitBillEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(persistencePort.findParticipantsBySplitBillId(splitBillId)).thenReturn(List.of());

        splitBillService.activateSplitBill(splitBillId);

        ArgumentCaptor<SplitBillEntity> captor = ArgumentCaptor.forClass(SplitBillEntity.class);
        verify(persistencePort).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(SplitBillEntity.SplitStatus.ACTIVE);
        verify(eventPublisher).publishSplitBillActivated(any(SplitBillEntity.class));
    }

    @Test
    void activateSplitBill_WhenNotDraft_ShouldThrowException() {
        SplitBillEntity splitBill = SplitBillEntity.builder()
                .id(splitBillId)
                .splitType(SplitBillEntity.SplitType.EQUAL)
                .totalAmount(new BigDecimal("1000.00"))
                .currency("IDR")
                .status(SplitBillEntity.SplitStatus.ACTIVE)
                .build();

        when(persistencePort.findById(splitBillId)).thenReturn(Optional.of(splitBill));

        assertThatThrownBy(() -> splitBillService.activateSplitBill(splitBillId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Can only activate draft split bills");
    }

    @Test
    void makePayment_WhenValid_ShouldUpdateParticipantAndPublishEvent() {
        UUID participantAccountId = UUID.randomUUID();
        SplitBillParticipantEntity participant = SplitBillParticipantEntity.builder()
                .id(participantId)
                .splitBillId(splitBillId)
                .accountId(participantAccountId)
                .accountNumber("1234567890")
                .accountName("John Doe")
                .amountOwed(new BigDecimal("500.00"))
                .amountPaid(new BigDecimal("200.00"))
                .status(SplitBillParticipantEntity.ParticipantStatus.PARTIALLY_PAID)
                .build();

        SplitBillEntity splitBill = SplitBillEntity.builder()
                .id(splitBillId)
                .referenceNumber("SPL123456")
                .creatorAccountId(accountId)
                .totalAmount(new BigDecimal("1000.00"))
                .currency("IDR")
                .title("Test Split Bill")
                .splitType(SplitBillEntity.SplitType.EQUAL)
                .status(SplitBillEntity.SplitStatus.IN_PROGRESS)
                .participants(List.of(participant))
                .build();

        MakePaymentRequest paymentRequest = MakePaymentRequest.builder()
                .amount(new BigDecimal("300.00"))
                .build();

        when(persistencePort.findById(splitBillId)).thenReturn(Optional.of(splitBill));
        when(persistencePort.findParticipantById(participantId)).thenReturn(Optional.of(participant));
        when(persistencePort.saveParticipant(any(SplitBillParticipantEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(persistencePort.save(any(SplitBillEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(persistencePort.findParticipantsBySplitBillId(splitBillId)).thenReturn(List.of(participant));

        splitBillService.makePayment(splitBillId, participantId, paymentRequest);

        ArgumentCaptor<SplitBillParticipantEntity> participantCaptor = ArgumentCaptor.forClass(SplitBillParticipantEntity.class);
        verify(persistencePort).saveParticipant(participantCaptor.capture());

        assertThat(participantCaptor.getValue().getAmountPaid()).isEqualByComparingTo("500.00");
        assertThat(participantCaptor.getValue().getStatus())
                .isEqualTo(SplitBillParticipantEntity.ParticipantStatus.SETTLED);

        verify(eventPublisher).publishPaymentMade(any(SplitBillEntity.class), any(SplitBillParticipantEntity.class),
                eq(new BigDecimal("300.00")));
    }

    @Test
    void makePayment_WhenExceedsOwedAmount_ShouldThrowException() {
        UUID participantAccountId = UUID.randomUUID();
        SplitBillParticipantEntity participant = SplitBillParticipantEntity.builder()
                .id(participantId)
                .splitBillId(splitBillId)
                .accountId(participantAccountId)
                .amountOwed(new BigDecimal("500.00"))
                .amountPaid(new BigDecimal("300.00"))
                .status(SplitBillParticipantEntity.ParticipantStatus.PARTIALLY_PAID)
                .build();

        SplitBillEntity splitBill = SplitBillEntity.builder()
                .id(splitBillId)
                .referenceNumber("SPL123456")
                .creatorAccountId(accountId)
                .totalAmount(new BigDecimal("1000.00"))
                .currency("IDR")
                .title("Test Split Bill")
                .splitType(SplitBillEntity.SplitType.EQUAL)
                .status(SplitBillEntity.SplitStatus.IN_PROGRESS)
                .participants(List.of(participant))
                .build();

        MakePaymentRequest paymentRequest = MakePaymentRequest.builder()
                .amount(new BigDecimal("300.00"))
                .build();

        when(persistencePort.findById(splitBillId)).thenReturn(Optional.of(splitBill));
        when(persistencePort.findParticipantById(participantId)).thenReturn(Optional.of(participant));

        assertThatThrownBy(() -> splitBillService.makePayment(splitBillId, participantId, paymentRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment exceeds amount owed");
    }

    @Test
    void addParticipant_WhenInDraft_ShouldAddParticipantAndPublishEvent() {
        SplitBillEntity splitBill = SplitBillEntity.builder()
                .id(splitBillId)
                .referenceNumber("SPL123456")
                .creatorAccountId(accountId)
                .totalAmount(new BigDecimal("1000.00"))
                .currency("IDR")
                .title("Test Split Bill")
                .splitType(SplitBillEntity.SplitType.EQUAL)
                .status(SplitBillEntity.SplitStatus.DRAFT)
                .build();

        AddParticipantRequest addRequest = AddParticipantRequest.builder()
                .accountId(UUID.randomUUID())
                .accountNumber("1234567890")
                .accountName("John Doe")
                .amountOwed(new BigDecimal("200.00"))
                .build();

        when(persistencePort.findById(splitBillId)).thenReturn(Optional.of(splitBill));
        when(persistencePort.saveParticipant(any(SplitBillParticipantEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(persistencePort.findParticipantsBySplitBillId(splitBillId)).thenReturn(List.of());

        splitBillService.addParticipant(splitBillId, addRequest);

        verify(persistencePort).saveParticipant(any(SplitBillParticipantEntity.class));
        verify(eventPublisher).publishParticipantAdded(any(SplitBillEntity.class), any(SplitBillParticipantEntity.class));
    }

    @Test
    void cancelSplitBill_WhenActive_ShouldCancelAndPublishEvent() {
        SplitBillEntity splitBill = SplitBillEntity.builder()
                .id(splitBillId)
                .referenceNumber("SPL123456")
                .creatorAccountId(accountId)
                .totalAmount(new BigDecimal("1000.00"))
                .currency("IDR")
                .title("Test Split Bill")
                .splitType(SplitBillEntity.SplitType.EQUAL)
                .status(SplitBillEntity.SplitStatus.ACTIVE)
                .build();

        when(persistencePort.findById(splitBillId)).thenReturn(Optional.of(splitBill));
        when(persistencePort.save(any(SplitBillEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(persistencePort.findParticipantsBySplitBillId(splitBillId)).thenReturn(List.of());

        SplitBillResponse response = splitBillService.cancelSplitBill(splitBillId);

        assertThat(response).isNotNull();
        ArgumentCaptor<SplitBillEntity> captor = ArgumentCaptor.forClass(SplitBillEntity.class);
        verify(persistencePort).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(SplitBillEntity.SplitStatus.CANCELLED);
        verify(eventPublisher).publishSplitBillCancelled(any(SplitBillEntity.class));
    }

    @Test
    void cancelSplitBill_WhenCompleted_ShouldThrowException() {
        SplitBillEntity splitBill = SplitBillEntity.builder()
                .id(splitBillId)
                .referenceNumber("SPL123456")
                .creatorAccountId(accountId)
                .totalAmount(new BigDecimal("1000.00"))
                .currency("IDR")
                .title("Test Split Bill")
                .splitType(SplitBillEntity.SplitType.EQUAL)
                .status(SplitBillEntity.SplitStatus.COMPLETED)
                .build();

        when(persistencePort.findById(splitBillId)).thenReturn(Optional.of(splitBill));

        assertThatThrownBy(() -> splitBillService.cancelSplitBill(splitBillId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel split bill in current status");
    }
}
