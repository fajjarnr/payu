package id.payu.gateway.adapter.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.gateway.domain.entity.RatePlan;
import id.payu.gateway.domain.repository.RatePlanRepository;
import id.payu.gateway.domain.vo.RateLimit;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class JdbcRatePlanRepository implements RatePlanRepository {

    private static final String SELECT = "SELECT id, name, description, requests_per_minute, requests_per_hour, "
        + "requests_per_day, endpoint_overrides, active FROM gateway_rate_plans";

    @Inject
    JdbcGatewayStore store;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public Uni<Optional<RatePlan>> findById(String id) {
        return store.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(SELECT + " WHERE id = ?")) {
                statement.setString(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(read(resultSet)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public Uni<Optional<RatePlan>> findByName(String name) {
        return store.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(SELECT + " WHERE lower(name) = lower(?)")) {
                statement.setString(1, name);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(read(resultSet)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public Multi<RatePlan> findAllActive() {
        return findAll().filter(RatePlan::isActive);
    }

    @Override
    public Multi<RatePlan> findAll() {
        return store.query(connection -> {
            List<RatePlan> plans = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(SELECT + " ORDER BY name");
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    plans.add(read(resultSet));
                }
            }
            return plans;
        }).onItem().transformToMulti(Multi.createFrom()::iterable);
    }

    @Override
    public Uni<RatePlan> save(RatePlan plan) {
        return store.query(connection -> {
            String update = "UPDATE gateway_rate_plans SET name = ?, description = ?, requests_per_minute = ?, "
                + "requests_per_hour = ?, requests_per_day = ?, endpoint_overrides = ?, active = ?, "
                + "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            int updated;
            try (PreparedStatement statement = connection.prepareStatement(update)) {
                statement.setString(1, plan.getName());
                statement.setString(2, plan.getDescription());
                statement.setInt(3, plan.getDefaultLimit().requestsPerMinute());
                statement.setInt(4, plan.getDefaultLimit().requestsPerHour());
                statement.setInt(5, plan.getDefaultLimit().requestsPerDay());
                statement.setString(6, encode(plan.getEndpointOverrides()));
                statement.setBoolean(7, plan.isActive());
                statement.setString(8, plan.getId());
                updated = statement.executeUpdate();
            }
            if (updated == 0) {
                String insert = "INSERT INTO gateway_rate_plans "
                    + "(id, name, description, requests_per_minute, requests_per_hour, requests_per_day, endpoint_overrides, active) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(insert)) {
                statement.setString(1, plan.getId());
                statement.setString(2, plan.getName());
                statement.setString(3, plan.getDescription());
                statement.setInt(4, plan.getDefaultLimit().requestsPerMinute());
                statement.setInt(5, plan.getDefaultLimit().requestsPerHour());
                statement.setInt(6, plan.getDefaultLimit().requestsPerDay());
                statement.setString(7, encode(plan.getEndpointOverrides()));
                statement.setBoolean(8, plan.isActive());
                statement.executeUpdate();
                }
            }
            JdbcGatewayStore.audit(connection, "rate-plan", plan.getId(), "UPSERT");
            return plan;
        });
    }

    @Override
    public Uni<Boolean> deleteById(String id) {
        return store.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM gateway_rate_plans WHERE id = ?")) {
                statement.setString(1, id);
                boolean deleted = statement.executeUpdate() > 0;
                if (deleted) {
                    JdbcGatewayStore.audit(connection, "rate-plan", id, "DELETE");
                }
                return deleted;
            }
        });
    }

    @Override
    public Uni<Boolean> existsById(String id) {
        return findById(id).map(Optional::isPresent);
    }

    private RatePlan read(ResultSet resultSet) throws SQLException {
        RatePlan plan = new RatePlan(
            resultSet.getString("id"),
            resultSet.getString("name"),
            resultSet.getString("description"),
            RateLimit.of(
                resultSet.getInt("requests_per_minute"),
                resultSet.getInt("requests_per_hour"),
                resultSet.getInt("requests_per_day")));
        if (!resultSet.getBoolean("active")) {
            plan.deactivate();
        }
        try {
            Map<String, RateLimit> overrides = objectMapper.readValue(
                resultSet.getString("endpoint_overrides"), new TypeReference<>() {});
            overrides.forEach(plan::addEndpointOverride);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to decode rate plan overrides", e);
        }
        return plan;
    }

    private String encode(Map<String, RateLimit> overrides) {
        try {
            return objectMapper.writeValueAsString(overrides);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to encode rate plan overrides", e);
        }
    }
}
