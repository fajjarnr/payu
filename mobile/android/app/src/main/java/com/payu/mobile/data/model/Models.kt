package com.payu.mobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: String,
    val name: String,
    val accountNumber: String,
    val type: String,
    val balance: Double,
    val currency: String
)

@Serializable
data class Card(
    val id: String,
    val cardNumber: String,
    val holderName: String,
    val expiry: String,
    val type: String,
    val isLocked: Boolean
)

@Serializable
data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String
) {
    val initials: String
        get() = fullName.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
}

@Serializable
data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,
    val date: String,
    val type: String,
    val status: String
)

@Serializable
data class BalanceResponse(
    val balance: Double,
    val available: Double
)

@Serializable
data class TransferRequest(
    val recipientAccount: String,
    val amount: Double,
    val note: String? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: User
)
