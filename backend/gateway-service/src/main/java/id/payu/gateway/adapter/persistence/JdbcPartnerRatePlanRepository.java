package id.payu.gateway.adapter.persistence;

import id.payu.gateway.domain.entity.PartnerRatePlan;
import id.payu.gateway.domain.repository.PartnerRatePlanRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JdbcPartnerRatePlanRepository implements PartnerRatePlanRepository {

    private static final String SELECT = "SELECT id, partner_id, rate_plan_id, assigned_at, effective_from, "
        + "effective_until, active FROM gateway_partner_rate_plans";
    private static final String SELECT_EFFECTIVE_BY_PARTNER =
        "SELECT id, partner_id, rate_plan_id, assigned_at, effective_from, effective_until, active "
        + "FROM gateway_partner_rate_plans WHERE partner_id = ? AND active = TRUE AND effective_from <= ? "
        + "AND (effective_until IS NULL OR effective_until >= ?) ORDER BY effective_from DESC LIMIT 1";
    private static final String SELECT_BY_PARTNER =
        "SELECT id, partner_id, rate_plan_id, assigned_at, effective_from, effective_until, active "
        + "FROM gateway_partner_rate_plans WHERE partner_id = ?";
    private static final String SELECT_BY_RATE_PLAN =
        "SELECT id, partner_id, rate_plan_id, assigned_at, effective_from, effective_until, active "
        + "FROM gateway_partner_rate_plans WHERE rate_plan_id = ?";

    @Inject
    JdbcGatewayStore store;

    @Override
    public Uni<Optional<PartnerRatePlan>> findEffectiveByPartnerId(String partnerId, Instant timestamp) {
        return store.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(SELECT_EFFECTIVE_BY_PARTNER)) {
                statement.setString(1, partnerId);
                JdbcGatewayStore.setInstant(statement, 2, timestamp);
                JdbcGatewayStore.setInstant(statement, 3, timestamp);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(read(resultSet)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public Multi<PartnerRatePlan> findByPartnerId(String partnerId) {
        return find(SELECT_BY_PARTNER, partnerId);
    }

    @Override
    public Multi<PartnerRatePlan> findByRatePlanId(String ratePlanId) {
        return find(SELECT_BY_RATE_PLAN, ratePlanId);
    }

    @Override
    public Uni<PartnerRatePlan> save(PartnerRatePlan assignment) {
        return store.query(connection -> {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                if ("PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) {
                    // ponytail: table lock keeps replacement atomic across replicas; use a partner lock if write volume requires it.
                    try (PreparedStatement lock = connection.prepareStatement(
                        "LOCK TABLE gateway_partner_rate_plans IN SHARE ROW EXCLUSIVE MODE")) {
                        lock.execute();
                    }
                }
                try (PreparedStatement deactivate = connection.prepareStatement(
                    "UPDATE gateway_partner_rate_plans SET active = FALSE, active_partner_key = NULL WHERE partner_id = ? AND active = TRUE")) {
                    deactivate.setString(1, assignment.getPartnerId());
                    deactivate.executeUpdate();
                }
                String update = "UPDATE gateway_partner_rate_plans SET partner_id = ?, rate_plan_id = ?, "
                    + "assigned_at = ?, effective_from = ?, effective_until = ?, active = ?, "
                    + "active_partner_key = CASE WHEN ? THEN partner_id ELSE NULL END WHERE id = ?";
                int updated;
                try (PreparedStatement statement = connection.prepareStatement(update)) {
                    statement.setString(1, assignment.getPartnerId());
                    statement.setString(2, assignment.getRatePlanId());
                    JdbcGatewayStore.setInstant(statement, 3, assignment.getAssignedAt());
                    JdbcGatewayStore.setInstant(statement, 4, assignment.getEffectiveFrom());
                    JdbcGatewayStore.setInstant(statement, 5, assignment.getEffectiveUntil());
                    statement.setBoolean(6, assignment.isActive());
                    statement.setBoolean(7, assignment.isActive());
                    statement.setString(8, assignment.getId());
                    updated = statement.executeUpdate();
                }
                if (updated == 0) {
                    try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO gateway_partner_rate_plans "
                            + "(id, partner_id, rate_plan_id, assigned_at, effective_from, effective_until, active, active_partner_key) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                        statement.setString(1, assignment.getId());
                        statement.setString(2, assignment.getPartnerId());
                        statement.setString(3, assignment.getRatePlanId());
                        JdbcGatewayStore.setInstant(statement, 4, assignment.getAssignedAt());
                        JdbcGatewayStore.setInstant(statement, 5, assignment.getEffectiveFrom());
                        JdbcGatewayStore.setInstant(statement, 6, assignment.getEffectiveUntil());
                        statement.setBoolean(7, assignment.isActive());
                        statement.setString(8, assignment.isActive() ? assignment.getPartnerId() : null);
                        statement.executeUpdate();
                    }
                }
                JdbcGatewayStore.audit(connection, "partner-rate-plan", assignment.getId(), "ASSIGN");
                connection.commit();
                return assignment;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        });
    }

    @Override
    public Uni<Void> deactivateByPartnerId(String partnerId) {
        return store.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE gateway_partner_rate_plans SET active = FALSE, active_partner_key = NULL "
                    + "WHERE partner_id = ? AND active = TRUE")) {
                statement.setString(1, partnerId);
                int updated = statement.executeUpdate();
                if (updated > 0) {
                    JdbcGatewayStore.audit(connection, "partner-rate-plan", partnerId, "DEACTIVATE");
                }
            }
            return null;
        });
    }

    @Override
    public Uni<Boolean> deleteById(String id) {
        return store.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM gateway_partner_rate_plans WHERE id = ?")) {
                statement.setString(1, id);
                boolean deleted = statement.executeUpdate() > 0;
                if (deleted) {
                    JdbcGatewayStore.audit(connection, "partner-rate-plan", id, "DELETE");
                }
                return deleted;
            }
        });
    }

    private Multi<PartnerRatePlan> find(String sql, String value) {
        return store.query(connection -> {
            List<PartnerRatePlan> assignments = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, value);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        assignments.add(read(resultSet));
                    }
                }
            }
            return assignments;
        }).onItem().transformToMulti(Multi.createFrom()::iterable);
    }

    private PartnerRatePlan read(ResultSet resultSet) throws SQLException {
        PartnerRatePlan assignment = new PartnerRatePlan(
            resultSet.getString("id"),
            resultSet.getString("partner_id"),
            resultSet.getString("rate_plan_id"));
        assignment.setEffectivePeriod(
            JdbcGatewayStore.getInstant(resultSet, "effective_from"),
            JdbcGatewayStore.getInstant(resultSet, "effective_until"));
        if (!resultSet.getBoolean("active")) {
            assignment.deactivate();
        }
        return assignment;
    }
}
