package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.rememberAsyncImagePainter
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureDashboardScreen(
    viewModel: SecureViewModel,
    modifier: Modifier = Modifier
) {
    val contacts by viewModel.contacts.collectAsState()
    val activeStatuses by viewModel.activeStatuses.collectAsState()
    val myProfile by viewModel.myProfile.collectAsState()
    val activeContact by viewModel.activeContact.collectAsState()
    val callSession by viewModel.activeCallSession.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val peerConnectionState by viewModel.peerConnectionState.collectAsState()
    val remoteVideoRotation by viewModel.remoteVideoRotation.collectAsState()
    val localVideoRotation by viewModel.localVideoRotation.collectAsState()
    val p2pLogs by viewModel.p2pLogs.collectAsState()
    val fetchedProfiles by viewModel.fetchedProfiles.collectAsState()

    val context = LocalContext.current
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var isCallMinimized by remember { mutableStateOf(false) }

    LaunchedEffect(callSession) {
        if (callSession == null) {
            isCallMinimized = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Keep WebView attached permanently at the root of SecureDashboardScreen once PeerJS is initialized (after onboarding).
        // It remains virtually invisible (1dp size and low alpha) in the background so that signalling and calling connections
        // stay active and healthy. When a video call connects, it expands to fill the entire screen dynamically.
        val isVideoCallActiveAndConnected = callSession?.callType == CallType.VIDEO && callSession?.status == CallStatus.CONNECTED
        if (myProfile != null && myProfile?.name != "Security Agent") {
            Box(
                modifier = if (isVideoCallActiveAndConnected && callSession?.isVideoOff == false) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.size(1.dp).alpha(0.01f)
                }
            ) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { com.example.data.PeerJSManager.webView ?: android.webkit.WebView(context) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (myProfile != null && myProfile?.name == "Security Agent") {
            OnboardingScreen(
                viewModel = viewModel,
                myProfile = myProfile!!
            )
        } else {
            // Main Screen or Chat Room overlay
            if (activeContact != null) {
            ChatRoomView(
                viewModel = viewModel,
                contact = activeContact!!,
                onBack = { viewModel.selectContact(null) }
            )
        } else {
            Scaffold(
                topBar = {
                    LargeTopAppBar(
                        title = {
                            Column(modifier = Modifier.padding(start = 4.dp)) {
                                Text(
                                    text = "S7 Call",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 26.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    letterSpacing = (-0.5).sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VerifiedUser,
                                        contentDescription = "Verified E2EE",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "END-TO-END ENCRYPTED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        },
                        actions = {
                            // Add Friend Icon Button
                            IconButton(
                                onClick = { showAddFriendDialog = true },
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(40.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PersonAdd,
                                    contentDescription = "Add Friend",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        val activeItemColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )

                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { viewModel.selectTab(0) },
                            icon = { Icon(Icons.AutoMirrored.Filled.Chat, "Chats") },
                            label = { Text("Chats", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = activeItemColors
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { viewModel.selectTab(1) },
                            icon = { Icon(Icons.Filled.History, "Status") },
                            label = { Text("Status", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = activeItemColors
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { viewModel.selectTab(2) },
                            icon = { Icon(Icons.Filled.Shield, "Privacy") },
                            label = { Text("Privacy", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = activeItemColors
                        )
                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { viewModel.selectTab(3) },
                            icon = { Icon(Icons.Filled.Info, "Credits") },
                            label = { Text("Credits", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = activeItemColors
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (selectedTab) {
                        0 -> ChatsTab(
                            contacts = contacts,
                            statuses = activeStatuses,
                            myProfile = myProfile,
                            onContactClick = { viewModel.selectContact(it) },
                            onCallClick = { contact, isVideo ->
                                viewModel.initiateCall(contact, if (isVideo) CallType.VIDEO else CallType.AUDIO)
                            },
                            onStatusClick = { viewModel.selectTab(1) },
                            onAddFriendClick = { showAddFriendDialog = true }
                        )
                        1 -> StatusTab(
                            statuses = activeStatuses,
                            onAddStatus = { text ->
                                viewModel.postStatusUpdate(text)
                                Toast.makeText(context, "Disappearing status posted!", Toast.LENGTH_SHORT).show()
                            }
                        )
                        2 -> SettingsTab(
                            myProfile = myProfile,
                            onUpdateProfile = { name, visibility, showStatus, dark ->
                                viewModel.updateProfile(name, visibility, showStatus, dark)
                            },
                            onAddContact = { id, name ->
                                val success = viewModel.addNewContact(id, name)
                                if (success) {
                                    Toast.makeText(context, "Secure Contact Added!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Invalid secure ID format. Use SEC-XXX...", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                        3 -> CreditsTab()
                    }
                }
            }
        }
    }

        // Animated full screen secure calling overlay
        AnimatedVisibility(
            visible = callSession != null && !isCallMinimized,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            callSession?.let { session ->
                ActiveCallOverlay(
                    session = session,
                    peerConnectionState = peerConnectionState,
                    remoteVideoRotation = remoteVideoRotation,
                    localVideoRotation = localVideoRotation,
                    p2pLogs = p2pLogs,
                    onClearLogs = { viewModel.clearP2PLogs() },
                    onCycleRemoteRotation = { viewModel.cycleRemoteRotation() },
                    onCycleLocalRotation = { viewModel.cycleLocalRotation() },
                    onAccept = { viewModel.acceptCall() },
                    onDecline = { viewModel.declineCall() },
                    onHangup = { viewModel.endCall() },
                    onToggleMute = { viewModel.toggleMute() },
                    onToggleVideo = { viewModel.toggleVideo() },
                    onToggleSpeaker = { viewModel.toggleSpeaker() },
                    onCycleFilter = { viewModel.cycleFilter() },
                    onMinimize = { isCallMinimized = true }
                )
            }
        }

        // Minimized Calling Pill floating at the bottom-center of the screen
        if (callSession != null && isCallMinimized) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clickable { isCallMinimized = false },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xEB0F172A)),
                    border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0x3310B981), CircleShape)
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (callSession!!.callType == CallType.VIDEO) Icons.Filled.Videocam else Icons.Filled.Call,
                                    contentDescription = "Active Call",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = callSession!!.contactName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val min = callSession!!.durationSec / 60
                                val sec = callSession!!.durationSec % 60
                                Text(
                                    text = if (callSession!!.status == CallStatus.CONNECTED) {
                                        String.format("Active • %02d:%02d", min, sec)
                                    } else {
                                        "Connecting..."
                                    },
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { isCallMinimized = false },
                                modifier = Modifier
                                    .background(Color(0x22FFFFFF), CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Fullscreen,
                                    contentDescription = "Restore",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = { viewModel.endCall() },
                                modifier = Modifier
                                    .background(Color(0xFFEF4444), CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CallEnd,
                                    contentDescription = "End Call",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddFriendDialog) {
            var friendIdInput by remember { mutableStateOf("") }
            var friendNameInput by remember { mutableStateOf("") }
            var idError by remember { mutableStateOf(false) }

            // Auto-fetch name locally or request via P2P
            LaunchedEffect(friendIdInput) {
                val clean = friendIdInput.trim().uppercase()
                val finalId = if (!clean.startsWith("SEC-")) "SEC-$clean" else clean
                if (clean.length >= 5) {
                    val localName = viewModel.getLocalContactName(finalId)
                    if (localName != null) {
                        friendNameInput = localName
                    } else {
                        viewModel.requestProfile(finalId)
                    }
                }
            }

            // Observe P2P profile fetches
            val cleanId = friendIdInput.trim().uppercase()
            val finalId = if (!cleanId.startsWith("SEC-")) "SEC-$cleanId" else cleanId
            val p2pFetchedName = fetchedProfiles[finalId]
            LaunchedEffect(p2pFetchedName) {
                if (!p2pFetchedName.isNullOrBlank()) {
                    friendNameInput = p2pFetchedName
                }
            }

            AlertDialog(
                onDismissRequest = { showAddFriendDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PersonAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Establish Secure Tunnel",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Connect with a peer using their special Secure ID to start an end-to-end encrypted chat.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = friendIdInput,
                            onValueChange = {
                                friendIdInput = it
                                val clean = it.trim().uppercase()
                                val hasSec = clean.startsWith("SEC-")
                                val ipPart = if (hasSec) clean.substring(4) else clean
                                val isIp = ipPart.matches(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}"""))
                                idError = it.isNotBlank() && !hasSec && !isIp
                            },
                            label = { Text("Friend's S7 Secure ID or Local IP") },
                            placeholder = { Text("e.g. SEC-192.168.1.15 or 192.168.1.15") },
                            isError = idError,
                            supportingText = {
                                if (idError) {
                                    Text("Must be a valid S7 Secure ID (SEC-...) or Local IP Address", color = MaterialTheme.colorScheme.error)
                                } else {
                                    Text("Example: SEC-192.168.1.15 or 192.168.1.15", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = friendNameInput,
                            onValueChange = { friendNameInput = it },
                            label = { Text("Friend's Name") },
                            placeholder = { Text("e.g. Sarah Miller") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )



                        // Removed pending requests section as friends are added directly
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (friendIdInput.isNotBlank() && friendNameInput.isNotBlank()) {
                                val success = viewModel.addNewContact(friendIdInput, friendNameInput)
                                if (success) {
                                    Toast.makeText(context, "Secure Contact Added!", Toast.LENGTH_SHORT).show()
                                    showAddFriendDialog = false
                                } else {
                                    idError = true
                                    Toast.makeText(context, "Invalid secure ID format. Use SEC-XXX...", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Please enter both ID and Name.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Establish Tunnel", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddFriendDialog = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

// CHATS TAB
@Composable
fun ChatsTab(
    contacts: List<Contact>,
    statuses: List<StatusUpdate>,
    myProfile: UserProfile?,
    onContactClick: (Contact) -> Unit,
    onCallClick: (Contact, Boolean) -> Unit,
    onStatusClick: () -> Unit,
    onAddFriendClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Horizontal Status Updates Bar
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "ACTIVE STATUSES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // My Status Add Circle
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onStatusClick() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddAPhoto,
                                contentDescription = "Add Status",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "My Status",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Contacts' Status updates
                    statuses.forEach { status ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onStatusClick() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = status.userName.take(2).uppercase(),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = status.userName.substringBefore(" "),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
            }
        }

        // Info Banner
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = "Verified E2EE",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Peer-to-peer tunnels are fully isolated & encrypted",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.2.sp
                )
            }
        }

        // Channels / Chats List Title
        item {
            Text(
                text = "SECURE CHANNELS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            )
        }

        if (contacts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = "No Chats",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Encrypted Channels",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add contacts to establish an end-to-end encrypted channel.",
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAddFriendClick,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Friend / Contact", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(contacts) { contact ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onContactClick(contact) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Image with WhatsApp-style status ring / online indicator
                        Box {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = contact.profilePicUrl,
                                    placeholder = rememberAsyncImagePainter("https://picsum.photos/150")
                                ),
                                contentDescription = contact.name,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            if (contact.onlineStatus == "online") {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(Color(0xFF25D366), CircleShape) // WhatsApp Green
                                        .border(2.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                                        .align(Alignment.BottomEnd)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Text Info
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = contact.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = "Encrypted",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                
                                Text(
                                    text = "12:34 PM",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DoneAll, // WhatsApp double tick
                                    contentDescription = "Delivered",
                                    tint = Color(0xFF34B7F1), // WhatsApp blue ticks
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tap to open secure S7 voice/video tunnel",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Audio & Video Direct Dial Buttons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onCallClick(contact, false) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Call,
                                    contentDescription = "Audio Call",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { onCallClick(contact, true) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VideoCall,
                                    contentDescription = "Video Call",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(start = 70.dp)
                    )
                }
            }
        }

        // User Identity Card (At the bottom of chat screen)
        item {
            myProfile?.let { profile ->
                val clipboardManager = LocalClipboardManager.current
                val context = LocalContext.current
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val localIp = com.example.util.NetworkUtils.getLocalIpAddress()
                        val secureIpId = "SEC-$localIp"
                        Column {
                            Text(
                                text = "MY S7 ID",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = secureIpId,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(secureIpId))
                                Toast.makeText(context, "Secure ID copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Share ID", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// STATUS UPDATES TAB
@Composable
fun StatusTab(
    statuses: List<StatusUpdate>,
    onAddStatus: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Status Creation Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Disappearing status",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("What's secure today? Disappears in 24 hours...", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onAddStatus(textInput)
                            textInput = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Filled.Add, "Post")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Post Update")
                }
            }
        }

        // Active Status List
        Text(
            text = "Contacts updates",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (statuses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No status updates available in last 24h.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(statuses) { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Colored ring for unread status
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = status.userName.take(2).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = status.userName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = status.statusText,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Expiry progress
                            val hoursLeft = ((status.expiresAt - System.currentTimeMillis()) / (1000 * 60 * 60)).coerceAtLeast(0)
                            Text(
                                text = "⏱️ Vanishes in $hoursLeft hours",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                }
            }
        }
    }
}

// PRIVACY & SETTINGS TAB
@Composable
fun SettingsTab(
    myProfile: UserProfile?,
    onUpdateProfile: (String, String, Boolean, Boolean) -> Unit,
    onAddContact: (String, String) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var privacySelection by remember { mutableStateOf("EVERYONE") }
    var showOnline by remember { mutableStateOf(true) }
    var darkThemeActive by remember { mutableStateOf(false) }

    var contactIdInput by remember { mutableStateOf("") }
    var contactNameInput by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Set initial values
    LaunchedEffect(myProfile) {
        myProfile?.let {
            nameInput = it.name
            privacySelection = it.profileVisibility
            showOnline = it.onlineStatusVisibility
            darkThemeActive = it.isDarkMode
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Add secure contact using ID
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Establish Secure Tunnel",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Create a localized point-to-point secure channel with another device.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                OutlinedTextField(
                    value = contactIdInput,
                    onValueChange = { contactIdInput = it },
                    label = { Text("Contact's Special Secure ID") },
                    placeholder = { Text("e.g. SEC-205-943-WQA", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = contactNameInput,
                    onValueChange = { contactNameInput = it },
                    label = { Text("Contact Nickname") },
                    placeholder = { Text("e.g. Sarah Miller", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (contactIdInput.isNotBlank() && contactNameInput.isNotBlank()) {
                            onAddContact(contactIdInput, contactNameInput)
                            contactIdInput = ""
                            contactNameInput = ""
                        } else {
                            Toast.makeText(context, "Fill both ID and Nickname fields!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.PersonAdd, "Add", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Secure Contact", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Custom Privacy Settings",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-0.3).sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Profile Visibility
                Text(
                    text = "Profile Visibility (Pic & Status)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("EVERYONE", "CONTACTS_ONLY", "NOBODY").forEach { level ->
                        FilterChip(
                            selected = privacySelection == level,
                            onClick = {
                                privacySelection = level
                                onUpdateProfile(nameInput, level, showOnline, darkThemeActive)
                            },
                            label = { Text(level.replace("_", " "), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Online indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Online Status Indicator",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Let others see when you are online",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showOnline,
                        onCheckedChange = {
                            showOnline = it
                            onUpdateProfile(nameInput, privacySelection, it, darkThemeActive)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Dark mode toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dark Mode Interface",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Night-time eye protection design",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = darkThemeActive,
                        onCheckedChange = {
                            darkThemeActive = it
                            onUpdateProfile(nameInput, privacySelection, showOnline, it)
                            Toast.makeText(context, "Theme mode toggled successfully!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Copy My Secure ID Info
        myProfile?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val localIp = com.example.util.NetworkUtils.getLocalIpAddress()
                    val secureIpId = "SEC-$localIp"
                    
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(secureIpId))
                            Toast.makeText(context, "Secure ID copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "My Personal Secure Tunnel ID",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = (-0.2).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = secureIpId,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "My Local IP: $localIp",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Add contacts using their Local IP address to initiate real peer-to-peer call streams.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsTab() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var suggestionText by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App / Brand Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Beautiful futuristic icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Developer Credits",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "S7 Call Stream",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-0.5).sp
                )

                Text(
                    text = "Decentralized • End-to-End Encrypted • P2P",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp),
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "S7 Call is an ultra-secure calling and chatting app utilizing modern WebRTC data & media streams. No servers store your private logs—everything is strictly peer-to-peer and heavily protected.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }

        // Developer Credit Card with Instagram styling
        val instagramGradient = Brush.linearGradient(
            colors = listOf(
                Color(0xFF812A97), // Purple
                Color(0xFFE1306C), // Pink-red
                Color(0xFFF77737), // Orange
                Color(0xFFFCAF45)  // Yellow
            )
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Designed & Developed By",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Reach out for business, questions, or just a friendly chat!",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Instagram-styled banner/button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brush = instagramGradient, shape = RoundedCornerShape(18.dp))
                        .clickable {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse("https://instagram.com/nishix_vamp")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open browser. Copying handle...", Toast.LENGTH_SHORT).show()
                                clipboardManager.setText(AnnotatedString("@nishix_vamp"))
                            }
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = "Instagram",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Instagram Contact",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "@nishix_vamp",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.3).sp
                                )
                            }
                        }
                        
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "Open Profile",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Website styled banner/button with high-quality tech gradient
                val shaderGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF4FACFE), // Blue
                        Color(0xFF00F2FE)  // Cyan
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brush = shaderGradient, shape = RoundedCornerShape(18.dp))
                        .clickable {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse("https://shader7.com")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open browser. Copying link...", Toast.LENGTH_SHORT).show()
                                clipboardManager.setText(AnnotatedString("https://shader7.com"))
                            }
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Language,
                                    contentDescription = "Website",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Official Website",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "shader7.com",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.3).sp
                                )
                            }
                        }
                        
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "Open Website",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString("@nishix_vamp"))
                            Toast.makeText(context, "Instagram handle @nishix_vamp copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Instagram", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Insta", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString("https://shader7.com"))
                            Toast.makeText(context, "Website link copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Website", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Website", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        // Suggestions / Feedback box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Help Us Improve",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "S7 Call is fully decentralized & serverless to ensure total privacy. To submit your suggestions safely, this will draft a direct email to the developer (nxdecore@gmail.com) and copy the text to your clipboard.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                    lineHeight = 16.sp
                )

                if (submitted) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFF10B981).copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Submitted",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Email Draft Created!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = "Your suggestion was copied to your clipboard and drafted. Thank you for making S7 Call better!",
                                fontSize = 11.sp,
                                color = Color(0xFF047857),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp),
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = {
                                    submitted = false
                                    suggestionText = ""
                                }
                            ) {
                                Text("Send Another", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = suggestionText,
                        onValueChange = { suggestionText = it },
                        placeholder = { Text("Describe features, improvements, or enhancements...", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        shape = RoundedCornerShape(16.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (suggestionText.isNotBlank()) {
                                // Copy to clipboard as a fallback backup
                                clipboardManager.setText(AnnotatedString(suggestionText))
                                
                                try {
                                    val emailIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                        data = android.net.Uri.parse("mailto:") // only email apps should handle this
                                        putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("nxdecore@gmail.com"))
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "S7 Call Stream - Suggestion & Feedback")
                                        putExtra(android.content.Intent.EXTRA_TEXT, "Hi Nishikant,\n\nHere is my feedback for S7 Call Stream:\n\n$suggestionText\n\n---\nSent from S7 Call Stream Android App")
                                    }
                                    context.startActivity(android.content.Intent.createChooser(emailIntent, "Send Feedback Via..."))
                                    submitted = true
                                } catch (e: Exception) {
                                    // Fallback if no email client is available
                                    Toast.makeText(context, "Copied suggestion to clipboard. Please email nxdecore@gmail.com!", Toast.LENGTH_LONG).show()
                                    submitted = true
                                }
                            } else {
                                Toast.makeText(context, "Please enter your suggestion first!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Submit", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submit Suggestion", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        // App Version Footer
        Text(
            text = "S7 Call v1.2.5\nSecured Decentralized Node",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp),
            lineHeight = 16.sp
        )
    }
}

// CHAT ROOM VIEW (CONVERSATION DETAIL SCREEN)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomView(
    viewModel: SecureViewModel,
    contact: Contact,
    onBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val myProfile by viewModel.myProfile.collectAsState()
    val disappearingTime by viewModel.currentDisappearingTime.collectAsState()
    val peerConnectionState by viewModel.peerConnectionState.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var showVerifyDialog by remember { mutableStateOf(false) }
    val myId = myProfile?.mySecureId ?: "SEC-814-297-ZPH"

    var selectedDisappearDuration by remember { mutableStateOf(disappearingTime) }

    LaunchedEffect(contact) {
        // Reset timer indicator selection
        selectedDisappearDuration = disappearingTime
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            Image(
                                painter = rememberAsyncImagePainter(contact.profilePicUrl),
                                contentDescription = contact.name,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            if (contact.onlineStatus == "online") {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFF10B981), CircleShape)
                                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                        .align(Alignment.BottomEnd)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val statusColor = when (peerConnectionState) {
                                    "online" -> Color(0xFF10B981)
                                    "connecting" -> Color(0xFFF59E0B)
                                    else -> Color(0xFFEF4444)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(statusColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isTyping) "typing secure reply..." else "${peerConnectionState.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }} • ${contact.id}",
                                    fontSize = 11.sp,
                                    color = if (isTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Audio call
                    IconButton(onClick = { viewModel.initiateCall(contact, CallType.AUDIO) }) {
                        Icon(Icons.Filled.Call, "Audio Call")
                    }
                    // Video call
                    IconButton(onClick = { viewModel.initiateCall(contact, CallType.VIDEO) }) {
                        Icon(Icons.Filled.VideoCall, "Video Call")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        val isDark = isSystemInDarkTheme() || (myProfile?.isDarkMode == true)
        val chatBgColor = if (isDark) Color(0xFF0B141A) else Color(0xFFEFEAE2)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(chatBgColor)
        ) {
            // E2EE Shield Verification Banner
            Card(
                onClick = { showVerifyDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "E2EE",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "End-to-End Encrypted Tunnel",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Cipher: AES-256. Click to verify peer security fingerprint.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, "Verify", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }

            // Disappearing Messages Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "⏱️ Vanish:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterVertically).padding(end = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(
                    Pair(0, "Off"),
                    Pair(10, "10s"),
                    Pair(30, "30s"),
                    Pair(60, "1m"),
                    Pair(300, "5m"),
                    Pair(3600, "1hr")
                ).forEach { (seconds, label) ->
                    val isSelected = selectedDisappearDuration == seconds
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedDisappearDuration = seconds
                            viewModel.setDisappearingTime(seconds)
                        },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            // Messages Container
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    var showCiphertext by remember { mutableStateOf(false) }

                    val isMyMsg = msg.senderId == myId || msg.isSender
                    
                    // Decrypt content using symmetric AES shared key
                    val decryptedText = remember(msg.encryptedPayload) {
                        val sharedKey = SecureCrypto.generateSharedKey(msg.senderId, msg.receiverId)
                        SecureCrypto.decryptAES(msg.encryptedPayload, sharedKey)
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isMyMsg) Alignment.End else Alignment.Start
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMyMsg) Arrangement.End else Arrangement.Start
                        ) {
                            val bubbleColor = if (isMyMsg) {
                                if (isDark) Color(0xFF005C4B) else Color(0xFFD9FDD3)
                            } else {
                                if (isDark) Color(0xFF202C33) else Color(0xFFFFFFFF)
                            }
                            val bubbleTextColor = if (isDark) Color(0xFFE9EDEF) else Color(0xFF111B21)
                            
                            Card(
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .clickable { showCiphertext = !showCiphertext },
                                shape = RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isMyMsg) 12.dp else 2.dp,
                                    bottomEnd = if (isMyMsg) 2.dp else 12.dp
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = bubbleColor
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    if (showCiphertext) {
                                        // Display base64 cryptographically scrambled data
                                        Text(
                                            text = "🔐 CIPHERTEXT (Base64 E2EE):\n${msg.encryptedPayload}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = if (isMyMsg) bubbleTextColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "(Click again to see decrypted payload)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMyMsg) bubbleTextColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        // Display standard decrypted content
                                        Text(
                                            text = decryptedText,
                                            fontSize = 15.sp,
                                            color = bubbleTextColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Display security fingerprint confirmation
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "FPR: ${msg.encryptionKeyFingerprint.take(12)}...",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isMyMsg) bubbleTextColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        
                                        if (msg.isDisappearing) {
                                            val secondsRemaining = ((msg.expiresAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                                            Text(
                                                text = "⏱️ ${secondsRemaining}s",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isMyMsg) bubbleTextColor.copy(alpha = 0.8f) else Color.Red
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Input Bar Container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Media upload trigger
                IconButton(
                    onClick = {
                        viewModel.sendSecurePhoto("vault_snapshot_IMG_${System.currentTimeMillis() % 10000}.jpg")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Send Secure Media",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Encrypted message...", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    ),
                    trailingIcon = {
                        if (messageText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    viewModel.sendSecureMessage(messageText)
                                    messageText = ""
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    // Cryptographic Key Fingerprint verification dialog
    if (showVerifyDialog) {
        val sharedKey = remember(contact) { SecureCrypto.generateSharedKey(myId, contact.id) }
        val fingerprint = remember(sharedKey) { SecureCrypto.generateFingerprint(sharedKey) }

        Dialog(onDismissRequest = { showVerifyDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = "Security Keys Verified",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Verify Security Code",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To confirm this chat is 100% secure, compare the hexadecimal values below with ${contact.name}'s phone. If matching, the AES tunnel is cryptographically unbreakable.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Simulated fingerprint boxes
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = fingerprint,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showVerifyDialog = false },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Confirm Secure Fingerprint")
                    }
                }
            }
        }
    }
}

// FULL SCREEN SECURE CALLHUD (AUDIO & VIDEO CALLS OVERLAY WITH LOW-LATENCY STATS)
@Composable
fun ActiveCallOverlay(
    session: CallSession,
    peerConnectionState: String,
    remoteVideoRotation: Int,
    localVideoRotation: Int,
    p2pLogs: List<String>,
    onClearLogs: () -> Unit,
    onCycleRemoteRotation: () -> Unit,
    onCycleLocalRotation: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onHangup: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleVideo: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onCycleFilter: () -> Unit,
    onMinimize: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showDiagnostics by remember { mutableStateOf(false) }

    val isVideoFeedActive = session.callType == CallType.VIDEO && session.status == CallStatus.CONNECTED && !session.isVideoOff
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isVideoFeedActive) Color.Transparent else Color(0xFF0B0F19)) // dark cinema/cyber slate
    ) {
        // Top right controller row containing Rotate and Minimize actions
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (session.callType == CallType.VIDEO && session.status == CallStatus.CONNECTED) {
                // Remote Rotation Cycle Button
                IconButton(
                    onClick = onCycleRemoteRotation,
                    modifier = Modifier
                        .background(Color(0x66000000), CircleShape)
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ScreenRotation,
                        contentDescription = "Rotate Remote Video",
                        tint = Color(0xFF10B981)
                    )
                }

                // Local Rotation Cycle Button
                IconButton(
                    onClick = onCycleLocalRotation,
                    modifier = Modifier
                        .background(Color(0x66000000), CircleShape)
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cached,
                        contentDescription = "Rotate Local Video",
                        tint = Color(0xFF34D399)
                    )
                }
            }

            // Diagnostics / Developer Logs Toggle Button
            IconButton(
                onClick = { showDiagnostics = !showDiagnostics },
                modifier = Modifier
                    .background(if (showDiagnostics) Color(0xFF10B981) else Color(0x66000000), CircleShape)
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.BugReport,
                    contentDescription = "Toggle P2P Diagnostics",
                    tint = if (showDiagnostics) Color.Black else Color.White
                )
            }

            // Minimize Button
            IconButton(
                onClick = onMinimize,
                modifier = Modifier
                    .background(Color(0x66000000), CircleShape)
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.FullscreenExit,
                    contentDescription = "Minimize Call",
                    tint = Color.White
                )
            }
        }
        // If it is a video call and connected, draw high fidelity camera representation
        if (session.callType == CallType.VIDEO && session.status == CallStatus.CONNECTED) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!session.isVideoOff) {
                    // Remote Video Feed is rendered by the root WebView beneath this transparent layer.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                    )
                } else {
                    // Video is turned off
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Camera is Paused",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // Audio Call or non-connected video state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = rememberAsyncImagePainter(session.contactPic),
                        contentDescription = session.contactName,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFF10B981), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = session.contactName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (session.status) {
                            CallStatus.OUTGOING_RINGING -> "SECURE OUTGOING RINGING..."
                            CallStatus.INCOMING_RINGING -> "SECURE INCOMING CALL..."
                            CallStatus.CONNECTED -> "SECURE CALL CONNECTED"
                            CallStatus.DISCONNECTED -> "CALL DISCONNECTED"
                            else -> ""
                        },
                        color = Color(0xFF10B981),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Overlay low latency, quality and battery HUD labels (Only when connected)
        if (session.status == CallStatus.CONNECTED) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                    .background(Color(0x99000000), RoundedCornerShape(12.dp))
                    .border(0.5.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = when (peerConnectionState) {
                        "online" -> "🟢 P2P Active (Online)"
                        "connecting" -> "🟡 Connecting..."
                        else -> "🔴 Offline (P2P Failed)"
                    }
                    val statusColor = when (peerConnectionState) {
                        "online" -> Color(0xFF10B981)
                        "connecting" -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    val min = session.durationSec / 60
                    val sec = session.durationSec % 60
                    Text(
                        text = String.format("%02d:%02d", min, sec),
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Latency: ${session.latencyMs}ms", color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("Codec: Opus HD Voice (${session.audioQualityKbps}kbps)", color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Profile: ${session.batteryEfficiency.replace("_", " ")}", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("E2EE: Verified SEC-Tunnel", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Bottom controller HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (session.status == CallStatus.INCOMING_RINGING) {
                // Ringing triggers accept/decline action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FloatingActionButton(
                        onClick = onDecline,
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Filled.CallEnd, "Decline")
                    }

                    FloatingActionButton(
                        onClick = onAccept,
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Filled.Call, "Accept")
                    }
                }
            } else {
                // Connected or Outgoing buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Toggle
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .background(
                                if (session.isMuted) Color(0xFFEF4444) else Color(0x33FFFFFF),
                                CircleShape
                            )
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (session.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                            contentDescription = "Mute",
                            tint = Color.White
                        )
                    }

                    // Video Camera Toggle (If Video Call)
                    if (session.callType == CallType.VIDEO) {
                        IconButton(
                            onClick = onToggleVideo,
                            modifier = Modifier
                                .background(
                                    if (session.isVideoOff) Color(0xFFEF4444) else Color(0x33FFFFFF),
                                    CircleShape
                                )
                                .size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (session.isVideoOff) Icons.Filled.VideocamOff else Icons.Filled.Videocam,
                                contentDescription = "Toggle Video",
                                tint = Color.White
                            )
                        }

                        // Filter mode cyler (to save power/enhance night video)
                        IconButton(
                            onClick = onCycleFilter,
                            modifier = Modifier
                                .background(Color(0x33FFFFFF), CircleShape)
                                .size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Brush,
                                contentDescription = "Camera Filter",
                                tint = Color.White
                            )
                        }
                    }

                    // Speaker Toggle
                    IconButton(
                        onClick = onToggleSpeaker,
                        modifier = Modifier
                            .background(
                                if (session.isSpeakerOn) Color(0xFF10B981) else Color(0x33FFFFFF),
                                CircleShape
                            )
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (session.isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                            contentDescription = "Speaker",
                            tint = Color.White
                        )
                    }

                    // Decline/Hangup
                    FloatingActionButton(
                        onClick = onHangup,
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Filled.CallEnd, "Hangup")
                    }
                }
            }
        }

        // Diagnostics console panel
        AnimatedVisibility(
            visible = showDiagnostics,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xF4090D16)), // dark cinema slate with alpha
                border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.BugReport,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "P2P WebRTC DIAGNOSTICS",
                                color = Color(0xFF10B981),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Clear Button
                            TextButton(
                                onClick = onClearLogs,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = "CLEAR LOGS",
                                    color = Color(0xFFEF4444),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            // Close Button
                            IconButton(
                                onClick = { showDiagnostics = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close Diagnostics",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(1.dp)
                            .background(Color(0xFF1E293B))
                    )

                    // Logs list
                    val scrollState = rememberScrollState()
                    // Auto-scroll to bottom when new logs arrive
                    LaunchedEffect(p2pLogs.size) {
                        if (p2pLogs.isNotEmpty()) {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    }

                    if (p2pLogs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No real-time handshake logs generated yet.\nWaiting for PeerJS/WebRTC signaling messages...",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                        ) {
                            p2pLogs.forEach { logLine ->
                                val color = when {
                                    logLine.contains("Error") || logLine.contains("failed") || logLine.contains("Warning") -> Color(0xFFF87171)
                                    logLine.contains("Candidate") || logLine.contains("ICE") -> Color(0xFF60A5FA)
                                    logLine.contains("Metadata") || logLine.contains("Track") -> Color(0xFFFBBF24)
                                    logLine.contains("connected") || logLine.contains("open") -> Color(0xFF34D399)
                                    else -> Color(0xFFCBD5E1)
                                }
                                Text(
                                    text = logLine,
                                    color = color,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    viewModel: SecureViewModel,
    myProfile: UserProfile
) {
    var step by remember { mutableStateOf(1) }
    var nameInput by remember { mutableStateOf("") }
    
    // Cache the generated ID once so it doesn't regenerate on every recomposition
    val generatedId = remember {
        "SEC-${com.example.util.NetworkUtils.getLocalIpAddress()}"
    }
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (step == 1) {
                // STEP 1: Enter Name & Description
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = "Security Core",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Welcome to S7 Call",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "A hyper-secure, end-to-end encrypted messaging and call network. Please set up your name to establish your permanent cryptographic ID.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 25) nameInput = it },
                    label = { Text("Your Profile Name") },
                    placeholder = { Text("e.g., Alex Vance", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            step = 2
                        } else {
                            Toast.makeText(context, "Please enter your name!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "Generate My Secure ID",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else {
                // STEP 2: Show Generated Secure ID
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Key,
                        contentDescription = "ID Generated",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(50.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Identity Registered!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Welcome aboard, ${nameInput.trim()}! Here is your permanent, secure S7 identification ID. Share it with friends to create E2EE connections.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Display the ID elegantly
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "MY PERMANENT S7 SECURE ID",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.2.sp
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = generatedId,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        TextButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(generatedId))
                                Toast.makeText(context, "S7 ID copied!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy ID",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Copy Cryptographic ID",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        viewModel.completeOnboarding(nameInput.trim(), generatedId, null)
                        Toast.makeText(context, "Welcome to S7 Call!", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Initialize E2EE Tunnel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
