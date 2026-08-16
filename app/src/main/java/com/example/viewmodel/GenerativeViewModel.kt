package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.BuildConfig

class GenerativeViewModel(
    private val generativeModel: GenerativeModel
) : ViewModel() {
    private val _uiState = MutableStateFlow<String>("")
    val uiState: StateFlow<String> = _uiState.asStateFlow()

    fun generateContent(prompt: String) {
        viewModelScope.launch {
            try {
                val response = generativeModel.generateContent(prompt)
                _uiState.value = response.text ?: "No response"
            } catch (e: Exception) {
                _uiState.value = "Error: ${e.localizedMessage}"
            }
        }
    }
}
