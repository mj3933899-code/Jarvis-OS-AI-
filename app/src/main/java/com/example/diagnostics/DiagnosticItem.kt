package com.example.diagnostics

data class DiagnosticItem(
    val title: String,
    val status: String,
    val isHealthy: Boolean,
    val details: String,
    val fixHint: String? = null
)
