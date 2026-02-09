package id.payu.saga.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SagaResult}.
 * Validates factory methods and result classification.
 */
@DisplayName("SagaResult")
class SagaResultTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("success() should create completed result with data")
        void shouldCreateSuccessResult() {
            SagaResult<String> result = SagaResult.success("saga-1", "TransferSaga", "transfer-data");

            assertThat(result.getSagaId()).isEqualTo("saga-1");
            assertThat(result.getSagaType()).isEqualTo("TransferSaga");
            assertThat(result.getFinalState()).isEqualTo(SagaState.COMPLETED);
            assertThat(result.getData()).isEqualTo("transfer-data");
            assertThat(result.getCompletedAt()).isNotNull();
            assertThat(result.getErrorMessage()).isNull();
            assertThat(result.getErrorStep()).isNull();
        }

        @Test
        @DisplayName("failure() should create failed result with error details")
        void shouldCreateFailureResult() {
            SagaResult<String> result = SagaResult.failure("saga-1", "TransferSaga", "Insufficient balance", "debitStep");

            assertThat(result.getSagaId()).isEqualTo("saga-1");
            assertThat(result.getFinalState()).isEqualTo(SagaState.FAILED);
            assertThat(result.getErrorMessage()).isEqualTo("Insufficient balance");
            assertThat(result.getErrorStep()).isEqualTo("debitStep");
            assertThat(result.getCompletedAt()).isNotNull();
            assertThat(result.getData()).isNull();
        }

        @Test
        @DisplayName("compensated() should create compensated result")
        void shouldCreateCompensatedResult() {
            SagaResult<String> result = SagaResult.compensated("saga-1", "TransferSaga");

            assertThat(result.getFinalState()).isEqualTo(SagaState.COMPENSATED);
            assertThat(result.getCompletedAt()).isNotNull();
            assertThat(result.getData()).isNull();
        }
    }

    @Nested
    @DisplayName("State checks")
    class StateCheckTests {

        @Test
        @DisplayName("isSuccess() should return true only for COMPLETED")
        void shouldIdentifySuccess() {
            SagaResult<String> success = SagaResult.success("s1", "T", "data");
            SagaResult<String> failure = SagaResult.failure("s1", "T", "err", "step");
            SagaResult<String> compensated = SagaResult.compensated("s1", "T");

            assertThat(success.isSuccess()).isTrue();
            assertThat(failure.isSuccess()).isFalse();
            assertThat(compensated.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("isFailure() should return true for FAILED, COMPENSATION_FAILED, TIMED_OUT")
        void shouldIdentifyFailure() {
            SagaResult<String> failed = SagaResult.failure("s1", "T", "err", "step");
            SagaResult<String> compFailed = SagaResult.<String>builder()
                    .finalState(SagaState.COMPENSATION_FAILED).build();
            SagaResult<String> timedOut = SagaResult.<String>builder()
                    .finalState(SagaState.TIMED_OUT).build();
            SagaResult<String> success = SagaResult.success("s1", "T", "data");

            assertThat(failed.isFailure()).isTrue();
            assertThat(compFailed.isFailure()).isTrue();
            assertThat(timedOut.isFailure()).isTrue();
            assertThat(success.isFailure()).isFalse();
        }

        @Test
        @DisplayName("isCompensated() should return true only for COMPENSATED")
        void shouldIdentifyCompensated() {
            SagaResult<String> compensated = SagaResult.compensated("s1", "T");
            SagaResult<String> success = SagaResult.success("s1", "T", "data");
            SagaResult<String> failed = SagaResult.failure("s1", "T", "err", "step");

            assertThat(compensated.isCompensated()).isTrue();
            assertThat(success.isCompensated()).isFalse();
            assertThat(failed.isCompensated()).isFalse();
        }
    }
}
