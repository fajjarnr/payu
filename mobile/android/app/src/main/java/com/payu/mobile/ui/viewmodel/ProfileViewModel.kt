package com.payu.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payu.mobile.data.repository.PayURepository
import com.payu.mobile.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: PayURepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    init {
        // For demo, set a mock user
        _uiState.value = _uiState.value.copy(
            user = User(
                id = "1",
                fullName = "Test User",
                email = "test@payu.id",
                phoneNumber = "08123456789"
            )
        )
    }
    
    fun logout() {
        repository.logout()
        _uiState.value = _uiState.value.copy(user = null)
    }
}

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
