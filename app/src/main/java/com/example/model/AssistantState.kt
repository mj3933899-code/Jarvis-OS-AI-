package com.example.model

enum class AssistantState(val labelEn: String, val labelBn: String) {
    OFF("OFF", "বন্ধ"),
    STARTING("STARTING", "শুরু হচ্ছে"),
    ACTIVE("ACTIVE", "সক্রিয়"),
    LISTENING("LISTENING", "শুনছি..."),
    PROCESSING("PROCESSING", "প্রসেসিং হচ্ছে"),
    PLANNING("PLANNING", "পরিকল্পনা করছি"),
    EXECUTING("EXECUTING", "কাজ সম্পাদন করছি"),
    VERIFYING("VERIFYING", "যাচাই করছি"),
    RECOVERING("RECOVERING", "পুনরুদ্ধার করছি"),
    SPEAKING("SPEAKING", "কথা বলছি"),
    PAUSED("PAUSED", "স্থগিত"),
    STOPPING("STOPPING", "থামানো হচ্ছে"),
    ERROR("ERROR", "ত্রুটি")
}

enum class TaskStatus {
    QUEUED,
    PLANNING,
    RUNNING,
    PAUSED,
    WAITING_PERMISSION,
    WAITING_USER,
    TESTING,
    FIXING,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class ActionRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class VerificationStatus {
    CONFIRMED,
    LIKELY,
    UNCERTAIN,
    FAILED,
    NOT_AVAILABLE
}

enum class VoiceMatchStatus {
    OFF,
    ENROLLING,
    READY,
    VERIFYING,
    VERIFIED,
    REJECTED,
    ERROR
}

enum class ProviderStatus {
    NOT_CONFIGURED,
    CONFIGURED,
    TESTING,
    CONNECTED,
    ERROR,
    RATE_LIMITED,
    UNAVAILABLE
}
