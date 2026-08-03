package id.payu.gateway.adapter.persistence;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

@ApplicationScoped
public class JdbcGatewayStore {

    @Inject
    DataSource dataSource;

    public <T> Uni<T> query(SqlWork<T> work) {
        return Uni.createFrom().item(() -> {
            try (Connection connection = dataSource.getConnection()) {
                return work.apply(connection);
            } catch (SQLException e) {
                throw new IllegalStateException("Gateway database operation failed", e);
            }
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public static void setInstant(PreparedStatement statement, int index, Instant value) throws SQLException {
        statement.setTimestamp(index, value == null ? null : Timestamp.from(value));
    }

    public static Instant getInstant(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    public static void audit(Connection connection, String entityType, String entityId, String action)
        throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO gateway_configuration_audit (entity_type, entity_id, action, actor) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, entityType);
            statement.setString(2, entityId);
            statement.setString(3, action);
            statement.setString(4, "gateway-service");
            statement.executeUpdate();
        }
    }

    @FunctionalInterface
    public interface SqlWork<T> {
        T apply(Connection connection) throws SQLException;
    }
}
