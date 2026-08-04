package id.payu.transaction.adapter.persistence.entity;

import jakarta.persistence.Column;
import org.hibernate.annotations.ColumnTransformer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionEntityMappingTest {

    @Test
    void storesMetadataAsJsonbWithDatabaseCast() throws NoSuchFieldException {
        var metadata = TransactionEntity.class.getDeclaredField("metadata");

        assertThat(metadata.getAnnotation(Column.class).columnDefinition()).isEqualTo("jsonb");
        assertThat(metadata.getAnnotation(ColumnTransformer.class).write()).isEqualTo("?::jsonb");
    }
}
