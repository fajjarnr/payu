package id.payu.transaction.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionMemoTest {

    @Test
    void shouldSetAndGetMemo() {
        Transaction transaction = new Transaction();
        String memo = "Bayar makan siang";

        transaction.setMemo(memo);

        assertEquals(memo, transaction.getMemo());
    }

    @Test
    void shouldAllowNullMemo() {
        Transaction transaction = new Transaction();

        transaction.setMemo(null);

        assertNull(transaction.getMemo());
    }

    @Test
    void shouldAllowEmptyMemo() {
        Transaction transaction = new Transaction();

        transaction.setMemo("");

        assertEquals("", transaction.getMemo());
    }

    @Test
    void shouldAllowLongMemoUpTo140Chars() {
        Transaction transaction = new Transaction();
        String longMemo = "a".repeat(140);

        transaction.setMemo(longMemo);

        assertEquals(140, transaction.getMemo().length());
    }
}
