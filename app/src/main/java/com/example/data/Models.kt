package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey val id: String, // Special Secure ID, e.g., SEC-894-321-XYZ
    val name: String,
    val profilePicUrl: String,
    val onlineStatus: String = "offline", // online, offline, idle
    val lastActive: Long = System.currentTimeMillis(),
    val profileVisibility: String = "EVERYONE" // EVERYONE, CONTACTS_ONLY, NOBODY
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: String,
    val receiverId: String,
    val encryptedPayload: String, // The actual encrypted ciphertext base64
    val encryptionKeyFingerprint: String, // Verification code shown in UI
    val mediaUri: String? = null, // Path or name of secure media file
    val timestamp: Long = System.currentTimeMillis(),
    val isDisappearing: Boolean = false,
    val disappearDurationSec: Int = 30, // Default 30s disappearing time if enabled
    val expiresAt: Long = 0L, // Timestamp when message should vanish
    val isRead: Boolean = false,
    val isSender: Boolean = true // Flag to easily distinguish local send vs receive
)

@Entity(tableName = "statuses")
data class StatusUpdate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val userName: String,
    val statusText: String,
    val mediaUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000 // Disappears in 24 hours
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Singleton profile
    val mySecureId: String,
    val name: String,
    val profilePicUrl: String,
    val profileVisibility: String = "EVERYONE", // EVERYONE, CONTACTS_ONLY, NOBODY
    val onlineStatusVisibility: Boolean = true,
    val isDarkMode: Boolean = false
)

@Entity(tableName = "friend_requests")
data class FriendRequest(
    @PrimaryKey val id: String, // Secure ID of the peer
    val name: String,
    val profilePicUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)

