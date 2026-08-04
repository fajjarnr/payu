package id.payu.lending.adapter.persistence;

import id.payu.lending.domain.model.Loan;
import id.payu.lending.entity.LoanEntity;
import id.payu.lending.repository.LoanRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

class LoanPersistenceAdapterTest {

    private final LoanRepository repository = mock(LoanRepository.class);
    private final LoanPersistenceAdapter adapter = new LoanPersistenceAdapter(repository);

    @Test
    void updatesExistingGeneratedIdEntityWithoutRecreatingDetachedInstance() {
        UUID loanId = UUID.randomUUID();
        Loan loan = new Loan();
        loan.setId(loanId);
        LoanEntity managed = new LoanEntity();
        managed.setId(loanId);
        when(repository.findById(loanId)).thenReturn(Optional.of(managed));
        when(repository.save(any(LoanEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adapter.save(loan);

        verify(repository).findById(loanId);
        verify(repository).save(same(managed));
    }
}
