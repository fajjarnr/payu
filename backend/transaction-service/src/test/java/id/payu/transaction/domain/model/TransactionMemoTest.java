package id.payu.transaction.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionMemoTest {

    @Test
    void shouldSetAndGetMemo() {
        TransactionEntity transaction = new TransactionEntity();
        String memo = "Bayar makan siang";

        transaction.setMemo(memo);

        assertEquals(memo, transaction.getMemo());
    }

    @Test
    void shouldAllowNullMemo() {
        TransactionEntity transaction = new TransactionEntity();

        transaction.setMemo(null);

        assertNull(transaction.getMemo());
    }

    @Test
    void shouldAllowEmptyMemo() {
        TransactionEntity transaction = new TransactionEntity();

        transaction.setMemo("");

        assertEquals("", transaction.getMemo());
    }

    @Test
    void shouldAllowLongMemoUpTo140Chars() {
        TransactionEntity transaction = new TransactionEntity();
        String longMemo = "a".repeat(140);

        transaction.setMemo(longMemo);

        assertEquals(140, transaction.getMemo().length());
    }
}
