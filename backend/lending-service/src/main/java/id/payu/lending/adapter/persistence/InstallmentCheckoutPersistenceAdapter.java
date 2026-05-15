package id.payu.lending.adapter.persistence;

import id.payu.lending.domain.model.InstallmentCheckout;
import id.payu.lending.domain.model.CheckoutStatus;
import id.payu.lending.domain.port.out.InstallmentCheckoutPersistencePort;
import id.payu.lending.entity.InstallmentCheckoutEntity;
import id.payu.lending.repository.InstallmentCheckoutRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class InstallmentCheckoutPersistenceAdapter implements InstallmentCheckoutPersistencePort {

    private final InstallmentCheckoutRepository repository;

    public InstallmentCheckoutPersistenceAdapter(InstallmentCheckoutRepository repository) {
        this.repository = repository;
    }

    @Override
    public InstallmentCheckout save(InstallmentCheckout checkout) {
        InstallmentCheckoutEntity entity = toEntity(checkout);
        InstallmentCheckoutEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<InstallmentCheckout> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<InstallmentCheckout> findByUserId(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<InstallmentCheckout> findByExternalOrderId(String externalOrderId) {
        return repository.findByExternalOrderId(externalOrderId).map(this::toDomain);
    }

    private InstallmentCheckout toDomain(InstallmentCheckoutEntity entity) {
        InstallmentCheckout checkout = new InstallmentCheckout();
        checkout.setId(entity.getId());
        checkout.setUserId(entity.getUserId());
        checkout.setPayLaterId(entity.getPayLaterId());
        checkout.setLoanId(entity.getLoanId());
        checkout.setPartnerId(entity.getPartnerId());
        checkout.setExternalOrderId(entity.getExternalOrderId());
        checkout.setPurchaseAmount(entity.getPurchaseAmount());
        checkout.setCurrency(entity.getCurrency());
        checkout.setTenor(entity.getTenor());
        checkout.setMonthlyPayment(entity.getMonthlyPayment());
        checkout.setInterestRate(entity.getInterestRate());
        checkout.setStatus(entity.getStatus());
        checkout.setFailureReason(entity.getFailureReason());
        checkout.setCreatedAt(entity.getCreatedAt());
        checkout.setUpdatedAt(entity.getUpdatedAt());
        return checkout;
    }

    private InstallmentCheckoutEntity toEntity(InstallmentCheckout checkout) {
        InstallmentCheckoutEntity entity = new InstallmentCheckoutEntity();
        entity.setId(checkout.getId());
        entity.setUserId(checkout.getUserId());
        entity.setPayLaterId(checkout.getPayLaterId());
        entity.setLoanId(checkout.getLoanId());
        entity.setPartnerId(checkout.getPartnerId());
        entity.setExternalOrderId(checkout.getExternalOrderId());
        entity.setPurchaseAmount(checkout.getPurchaseAmount());
        entity.setCurrency(checkout.getCurrency());
        entity.setTenor(checkout.getTenor());
        entity.setMonthlyPayment(checkout.getMonthlyPayment());
        entity.setInterestRate(checkout.getInterestRate());
        entity.setStatus(checkout.getStatus());
        entity.setFailureReason(checkout.getFailureReason());
        entity.setCreatedAt(checkout.getCreatedAt());
        entity.setUpdatedAt(checkout.getUpdatedAt());
        return entity;
    }
}
