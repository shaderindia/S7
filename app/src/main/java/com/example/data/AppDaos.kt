package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: String): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Update
    suspend fun updateContact(contact: Contact)

    @Delete
    suspend fun deleteContact(contact: Contact)

    @Query("UPDATE contacts SET onlineStatus = :status, lastActive = :timestamp WHERE id = :id")
    suspend fun updateOnlineStatus(id: String, status: String, timestamp: Long)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE (senderId = :myId AND receiverId = :contactId) OR (senderId = :contactId AND receiverId = :myId) ORDER BY timestamp ASC")
    fun getMessagesWithContact(myId: String, contactId: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Int)

    @Query("DELETE FROM messages WHERE expiresAt > 0 AND expiresAt < :currentTime")
    suspend fun deleteExpiredMessages(currentTime: Long): Int

    @Query("UPDATE messages SET isRead = 1 WHERE senderId = :contactId AND receiverId = :myId")
    suspend fun markMessagesAsRead(myId: String, contactId: String)
}

@Dao
interface StatusDao {
    @Query("SELECT * FROM statuses WHERE expiresAt > :currentTime ORDER BY timestamp DESC")
    fun getActiveStatuses(currentTime: Long): Flow<List<StatusUpdate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: StatusUpdate)

    @Query("DELETE FROM statuses WHERE expiresAt < :currentTime")
    suspend fun deleteExpiredStatuses(currentTime: Long): Int
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getMyProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getMyProfileDirect(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Update
    suspend fun updateProfile(profile: UserProfile)
}

@Dao
interface FriendRequestDao {
    @Query("SELECT * FROM friend_requests ORDER BY timestamp DESC")
    fun getAllPendingRequests(): Flow<List<FriendRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: FriendRequest)

    @Delete
    suspend fun deleteRequest(request: FriendRequest)

    @Query("DELETE FROM friend_requests WHERE id = :id")
    suspend fun deleteRequestById(id: String)
}

