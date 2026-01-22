package com.payu.mobile

import com.payu.mobile.data.model.Account
import com.payu.mobile.data.model.Card
import com.payu.mobile.data.model.User
import org.junit.Assert.*
import org.junit.Test

class ModelTest {
    
    @Test
    fun `account model should have correct properties`() {
        val account = Account(
            id = "1",
            name = "Primary Account",
            accountNumber = "1234567890",
            type = "savings",
            balance = 5000000.0,
            currency = "IDR"
        )
        
        assertEquals("1", account.id)
        assertEquals("Primary Account", account.name)
        assertEquals(5000000.0, account.balance, 0.001)
        assertEquals("IDR", account.currency)
    }
    
    @Test
    fun `card model should have correct properties`() {
        val card = Card(
            id = "1",
            cardNumber = "**** **** **** 1234",
            holderName = "John Doe",
            expiry = "12/27",
            type = "virtual",
            isLocked = false
        )
        
        assertEquals("1", card.id)
        assertEquals("virtual", card.type)
        assertFalse(card.isLocked)
    }
    
    @Test
    fun `user model should calculate initials correctly`() {
        val user = User(
            id = "1",
            fullName = "John Doe",
            email = "john@example.com",
            phoneNumber = "08123456789"
        )
        
        assertEquals("JD", user.initials)
    }
    
    @Test
    fun `user model with single name should calculate initials correctly`() {
        val user = User(
            id = "1",
            fullName = "John",
            email = "john@example.com",
            phoneNumber = "08123456789"
        )
        
        assertEquals("J", user.initials)
    }
}
