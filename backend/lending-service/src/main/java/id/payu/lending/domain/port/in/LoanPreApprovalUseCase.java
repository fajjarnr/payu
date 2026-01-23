package id.payu.lending.domain.port.in;

import id.payu.lending.dto.LoanPreApprovalRequest;
import id.payu.lending.dto.LoanPreApprovalResponse;
import id.payu.lending.domain.model.LoanPreApproval;

import java.util.Optional;
import java.util.UUID;

public interface LoanPreApprovalUseCase {

    LoanPreApprovalResponse checkPreApproval(LoanPreApprovalRequest request);

    Optional<LoanPreApproval> getPreApprovalById(UUID preApprovalId);

    Optional<LoanPreApproval> getActivePreApprovalByUserId(UUID userId);
}
