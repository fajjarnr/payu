package id.payu.lending.adapter.persistence;

import id.payu.lending.domain.model.RepaymentSchedule;
import id.payu.lending.entity.RepaymentScheduleEntity;
import id.payu.lending.repository.RepaymentScheduleRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

class RepaymentSchedulePersistenceAdapterTest {

    private final RepaymentScheduleRepository repository = mock(RepaymentScheduleRepository.class);
    private final RepaymentSchedulePersistenceAdapter adapter = new RepaymentSchedulePersistenceAdapter(repository);

    @Test
    void updatesExistingGeneratedIdEntityWithoutRecreatingDetachedInstance() {
        UUID scheduleId = UUID.randomUUID();
        RepaymentSchedule schedule = new RepaymentSchedule();
        schedule.setId(scheduleId);
        RepaymentScheduleEntity managed = new RepaymentScheduleEntity();
        managed.setId(scheduleId);
        when(repository.findById(scheduleId)).thenReturn(Optional.of(managed));
        when(repository.save(any(RepaymentScheduleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adapter.save(schedule);

        verify(repository).findById(scheduleId);
        verify(repository).save(same(managed));
    }
}
