package com.payu.mobile

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.payu.mobile.data.repository.TokenManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TokenManagerTest {
    
    private lateinit var tokenManager: TokenManager
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        tokenManager = TokenManager(context)
        runBlocking {
            tokenManager.clearToken()
        }
    }
    
    @Test
    fun `saveToken should store token correctly`() = runTest {
        val token = "test_token_123"
        tokenManager.saveToken(token)
        
        tokenManager.authToken.collect { savedToken ->
            assertEquals(token, savedToken)
        }
    }
    
    @Test
    fun `clearToken should remove stored token`() = runTest {
        tokenManager.saveToken("test_token")
        tokenManager.clearToken()
        
        tokenManager.authToken.collect { savedToken ->
            assertNull(savedToken)
        }
    }
}
