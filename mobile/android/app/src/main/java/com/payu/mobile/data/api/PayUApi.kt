package com.payu.mobile.data.api

import com.payu.mobile.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface PayUApi {
    @GET("accounts/balance")
    suspend fun getBalance(): BalanceResponse
    
    @GET("accounts")
    suspend fun getAccounts(): List<Account>
    
    @GET("cards")
    suspend fun getCards(): List<Card>
    
    @GET("transactions")
    suspend fun getTransactions(): List<Transaction>
    
    @POST("transfers")
    suspend fun transfer(@Body request: TransferRequest): Transaction
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse
}
