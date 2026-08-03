package id.payu.partner.adapter.persistence.repository;

import id.payu.partner.adapter.persistence.entity.SnapBiPaymentEntity;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SnapBiPaymentRepositoryLockTest {

    @Test
    void refundLookupUsesPessimisticWriteLock() {
        Method method = Arrays.stream(SnapBiPaymentRepository.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals("findForUpdateByPartnerIdAndReferenceNo"))
                .findFirst()
                .orElseThrow();

        assertEquals(LockModeType.PESSIMISTIC_WRITE, method.getAnnotation(Lock.class).value());
    }
}
