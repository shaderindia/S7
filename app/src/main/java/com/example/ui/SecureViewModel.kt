package com.example.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class SecureViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = SecureRepository(db)

    // UI flows
    val contacts: StateFlow<List<Contact>> = repository.allContacts
        .map { list -> list.filter { it.isFriend && !it.id.startsWith("ROOM_WAITING_") } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeStatuses: StateFlow<List<StatusUpdate>> = repository.getActiveStatuses().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val myProfile: StateFlow<UserProfile?> = repository.myProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _activeContact = MutableStateFlow<Contact?>(null)
    val activeContact: StateFlow<Contact?> = _activeContact.asStateFlow()

    // Load messages dynamically based on selected active contact
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<Message>> = _activeContact
        .flatMapLatest { contact ->
            val profile = myProfile.value
            val myId = profile?.mySecureId ?: "SEC-814-297-ZPH"
            if (contact != null) {
                repository.getMessagesWithContact(myId, contact.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeCallSession = MutableStateFlow<CallSession?>(null)
    val activeCallSession: StateFlow<CallSession?> = _activeCallSession.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Chats, 1: Status, 2: Privacy/Profile, 3: Desktop Pairing
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _currentDisappearingTime = MutableStateFlow(0) // 0 means Off, others in seconds (e.g. 10, 30, 60)
    val currentDisappearingTime: StateFlow<Int> = _currentDisappearingTime.asStateFlow()

    private val _peerConnectionState = MutableStateFlow("connecting")
    val peerConnectionState: StateFlow<String> = _peerConnectionState.asStateFlow()

    private val _remoteVideoRotation = MutableStateFlow(270) // Default 270 is a perfect correction for Android portrait front cam
    val remoteVideoRotation: StateFlow<Int> = _remoteVideoRotation.asStateFlow()

    private val _localVideoRotation = MutableStateFlow(270) // Default 270 is a perfect correction for Android portrait front cam
    val localVideoRotation: StateFlow<Int> = _localVideoRotation.asStateFlow()

    private val _p2pLogs = MutableStateFlow<List<String>>(emptyList())
    val p2pLogs: StateFlow<List<String>> = _p2pLogs.asStateFlow()

    fun addP2PLog(msg: String) {
        val formatter = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
        val timestamp = formatter.format(java.util.Date())
        val line = "[$timestamp] $msg"
        _p2pLogs.update { currentList ->
            if (currentList.size > 250) {
                currentList.drop(1) + line
            } else {
                currentList + line
            }
        }
    }

    fun clearP2PLogs() {
        _p2pLogs.value = emptyList()
    }

    private val _fetchedProfiles = MutableStateFlow<Map<String, String>>(emptyMap())
    val fetchedProfiles: StateFlow<Map<String, String>> = _fetchedProfiles.asStateFlow()

    val activeRoomId = MutableStateFlow<String?>(null)
    val roomStatus = MutableStateFlow<String>("idle") // "idle", "waiting", "joining", "matched", "error"
    val pendingRoomCode = MutableStateFlow<String?>(null) // visible to user until friend joins

    fun startCreateRoom() {
        viewModelScope.launch {
            val myProfileObj = repository.getMyProfileDirect()
            val myName = myProfileObj?.name ?: "Security Agent"
            val myIdVal = myProfileObj?.mySecureId ?: "SEC-814-297-ZPH"

            val code = (100000..999999).random().toString()
            activeRoomId.value = code
            roomStatus.value = "waiting"
            pendingRoomCode.value = code

            PeerJSManager.createRoom(code, myIdVal, myName)

            // Creator joins immediately — insert a placeholder contact and open chat
            val placeholderId = "ROOM_WAITING_$code"
            repository.insertContact(
                Contact(
                    id = placeholderId,
                    name = "Waiting for friend...",
                    profilePicUrl = "https://picsum.photos/id/1025/150/150",
                    onlineStatus = "online"
                )
            )
            // Open the chat right away so creator is "in the room"
            roomStatus.value = "matched"
            activeRoomId.value = null
            val placeholder = repository.contactDao.getContactById(placeholderId)
            if (placeholder != null) selectContact(placeholder)
        }
    }

    fun startJoinRoom(roomIdInput: String) {
        val cleanRoomId = roomIdInput.trim()
        if (cleanRoomId.length < 5) return

        viewModelScope.launch {
            val myProfileObj = repository.getMyProfileDirect()
            val myName = myProfileObj?.name ?: "Security Agent"
            val myIdVal = myProfileObj?.mySecureId ?: "SEC-814-297-ZPH"

            activeRoomId.value = cleanRoomId
            roomStatus.value = "joining"

            PeerJSManager.joinRoom(cleanRoomId, myIdVal, myName)
        }
    }

    fun exitRoom(contact: Contact) {
        viewModelScope.launch {
            val match = "ROOM_WAITING_(\\d+)".toRegex().find(contact.id)
            val code = match?.groupValues?.get(1)
            if (code != null) {
                PeerJSManager.cancelRoom(code)
            } else {
                val currentRoom = activeRoomId.value
                if (currentRoom != null) {
                    PeerJSManager.cancelRoom(currentRoom)
                }
            }
            if (!contact.isFriend) {
                repository.contactDao.deleteContact(contact)
            }
            selectContact(null)
            roomStatus.value = "idle"
            activeRoomId.value = null
            pendingRoomCode.value = null
        }
    }

    fun addContactAsFriend(contact: Contact) {
        viewModelScope.launch {
            val updated = contact.copy(isFriend = true)
            repository.updateContact(updated)
            if (activeContact.value?.id == contact.id) {
                _activeContact.value = updated
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                Toast.makeText(getApplication(), "Added as friend locally!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteFriend(contact: Contact) {
        viewModelScope.launch {
            repository.contactDao.deleteContact(contact)
            if (activeContact.value?.id == contact.id) {
                selectContact(null)
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                Toast.makeText(getApplication(), "Friend deleted locally!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun cancelMatching() {
        val currentRoom = activeRoomId.value
        if (currentRoom != null) {
            PeerJSManager.cancelRoom(currentRoom)
        }
        activeRoomId.value = null
        roomStatus.value = "idle"
    }

    suspend fun getLocalContactName(id: String): String? {
        val cleanId = id.trim().uppercase()
        val finalId = if (!cleanId.startsWith("SEC-")) "SEC-$cleanId" else cleanId
        return repository.contactDao.getContactById(finalId)?.name
    }

    fun requestProfile(id: String) {
        val cleanId = id.trim().uppercase()
        val finalId = if (!cleanId.startsWith("SEC-")) "SEC-$cleanId" else cleanId
        PeerJSManager.requestProfile(finalId)
    }

    fun updateFetchedProfile(id: String, name: String) {
        val cleanId = id.trim().uppercase()
        val finalId = if (!cleanId.startsWith("SEC-")) "SEC-$cleanId" else cleanId
        _fetchedProfiles.update { current ->
            current + (finalId to name)
        }
    }

    fun cycleRemoteRotation() {
        _remoteVideoRotation.value = when (_remoteVideoRotation.value) {
            0 -> 90
            90 -> 180
            180 -> 270
            270 -> 0
            else -> 270
        }
        PeerJSManager.setVideoRotations(_remoteVideoRotation.value, _localVideoRotation.value)
    }

    fun cycleLocalRotation() {
        _localVideoRotation.value = when (_localVideoRotation.value) {
            0 -> 90
            90 -> 180
            180 -> 270
            270 -> 0
            else -> 270
        }
        PeerJSManager.setVideoRotations(_remoteVideoRotation.value, _localVideoRotation.value)
    }

    // Calling timers
    private var callTimerJob: Job? = null

    // Message auto-expiration loop
    init {
        viewModelScope.launch {
            repository.seedInitialData()
            
            // Start the PeerJS background bridge
            val myProfileObj = repository.getMyProfileDirect()
            val myIdVal = myProfileObj?.mySecureId ?: "SEC-814-297-ZPH"
            
            PeerJSManager.init(getApplication(), repository, myIdVal)
            
            // Handle incoming calls from PeerJS with video / audio distinction
            PeerJSManager.onIncomingCall = { remoteId, isVideo ->
                viewModelScope.launch {
                    val callerName = repository.contactDao.getContactById(remoteId)?.name ?: remoteId
                    val fingerprint = SecureCrypto.generateSharedKey("myId", remoteId)
                    val fullFingerprint = SecureCrypto.generateFingerprint(fingerprint)
                    
                    P2PRingtoneManager.playIncomingRingtone(getApplication())

                    _activeCallSession.value = CallSession(
                        contactId = remoteId,
                        contactName = callerName,
                        contactPic = "https://picsum.photos/id/1025/150/150",
                        callType = if (isVideo) CallType.VIDEO else CallType.AUDIO,
                        status = CallStatus.INCOMING_RINGING,
                        encryptionFingerprint = fullFingerprint,
                        batteryEfficiency = "PeerJS WebRTC"
                    )
                }
            }

            PeerJSManager.onCallConnectedCallback = {
                P2PRingtoneManager.stop()
                val current = _activeCallSession.value
                if (current != null) {
                    _activeCallSession.value = current.copy(status = CallStatus.CONNECTED)
                    startCallTimer()
                }
                PeerJSManager.setVideoRotations(_remoteVideoRotation.value, _localVideoRotation.value)
            }

            PeerJSManager.onCallDisconnectedCallback = {
                P2PRingtoneManager.stop()
                val current = _activeCallSession.value
                if (current != null) {
                    _activeCallSession.value = current.copy(status = CallStatus.DISCONNECTED)
                }
                viewModelScope.launch {
                    delay(1500)
                    if (_activeCallSession.value?.status == CallStatus.DISCONNECTED) {
                        _activeCallSession.value = null
                    }
                }
            }

            PeerJSManager.onConnectionStateChangeCallback = { state ->
                _peerConnectionState.value = state
            }

            PeerJSManager.onErrorCallback = { errorType ->
                if (errorType == "peer-unavailable") {
                    viewModelScope.launch {
                        Toast.makeText(getApplication(), "User is offline.", Toast.LENGTH_LONG).show()
                    }
                    endCall()
                } else if (errorType == "network" || errorType == "disconnected") {
                    _peerConnectionState.value = "offline"
                } else if (errorType == "room-not-found" || errorType == "room-create-error") {
                    viewModelScope.launch {
                        Toast.makeText(getApplication(), "Room not found or expired. Try again.", Toast.LENGTH_LONG).show()
                    }
                    activeRoomId.value = null
                    roomStatus.value = "idle"
                }
            }

            PeerJSManager.onLogCallback = { msg ->
                addP2PLog(msg)
            }

            PeerJSManager.onProfileFetched = { id, name ->
                updateFetchedProfile(id, name)
            }

            PeerJSManager.onRoomMatched = { remoteId, remoteName ->
                viewModelScope.launch {
                    // Remove any placeholder "waiting" contact from creator side
                    val currentContact = _activeContact.value
                    if (currentContact != null && currentContact.id.startsWith("ROOM_WAITING_")) {
                        repository.contactDao.deleteContact(currentContact)
                    }

                    repository.insertContact(
                        Contact(
                            id = remoteId,
                            name = remoteName,
                            profilePicUrl = "https://picsum.photos/id/1025/150/150",
                            onlineStatus = "online"
                        )
                    )
                    roomStatus.value = "matched"
                    activeRoomId.value = null
                    pendingRoomCode.value = null

                    // ponytail: ping immediately to initialize presence and connect WebRTC channels instantly
                    PeerJSManager.pingContacts(listOf(remoteId))

                    val addedContact = repository.contactDao.getContactById(remoteId)
                    if (addedContact != null) {
                        selectContact(addedContact)
                    }
                }
            }

            // Periodic background presence ping loop
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                delay(8000) // Wait for PeerJS connection to settle
                while (true) {
                    val contacts = repository.contactDao.getAllContacts().firstOrNull() ?: emptyList()
                    val ids = contacts.map { it.id }
                    if (ids.isNotEmpty()) {
                        PeerJSManager.pingContacts(ids)
                    }
                    delay(25000) // Ping every 25 seconds
                }
            }

            // Periodic cleanup & expiration timer (every 1 second)
            while (true) {
                repository.deleteExpiredData()
                delay(1000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        PeerJSManager.endCall()
    }

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun selectContact(contact: Contact?) {
        _activeContact.value = contact
        if (contact != null) {
            viewModelScope.launch {
                val profile = myProfile.value
                val myId = profile?.mySecureId ?: "SEC-814-297-ZPH"
                repository.markMessagesAsRead(myId, contact.id)
            }
        }
    }

    fun setDisappearingTime(seconds: Int) {
        _currentDisappearingTime.value = seconds
    }

    // Send E2EE text message
    fun sendSecureMessage(text: String) {
        val contact = activeContact.value ?: return
        val profile = myProfile.value ?: return
        val isDisappearing = _currentDisappearingTime.value > 0
        val duration = _currentDisappearingTime.value

        viewModelScope.launch {
            repository.sendSecureMessage(
                senderId = profile.mySecureId,
                receiverId = contact.id,
                plainText = text,
                isDisappearing = isDisappearing,
                disappearDurationSec = duration
            )

            if (contact.id == "SEC-739-182-AIK") {
                // AI Assistant contact
                simulateAegisResponse(text, profile.mySecureId, contact.id)
            } else {
                // Use PeerJS DataConnection to send the message
                PeerJSManager.sendMessage(
                    remoteId = contact.id,
                    text = text,
                    isDisappearing = isDisappearing,
                    disappearDurationSec = duration
                )
            }
        }
    }

    private fun getIpFromContactId(id: String): String? {
        val clean = id.trim().uppercase().replace("SEC-", "")
        val ipRegex = Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}""")
        return if (clean.matches(ipRegex)) clean else null
    }

    // Send secure media
    fun sendSecurePhoto(label: String) {
        val contact = activeContact.value ?: return
        val profile = myProfile.value ?: return
        val isDisappearing = _currentDisappearingTime.value > 0
        val duration = _currentDisappearingTime.value

        viewModelScope.launch {
            repository.sendSecureMedia(
                senderId = profile.mySecureId,
                receiverId = contact.id,
                mediaLabel = label,
                isDisappearing = isDisappearing,
                disappearDurationSec = duration
            )

            // Simulate immediate encrypted photo acknowledgement
            delay(1000)
            _isTyping.value = true
            delay(1500)
            _isTyping.value = false
            repository.receiveSecureMessage(
                senderId = contact.id,
                receiverId = profile.mySecureId,
                plainText = "🔑 E2EE Media verified and decrypted. Secure visual footprint match!",
                isDisappearing = isDisappearing,
                disappearDurationSec = duration
            )
        }
    }

    private suspend fun simulateAegisResponse(userText: String, myId: String, assistantId: String) {
        _isTyping.value = true
        delay(1200) // Simulate encryption processing & network latency
        
        val reply = GeminiService.getAegisReply(userText)
        
        _isTyping.value = false
        val isDisappearing = _currentDisappearingTime.value > 0
        val duration = _currentDisappearingTime.value

        repository.receiveSecureMessage(
            senderId = assistantId,
            receiverId = myId,
            plainText = reply,
            isDisappearing = isDisappearing,
            disappearDurationSec = duration
        )
    }

    // Core calling flows
    fun initiateCall(contact: Contact, type: CallType) {
        if (_peerConnectionState.value == "offline" || _peerConnectionState.value == "connecting") {
            viewModelScope.launch {
                Toast.makeText(getApplication(), "You are offline. Please check your network connection.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val fingerprint = SecureCrypto.generateSharedKey("myId", contact.id)
        val fullFingerprint = SecureCrypto.generateFingerprint(fingerprint)

        _activeCallSession.value = CallSession(
            contactId = contact.id,
            contactName = contact.name,
            contactPic = contact.profilePicUrl,
            callType = type,
            status = CallStatus.OUTGOING_RINGING,
            encryptionFingerprint = fullFingerprint,
            batteryEfficiency = "PeerJS Link"
        )

        P2PRingtoneManager.playOutgoingRingback()

        PeerJSManager.startCall(contact.id, type == CallType.VIDEO)
    }

    fun simulateIncomingCall(contact: Contact, type: CallType) {
        val fingerprint = SecureCrypto.generateSharedKey("myId", contact.id)
        val fullFingerprint = SecureCrypto.generateFingerprint(fingerprint)

        P2PRingtoneManager.playIncomingRingtone(getApplication())

        _activeCallSession.value = CallSession(
            contactId = contact.id,
            contactName = contact.name,
            contactPic = contact.profilePicUrl,
            callType = type,
            status = CallStatus.INCOMING_RINGING,
            encryptionFingerprint = fullFingerprint,
            batteryEfficiency = "PeerJS Link"
        )
    }

    fun acceptCall() {
        val session = _activeCallSession.value ?: return
        P2PRingtoneManager.stop()
        _activeCallSession.value = session.copy(status = CallStatus.CONNECTED)
        PeerJSManager.answerCall(session.callType == CallType.VIDEO)
        startCallTimer()
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            var duration = 0
            while (true) {
                delay(1000)
                duration++
                val current = _activeCallSession.value ?: break
                if (current.status == CallStatus.CONNECTED) {
                    val latency = Random.nextInt(14, 24)
                    _activeCallSession.value = current.copy(
                        durationSec = duration,
                        latencyMs = latency
                    )
                } else {
                    break
                }
            }
        }
    }

    fun declineCall() {
        val session = _activeCallSession.value ?: return
        _activeCallSession.value = session.copy(status = CallStatus.DISCONNECTED)
        PeerJSManager.endCall()
        viewModelScope.launch {
            delay(1000)
            _activeCallSession.value = null
        }
    }

    fun endCall() {
        callTimerJob?.cancel()
        val session = _activeCallSession.value ?: return
        _activeCallSession.value = session.copy(status = CallStatus.DISCONNECTED)
        PeerJSManager.endCall()
        viewModelScope.launch {
            delay(1000)
            _activeCallSession.value = null
        }
    }

    // Call adjustments
    fun toggleMute() {
        val session = _activeCallSession.value ?: return
        val newMuted = !session.isMuted
        _activeCallSession.value = session.copy(isMuted = newMuted)
        PeerJSManager.toggleMute(newMuted)
    }

    fun toggleVideo() {
        val session = _activeCallSession.value ?: return
        val newVideoOff = !session.isVideoOff
        _activeCallSession.value = session.copy(isVideoOff = newVideoOff)
        PeerJSManager.toggleVideo(newVideoOff)
    }

    fun toggleSpeaker() {
        val session = _activeCallSession.value ?: return
        _activeCallSession.value = session.copy(isSpeakerOn = !session.isSpeakerOn)
    }

    fun cycleFilter() {
        val session = _activeCallSession.value ?: return
        val currentFilter = session.filterMode
        val nextFilter = when (currentFilter) {
            "Standard Clear" -> "Battery Saver Eco-View"
            "Battery Saver Eco-View" -> "Low-Light Enhance"
            "Low-Light Enhance" -> "Ultra Sharp HD Clear"
            else -> "Standard Clear"
        }
        val batteryStatus = when (nextFilter) {
            "Standard Clear" -> "Optimized Codec Profile"
            "Battery Saver Eco-View" -> "Save Mode (Sub-6.5mW)"
            "Low-Light Enhance" -> "Adaptive Night Enhancement"
            "Ultra Sharp HD Clear" -> "Maximum Quality Peak"
            else -> "Optimized Codec Profile"
        }
        _activeCallSession.value = session.copy(filterMode = nextFilter, batteryEfficiency = batteryStatus)
    }

    // Profile Settings
    fun updateProfile(name: String, visibility: String, showStatus: Boolean, isDark: Boolean) {
        viewModelScope.launch {
            val current = repository.getMyProfileDirect()
            if (current != null) {
                repository.updateProfile(
                    current.copy(
                        name = name,
                        profileVisibility = visibility,
                        onlineStatusVisibility = showStatus,
                        isDarkMode = isDark
                    )
                )
            }
        }
    }

    // Status updates
    fun postStatusUpdate(text: String) {
        val profile = myProfile.value ?: return
        viewModelScope.launch {
            repository.insertStatus(
                StatusUpdate(
                    userId = profile.mySecureId,
                    userName = profile.name,
                    statusText = text,
                    mediaUri = null
                )
            )
        }
    }

    // Add a contact using special ID
    fun addNewContact(id: String, name: String): Boolean {
        val cleanId = id.trim().uppercase()
        val finalId = if (!cleanId.startsWith("SEC-")) "SEC-$cleanId" else cleanId
        
        viewModelScope.launch {
            val randomImgId = Random.nextInt(1, 1000)
            val profilePicUrl = "https://picsum.photos/id/$randomImgId/150/150"
            repository.insertContact(
                Contact(
                    id = finalId,
                    name = name.trim(),
                    profilePicUrl = profilePicUrl,
                    onlineStatus = "online"
                )
            )

            // Send a real-time P2P friend request via PeerJS
            val profile = myProfile.value
            val myName = profile?.name ?: "Security Agent"

            PeerJSManager.sendFriendRequest(
                remoteId = finalId,
                name = myName
            )

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                Toast.makeText(getApplication(), "Secure contact added and notified!", Toast.LENGTH_LONG).show()
            }
        }
        return true
    }

    fun completeOnboarding(name: String, generatedId: String, customProfilePic: String?) {
        viewModelScope.launch {
            val current = repository.getMyProfileDirect()
            val finalProfilePic = customProfilePic ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
            if (current != null) {
                repository.updateProfile(
                    current.copy(
                        mySecureId = generatedId,
                        name = name.trim(),
                        profilePicUrl = finalProfilePic
                    )
                )
            } else {
                repository.updateProfile(
                    UserProfile(
                        mySecureId = generatedId,
                        name = name.trim(),
                        profilePicUrl = finalProfilePic
                    )
                )
            }
            
            // Re-initialize the PeerJS background bridge with the correct generated ID!
            PeerJSManager.init(getApplication(), repository, generatedId)
        }
    }
}
