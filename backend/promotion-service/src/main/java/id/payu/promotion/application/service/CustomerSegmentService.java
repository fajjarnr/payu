package id.payu.promotion.application.service;

import id.payu.promotion.domain.model.CustomerSegment;
import id.payu.promotion.domain.model.SegmentMembership;
import id.payu.promotion.domain.port.out.CustomerSegmentPersistencePort;
import id.payu.promotion.domain.port.out.SegmentMembershipPersistencePort;
import id.payu.promotion.interfaces.dto.CreateCustomerSegmentRequest;
import id.payu.promotion.interfaces.dto.CustomerSegmentResponse;
import id.payu.promotion.interfaces.dto.SegmentMembershipResponse;
import id.payu.promotion.interfaces.dto.SegmentMembersResponse;
import id.payu.promotion.interfaces.dto.UpdateCustomerSegmentRequest;
import id.payu.promotion.interfaces.dto.UserSegmentsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * BE-PROMO-001: REST-accessible customer segmentation. The entity + migration
 * existed but there was no service exposing /api/v1/segments.
 */
@Service
public class CustomerSegmentService {

    private final CustomerSegmentPersistencePort segmentPort;
    private final SegmentMembershipPersistencePort membershipPort;

    public CustomerSegmentService(CustomerSegmentPersistencePort segmentPort,
                                  SegmentMembershipPersistencePort membershipPort) {
        this.segmentPort = segmentPort;
        this.membershipPort = membershipPort;
    }

    @Transactional
    public CustomerSegmentResponse create(CreateCustomerSegmentRequest request) {
        if (segmentPort.existsByName(request.name())) {
            throw new IllegalArgumentException("Segment with name '" + request.name() + "' already exists");
        }
        CustomerSegment segment = segmentPort.save(new CustomerSegment(
                UUID.randomUUID(), request.name(), request.description(), request.rules(),
                request.isActive() != null ? request.isActive() : true,
                request.priority() != null ? request.priority() : 0,
                null, null, null));
        return toResponse(segment);
    }

    @Transactional(readOnly = true)
    public List<CustomerSegmentResponse> listAll() {
        return segmentPort.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerSegmentResponse> listActive() {
        return segmentPort.findByIsActiveTrue().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<CustomerSegmentResponse> getById(UUID id) {
        return segmentPort.findById(id).map(this::toResponse);
    }

    @Transactional
    public Optional<CustomerSegmentResponse> update(UUID id, UpdateCustomerSegmentRequest request) {
        return segmentPort.findById(id).map(existing -> {
            CustomerSegment updated = segmentPort.save(new CustomerSegment(
                    existing.id(), request.name(), request.description(), request.rules(),
                    request.isActive() != null ? request.isActive() : existing.isActive(),
                    request.priority() != null ? request.priority() : existing.priority(),
                    existing.createdAt(), existing.updatedAt(), existing.version()));
            return toResponse(updated);
        });
    }

    @Transactional
    public boolean delete(UUID id) {
        if (segmentPort.findById(id).isEmpty()) {
            return false;
        }
        segmentPort.deleteById(id);
        return true;
    }

    @Transactional(readOnly = true)
    public UserSegmentsResponse getByAccount(String accountId) {
        List<SegmentMembershipResponse> memberships = membershipPort.findByAccountId(accountId).stream()
                .map(m -> new SegmentMembershipResponse(
                        m.id(), m.accountId(), m.segmentId(),
                        segmentPort.findById(m.segmentId()).map(CustomerSegment::name).orElse(null),
                        m.isActive(), m.lastEvaluatedAt(), m.createdAt()))
                .toList();
        return new UserSegmentsResponse(accountId, memberships);
    }

    @Transactional(readOnly = true)
    public SegmentMembersResponse getMembers(UUID segmentId) {
        CustomerSegment segment = segmentPort.findById(segmentId)
                .orElseThrow(() -> new IllegalArgumentException("Segment not found: " + segmentId));
        List<String> accountIds = membershipPort.findBySegmentId(segmentId).stream()
                .map(SegmentMembership::accountId)
                .toList();
        return new SegmentMembersResponse(
                segmentId.toString(), segment.name(), (long) accountIds.size(), accountIds);
    }

    private CustomerSegmentResponse toResponse(CustomerSegment s) {
        return new CustomerSegmentResponse(
                s.id(), s.name(), s.description(), s.rules(), s.isActive(), s.priority(),
                membershipPort.countBySegmentId(s.id()),
                s.createdAt(), s.updatedAt());
    }
}
