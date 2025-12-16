package com.glassous.openqwens.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class HtmlPreviewViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _htmlContent = MutableStateFlow("")
    val htmlContent: StateFlow<String> = _htmlContent.asStateFlow()

    init {
        val encodedHtml = savedStateHandle.get<String>("htmlContent")
        if (!encodedHtml.isNullOrBlank()) {
            try {
                // Decode the HTML content
                // Note: In a production app with very large HTML, consider using a Singleton/Repository 
                // instead of navigation arguments to avoid TransactionTooLargeException.
                _htmlContent.value = URLDecoder.decode(encodedHtml, StandardCharsets.UTF_8.toString())
            } catch (e: Exception) {
                _htmlContent.value = "Error decoding HTML content: ${e.message}"
            }
        } else {
            // Fallback or check Singleton if implemented
            _htmlContent.value = HtmlContentHolder.content ?: "No content provided."
        }
    }
}

// Simple Singleton to pass large data safely
object HtmlContentHolder {
    var content: String? = null
}
