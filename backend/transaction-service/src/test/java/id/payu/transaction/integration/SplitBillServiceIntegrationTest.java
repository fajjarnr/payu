package id.payu.transaction.integration;

import id.payu.outbox.repository.OutboxRepository;
import id.payu.transaction.adapter.persistence.entity.SplitBillEntity;
import id.payu.transaction.domain.model.SplitStatus;
import id.payu.transaction.adapter.persistence.repository.SplitBillJpaRepository;
import id.payu.transaction.application.service.SplitBillService;
import id.payu.transaction.config.TestcontainersConfig;
import id.payu.transaction.interfaces.dto.CreateSplitBillRequest;
import id.payu.transaction.interfaces.dto.MakePaymentRequest;
import id.payu.transaction.interfaces.dto.SplitBillResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QAMVP-008: split-bill money journey against real PostgreSQL (Testcontainers)
 * with the real transactional-outbox event publisher.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"payu.grpc.server.port=0", "spring.jpa.hibernate.ddl-auto=none"})
@Import(TestcontainersConfig.class)
@DisplayName("QAMVP-008 — split-bill journey (real PostgreSQL + outbox)")
class SplitBillServiceIntegrationTest {

    @Autowired
    private SplitBillService splitBillService;

    @Autowired
    private SplitBillJpaRepository splitBillJpaRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    private CreateSplitBillRequest createRequest(UUID creatorId) {
        CreateSplitBillRequest req = new CreateSplitBillRequest();
        req.setCreatorAccountId(creatorId);
        req.setTotalAmount(new BigDecimal("300000.0000"));
        req.setCurrency("IDR");
        req.setTitle("Team lunch");
        req.setSplitType(id.payu.transaction.domain.model.SplitType.EQUAL);
        req.setDueDate(Instant.now().plus(java.time.Duration.ofDays(7)));
        req.setParticipants(List.of(
                CreateSplitBillRequest.ParticipantRequest.builder()
                        .accountId(creatorId).accountNumber("0123456789")
                        .accountName("Creator").amountOwed(new BigDecimal("150000.0000"))
                        .build(),
                CreateSplitBillRequest.ParticipantRequest.builder()
                        .accountId(UUID.randomUUID()).accountNumber("0987654321")
                        .accountName("Participant").amountOwed(new BigDecimal("150000.0000"))
                        .build()));
        return req;
    }

    @Test
    @DisplayName("create → activate → add participant → payment → settle persists + publishes outbox")
    void splitBillJourney() {
        UUID creatorId = UUID.randomUUID();

        SplitBillResponse created = splitBillService.createSplitBill(createRequest(creatorId));
        assertThat(created.getStatus()).isEqualTo("DRAFT");

        SplitBillResponse activated = splitBillService.activateSplitBill(created.getId());
        assertThat(activated.getStatus()).isEqualTo("ACTIVE");

        UUID payeeId = created.getParticipants().get(1).getId();
        SplitBillResponse accepted = splitBillService.acceptSplitBill(created.getId(), payeeId);
        assertThat(accepted.getStatus()).isEqualTo("IN_PROGRESS");

        SplitBillResponse paid = splitBillService.makePayment(created.getId(), payeeId,
                MakePaymentRequest.builder().amount(new BigDecimal("150000.0000")).build());
        assertThat(paid.getStatus()).isEqualTo("IN_PROGRESS");

        UUID creatorPayeeId = created.getParticipants().get(0).getId();
        splitBillService.acceptSplitBill(created.getId(), creatorPayeeId);
        SplitBillResponse completed = splitBillService.makePayment(created.getId(), creatorPayeeId,
                MakePaymentRequest.builder().amount(new BigDecimal("150000.0000")).build());
        assertThat(completed.getStatus()).isEqualTo("COMPLETED");

        SplitBillEntity persisted = splitBillJpaRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(SplitStatus.COMPLETED);

        long outboxEvents = outboxRepository.findByAggregateId(created.getId().toString()).size();
        assertThat(outboxEvents).as("split-bill journey must publish outbox events (created/activated/completed)")
                .isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("cancel leaves no money movement and no completion")
    void cancelSplitBill() {
        SplitBillResponse created = splitBillService.createSplitBill(createRequest(UUID.randomUUID()));
        SplitBillResponse cancelled = splitBillService.cancelSplitBill(created.getId());
        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");

        SplitBillEntity persisted = splitBillJpaRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(SplitStatus.CANCELLED);
    }
}
