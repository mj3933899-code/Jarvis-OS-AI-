package com.example.service

data class ReceivedNotification(
    val id: Int,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long
)
