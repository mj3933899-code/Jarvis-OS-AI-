package com.example.voice

import java.util.Locale

data class TtsVoiceModel(
    val id: String,
    val name: String,
    val locale: Locale,
    val isBengali: Boolean,
    val requiresNetwork: Boolean
)
