package com.example.data

enum class CallType {
    AUDIO, VIDEO
}

enum class CallStatus {
    IDLE,
    OUTGOING_RINGING,
    INCOMING_RINGING,
    CONNECTED,
    DISCONNECTED
}

data class CallSession(
    val contactId: String,
    val contactName: String,
    val contactPic: String,
    val callType: CallType,
    val status: CallStatus = CallStatus.IDLE,
    val durationSec: Int = 0,
    val audioQualityKbps: Int = 48, // Opus HD clarity status
    val videoResolution: String = "1080p @ 60fps Ultra HD", // Adaptive resolution scaling
    val latencyMs: Int = 18, // P2P Latency
    val batteryEfficiency: String = "battery_optimized", // Optimized profile status
    val encryptionFingerprint: String = "SEC-XXXX-XXXX-XXXX-XXXX",
    val isMuted: Boolean = false,
    val isVideoOff: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val filterMode: String = "Standard Clear" // Camera enhancement filters
)
