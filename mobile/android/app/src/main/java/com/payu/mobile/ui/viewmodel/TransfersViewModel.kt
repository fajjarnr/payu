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
class TransfersViewModel @Inject constructor(
    private val repository: PayURepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TransfersUiState())
    val uiState: StateFlow<TransfersUiState> = _uiState.asStateFlow()
    
    fun performTransfer(recipient: String, amount: Double, note: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            repository.transfer(recipient, amount, note)
                .onSuccess { transaction ->
                    _uiState.value = _uiState.value.copy(
                        transferResult = Result.success(transaction),
                        isProcessing = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message,
                        isProcessing = false
                    )
                }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class TransfersUiState(
    val transferResult: Result<Transaction>? = null,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)
