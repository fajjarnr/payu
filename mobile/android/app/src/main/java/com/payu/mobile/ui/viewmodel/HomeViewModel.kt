package com.payu.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payu.mobile.data.repository.PayURepository
import com.payu.mobile.data.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PayURepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    fun loadBalance() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getBalance()
                .onSuccess { balance ->
                    _uiState.value = _uiState.value.copy(
                        balance = balance,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
        }
    }
    
    fun loadTransactions() {
        viewModelScope.launch {
            repository.getAccounts()
                .onSuccess { accounts ->
                    // For demo, create mock transactions
                    val mockTransactions = listOf(
                        Transaction(
                            id = "1",
                            title = "Salary",
                            amount = 5000000.0,
                            date = "Today",
                            type = "credit",
                            status = "completed"
                        ),
                        Transaction(
                            id = "2",
                            title = "Electricity Bill",
                            amount = -250000.0,
                            date = "Yesterday",
                            type = "debit",
                            status = "completed"
                        ),
                        Transaction(
                            id = "3",
                            title = "Freelance",
                            amount = 1200000.0,
                            date = "Jan 20",
                            type = "credit",
                            status = "completed"
                        )
                    )
                    _uiState.value = _uiState.value.copy(transactions = mockTransactions)
                }
        }
    }
}

data class HomeUiState(
    val balance: Double = 0.0,
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
