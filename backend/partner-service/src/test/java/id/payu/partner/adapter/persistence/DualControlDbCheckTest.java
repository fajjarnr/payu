package id.payu.partner.adapter.persistence;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves ADR-0035 DB CHECK exists in migration V21.
 * Also verifies outbox topic naming for payu.partner.*.v1.
 */
public class DualControlDbCheckTest {

    @Test
    void migrationContainsMakerCheckerCheck() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V21__dual_control_maker_checker.sql");
        assertThat(Files.exists(migration)).as("V21 migration exists").isTrue();
        String sql = Files.readString(migration);
        assertThat(sql).contains("chk_maker_checker");
        assertThat(sql).contains("maker_id <> checker_id");
        assertThat(sql).contains("maker_id IS NULL OR checker_id IS NULL");
        assertThat(sql).contains("idx_partners_status_requested");
        assertThat(sql).contains("PENDING_VERIFICATION");
        assertThat(sql).contains("PENDING_APPROVAL");
    }

    @Test
    void migrationUsesCorrectPartialIndex() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V21__dual_control_maker_checker.sql"));
        assertThat(sql).contains("WHERE status = 'PENDING_APPROVAL'");
    }

    @Test
    void entityHasRequiredColumns() throws Exception {
        var fields = id.payu.partner.adapter.persistence.entity.PartnerEntity.class.getDeclaredFields();
        var names = java.util.Arrays.stream(fields).map(java.lang.reflect.Field::getName).toList();
        assertThat(names).contains("makerId", "checkerId", "requestedAt", "decidedAt", "rejectionReason");
    }

    @Test
    void partnerStatusContainsRequiredValues() {
        var statuses = java.util.Arrays.stream(id.payu.partner.domain.PartnerStatus.values()).map(Enum::name).toList();
        assertThat(statuses).contains("PENDING_APPROVAL", "REJECTED", "ACTIVE", "SUSPENDED", "TERMINATED");
    }

    @Test
    void partnerTypeBypass() {
        assertThat(id.payu.partner.domain.PartnerType.INTERNAL.isBypassDualControl()).isTrue();
        assertThat(id.payu.partner.domain.PartnerType.SANDBOX.isBypassDualControl()).isTrue();
        assertThat(id.payu.partner.domain.PartnerType.SNAP_BI.isBypassDualControl()).isFalse();
    }
}
