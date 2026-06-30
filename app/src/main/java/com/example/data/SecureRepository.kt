package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlin.random.Random

class SecureRepository(private val db: AppDatabase) {
    val contactDao = db.contactDao()
    val messageDao = db.messageDao()
    val statusDao = db.statusDao()
    val profileDao = db.profileDao()
    val friendRequestDao = db.friendRequestDao()

    val allContacts: Flow<List<Contact>> = contactDao.getAllContacts()
    val myProfile: Flow<UserProfile?> = profileDao.getMyProfile()

    fun getMessagesWithContact(myId: String, contactId: String): Flow<List<Message>> {
        return messageDao.getMessagesWithContact(myId, contactId)
    }

    fun getActiveStatuses(): Flow<List<StatusUpdate>> {
        return statusDao.getActiveStatuses(System.currentTimeMillis())
    }

    suspend fun getMyProfileDirect(): UserProfile? {
        return profileDao.getMyProfileDirect()
    }

    suspend fun insertContact(contact: Contact) {
        contactDao.insertContact(contact)
    }

    suspend fun deleteContact(contact: Contact) {
        contactDao.deleteContact(contact)
    }

    suspend fun updateContact(contact: Contact) {
        contactDao.updateContact(contact)
    }

    suspend fun insertStatus(status: StatusUpdate) {
        statusDao.insertStatus(status)
    }

    suspend fun deleteExpiredData() {
        val now = System.currentTimeMillis()
        messageDao.deleteExpiredMessages(now)
        statusDao.deleteExpiredStatuses(now)
    }

    suspend fun markMessagesAsRead(myId: String, contactId: String) {
        messageDao.markMessagesAsRead(myId, contactId)
    }

    // High level secure text sender
    suspend fun sendSecureMessage(
        senderId: String,
        receiverId: String,
        plainText: String,
        isDisappearing: Boolean = false,
        disappearDurationSec: Int = 30
    ): Message {
        val sharedKey = SecureCrypto.generateSharedKey(senderId, receiverId)
        val (ciphertext, fingerprint) = SecureCrypto.encryptAES(plainText, sharedKey)
        
        val expiresAt = if (isDisappearing) {
            System.currentTimeMillis() + (disappearDurationSec * 1000)
        } else {
            0L
        }

        val message = Message(
            senderId = senderId,
            receiverId = receiverId,
            encryptedPayload = ciphertext,
            encryptionKeyFingerprint = fingerprint,
            timestamp = System.currentTimeMillis(),
            isDisappearing = isDisappearing,
            disappearDurationSec = disappearDurationSec,
            expiresAt = expiresAt,
            isSender = true
        )
        messageDao.insertMessage(message)
        return message
    }

    // High level secure media sender
    suspend fun sendSecureMedia(
        senderId: String,
        receiverId: String,
        mediaLabel: String, // e.g., "photo.jpg"
        isDisappearing: Boolean = false,
        disappearDurationSec: Int = 30
    ): Message {
        val sharedKey = SecureCrypto.generateSharedKey(senderId, receiverId)
        val (ciphertext, fingerprint) = SecureCrypto.encryptAES("📷 Secure Photo Transfer: $mediaLabel", sharedKey)

        val expiresAt = if (isDisappearing) {
            System.currentTimeMillis() + (disappearDurationSec * 1000)
        } else {
            0L
        }

        val message = Message(
            senderId = senderId,
            receiverId = receiverId,
            encryptedPayload = ciphertext,
            encryptionKeyFingerprint = fingerprint,
            mediaUri = mediaLabel,
            timestamp = System.currentTimeMillis(),
            isDisappearing = isDisappearing,
            disappearDurationSec = disappearDurationSec,
            expiresAt = expiresAt,
            isSender = true
        )
        messageDao.insertMessage(message)
        return message
    }

    // Insert incoming message
    suspend fun receiveSecureMessage(
        senderId: String, // from contact
        receiverId: String, // my id
        plainText: String,
        isDisappearing: Boolean = false,
        disappearDurationSec: Int = 30
    ): Message {
        val sharedKey = SecureCrypto.generateSharedKey(senderId, receiverId)
        val (ciphertext, fingerprint) = SecureCrypto.encryptAES(plainText, sharedKey)

        val expiresAt = if (isDisappearing) {
            System.currentTimeMillis() + (disappearDurationSec * 1000)
        } else {
            0L
        }

        val message = Message(
            senderId = senderId,
            receiverId = receiverId,
            encryptedPayload = ciphertext,
            encryptionKeyFingerprint = fingerprint,
            timestamp = System.currentTimeMillis(),
            isDisappearing = isDisappearing,
            disappearDurationSec = disappearDurationSec,
            expiresAt = expiresAt,
            isSender = false
        )
        messageDao.insertMessage(message)
        return message
    }

    suspend fun updateProfile(profile: UserProfile) {
        profileDao.insertProfile(profile)
    }

    // Seed database with empty, clean initial user profile state
    suspend fun seedInitialData() {
        val existingProfile = profileDao.getMyProfileDirect()
        val myId = existingProfile?.mySecureId ?: "SEC-814-297-ZPH"
        
        if (existingProfile == null) {
            profileDao.insertProfile(
                UserProfile(
                    mySecureId = myId,
                    name = "Security Agent",
                    profilePicUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    profileVisibility = "EVERYONE",
                    onlineStatusVisibility = true,
                    isDarkMode = false
                )
            )
        }
    }
}
