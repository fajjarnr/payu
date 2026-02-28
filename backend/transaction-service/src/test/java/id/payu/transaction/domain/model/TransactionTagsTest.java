package id.payu.transaction.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTagsTest {

    @Test
    void shouldSetAndGetTags() {
        Transaction transaction = new Transaction();
        String tagsJson = "[\"FOOD\", \"DINING\", \"WORK\"]";

        transaction.setTags(tagsJson);

        assertEquals(tagsJson, transaction.getTags());
    }

    @Test
    void shouldAllowNullTags() {
        Transaction transaction = new Transaction();

        transaction.setTags(null);

        assertNull(transaction.getTags());
    }

    @Test
    void shouldAllowEmptyTagsArray() {
        Transaction transaction = new Transaction();
        String emptyArray = "[]";

        transaction.setTags(emptyArray);

        assertEquals(emptyArray, transaction.getTags());
    }
}
