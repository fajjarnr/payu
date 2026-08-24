package id.payu.dispute.application.service;

import id.payu.dispute.domain.model.Chargeback;
import id.payu.dispute.domain.model.ChargebackStatus;
import id.payu.dispute.domain.port.in.ChargebackUseCase;
import id.payu.dispute.domain.port.out.ChargebackPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChargebackService implements ChargebackUseCase {

    private final ChargebackPersistencePort persistence;

    @Override
    public Chargeback create(UUID transactionId, UUID customerId, UUID merchantId, BigDecimal amount, String currency, String reason) {
        Chargeback cb = Chargeback.create(transactionId, customerId, merchantId, amount, currency, reason);
        return persistence.save(cb);
    }

    @Override
    public Chargeback submit(UUID chargebackId, String schemeCaseId) {
        Chargeback cb = persistence.findById(chargebackId).orElseThrow(() -> new IllegalArgumentException("Chargeback not found: " + chargebackId));
        cb.submit(schemeCaseId);
        return persistence.save(cb);
    }

    @Override
    public Chargeback startReview(UUID chargebackId) {
        Chargeback cb = persistence.findById(chargebackId).orElseThrow(() -> new IllegalArgumentException("Chargeback not found: " + chargebackId));
        cb.startReview();
        return persistence.save(cb);
    }

    @Override
    public Chargeback accept(UUID chargebackId) {
        Chargeback cb = persistence.findById(chargebackId).orElseThrow(() -> new IllegalArgumentException("Chargeback not found: " + chargebackId));
        cb.accept();
        return persistence.save(cb);
    }

    @Override
    public Chargeback reject(UUID chargebackId, String rejectionReason) {
        Chargeback cb = persistence.findById(chargebackId).orElseThrow(() -> new IllegalArgumentException("Chargeback not found: " + chargebackId));
        cb.reject(rejectionReason);
        return persistence.save(cb);
    }

    @Override
    public Chargeback reverse(UUID chargebackId) {
        Chargeback cb = persistence.findById(chargebackId).orElseThrow(() -> new IllegalArgumentException("Chargeback not found: " + chargebackId));
        cb.reverse();
        return persistence.save(cb);
    }

    @Override
    public Chargeback close(UUID chargebackId) {
        Chargeback cb = persistence.findById(chargebackId).orElseThrow(() -> new IllegalArgumentException("Chargeback not found: " + chargebackId));
        cb.close();
        return persistence.save(cb);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Chargeback> getById(UUID id) {
        return persistence.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Chargeback> getAll() {
        return persistence.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Chargeback> getByStatus(String status) {
        ChargebackStatus s = ChargebackStatus.valueOf(status.toUpperCase());
        return persistence.findByStatus(s);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Chargeback> getByCustomer(UUID customerId) {
        return persistence.findByCustomerId(customerId);
    }
}
