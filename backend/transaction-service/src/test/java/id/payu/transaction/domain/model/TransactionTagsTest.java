package id.payu.transaction.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTagsTest {

    @Test
    void shouldSetAndGetTags() {
        TransactionEntity transaction = new TransactionEntity();
        String tagsJson = "[\"FOOD\", \"DINING\", \"WORK\"]";

        transaction.setTags(tagsJson);

        assertEquals(tagsJson, transaction.getTags());
    }

    @Test
    void shouldAllowNullTags() {
        TransactionEntity transaction = new TransactionEntity();

        transaction.setTags(null);

        assertNull(transaction.getTags());
    }

    @Test
    void shouldAllowEmptyTagsArray() {
        TransactionEntity transaction = new TransactionEntity();
        String emptyArray = "[]";

        transaction.setTags(emptyArray);

        assertEquals(emptyArray, transaction.getTags());
    }
}
