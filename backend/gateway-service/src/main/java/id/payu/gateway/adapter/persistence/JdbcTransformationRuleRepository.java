package id.payu.gateway.adapter.persistence;

import id.payu.gateway.domain.entity.TransformationRule;
import id.payu.gateway.domain.repository.TransformationRuleRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JdbcTransformationRuleRepository implements TransformationRuleRepository {

    private static final String SELECT = "SELECT id, name, description, priority, active "
        + "FROM gateway_transformation_rules";

    @Inject
    JdbcGatewayStore store;

    @Override
    public Uni<Optional<TransformationRule>> findById(String id) {
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
    public Multi<TransformationRule> findAllActiveOrderedByPriority() {
        return find(" WHERE active = TRUE ORDER BY priority");
    }

    @Override
    public Multi<TransformationRule> findAll() {
        return find(" ORDER BY priority");
    }

    @Override
    public Multi<TransformationRule> findApplicableRules(String path, String method) {
        return findAllActiveOrderedByPriority()
            .filter(rule -> rule.matches(new TransformationRule.TransformationContext(
                path, method, java.util.Collections.emptyMap(), null)));
    }

    @Override
    public Uni<TransformationRule> save(TransformationRule rule) {
        return store.query(connection -> {
            saveOne(connection, rule);
            JdbcGatewayStore.audit(connection, "transformation-rule", rule.getId(), "UPSERT");
            return rule;
        });
    }

    @Override
    public Uni<Void> saveBatch(List<TransformationRule> rules) {
        return store.query(connection -> {
            for (TransformationRule rule : rules) {
                saveOne(connection, rule);
                JdbcGatewayStore.audit(connection, "transformation-rule", rule.getId(), "UPSERT");
            }
            return null;
        });
    }

    @Override
    public Uni<Boolean> deleteById(String id) {
        return store.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM gateway_transformation_rules WHERE id = ?")) {
                statement.setString(1, id);
                boolean deleted = statement.executeUpdate() > 0;
                if (deleted) {
                    JdbcGatewayStore.audit(connection, "transformation-rule", id, "DELETE");
                }
                return deleted;
            }
        });
    }

    @Override
    public Uni<Boolean> existsById(String id) {
        return findById(id).map(Optional::isPresent);
    }

    private Multi<TransformationRule> find(String suffix) {
        return store.query(connection -> {
            List<TransformationRule> rules = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(SELECT + suffix);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rules.add(read(resultSet));
                }
            }
            return rules;
        }).onItem().transformToMulti(Multi.createFrom()::iterable);
    }

    private TransformationRule read(ResultSet resultSet) throws SQLException {
        TransformationRule rule = new TransformationRule(
            resultSet.getString("id"),
            resultSet.getString("name"),
            resultSet.getString("description"),
            resultSet.getInt("priority"));
        if (!resultSet.getBoolean("active")) {
            rule.deactivate();
        }
        return rule;
    }

    private void saveOne(java.sql.Connection connection, TransformationRule rule) throws SQLException {
        String update = "UPDATE gateway_transformation_rules SET name = ?, description = ?, priority = ?, active = ?, "
            + "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        int updated;
        try (PreparedStatement statement = connection.prepareStatement(update)) {
            statement.setString(1, rule.getName());
            statement.setString(2, rule.getDescription());
            statement.setInt(3, rule.getPriority());
            statement.setBoolean(4, rule.isActive());
            statement.setString(5, rule.getId());
            updated = statement.executeUpdate();
        }
        if (updated == 0) {
            String insert = "INSERT INTO gateway_transformation_rules "
                + "(id, name, description, priority, active, conditions, actions) VALUES (?, ?, ?, ?, ?, '[]', '[]')";
            try (PreparedStatement statement = connection.prepareStatement(insert)) {
                statement.setString(1, rule.getId());
                statement.setString(2, rule.getName());
                statement.setString(3, rule.getDescription());
                statement.setInt(4, rule.getPriority());
                statement.setBoolean(5, rule.isActive());
                statement.executeUpdate();
            }
        }
    }
}
