package com.payu.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.payu.mobile.ui.viewmodel.TransfersViewModel

@Composable
fun TransfersScreen(viewModel: TransfersViewModel = hiltViewModel()) {
    var recipient by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("Transfer") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = recipient,
                onValueChange = { recipient = it },
                label = { Text("Account Number / Phone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (IDR)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Button(
                onClick = {
                    amount.toDoubleOrNull()?.let { transferAmount ->
                        viewModel.performTransfer(recipient, transferAmount, note.ifEmpty { null })
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = recipient.isNotBlank() && amount.isNotBlank() && !uiState.isProcessing
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Send Transfer")
                }
            }
        }
    }
    
    LaunchedEffect(uiState.transferResult) {
        if (uiState.transferResult?.isSuccess == true) {
            recipient = ""
            amount = ""
            note = ""
        }
    }
    
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            viewModel.clearError()
        }
    }
    
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            viewModel.clearError()
        }
    }
}
