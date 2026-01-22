package com.payu.mobile.data.repository

import com.payu.mobile.data.api.PayUApi
import com.payu.mobile.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PayURepository @Inject constructor(
    private val api: PayUApi,
    private val tokenManager: TokenManager
) {
    suspend fun getBalance(): Result<Double> {
        return try {
            val response = api.getBalance()
            Result.success(response.balance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAccounts(): Result<List<Account>> {
        return try {
            val accounts = api.getAccounts()
            Result.success(accounts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getCards(): Result<List<Card>> {
        return try {
            val cards = api.getCards()
            Result.success(cards)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun transfer(recipient: String, amount: Double, note: String?): Result<Transaction> {
        return try {
            val transaction = api.transfer(TransferRequest(recipient, amount, note))
            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            tokenManager.saveToken(response.accessToken)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun logout() {
        tokenManager.clearToken()
    }
}
