package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.google.firebase.FirebaseApp

object PeerJSManager {
    private const val TAG = "PeerJSManager"
    @SuppressLint("StaticFieldLeak")
    var webView: WebView? = null
    var myId: String = ""
    var repository: SecureRepository? = null
    var context: Context? = null
    var onIncomingCall: ((String, Boolean) -> Unit)? = null
    var onCallConnectedCallback: (() -> Unit)? = null
    var onCallDisconnectedCallback: (() -> Unit)? = null
    var onConnectionStateChangeCallback: ((String) -> Unit)? = null
    var onErrorCallback: ((String) -> Unit)? = null
    var onLogCallback: ((String) -> Unit)? = null
    var onProfileFetched: ((String, String) -> Unit)? = null
    var onRoomMatched: ((String, String) -> Unit)? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun init(ctx: Context, repo: SecureRepository, peerId: String) {
        context = ctx.applicationContext
        repository = repo
        myId = peerId
        
        val firebaseApp = try { FirebaseApp.getInstance() } catch(e: Exception) { null }
        val options = firebaseApp?.options
        val apiKey = options?.apiKey ?: ""
        var databaseUrl = options?.databaseUrl ?: ""
        if (databaseUrl.isEmpty()) {
            databaseUrl = "https://nsgb-gaming-default-rtdb.firebaseio.com"
        }
        val projectId = options?.projectId ?: ""

        Handler(Looper.getMainLooper()).post {
            if (webView == null) {
                // Ensure cache directories exist to prevent benign Chromium opendir warnings
                try {
                    val cacheDir = java.io.File(context!!.cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
                    if (!cacheDir.exists()) cacheDir.mkdirs()
                    val wasmDir = java.io.File(context!!.cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
                    if (!wasmDir.exists()) wasmDir.mkdirs()
                } catch (e: Exception) {
                    // Ignore
                }

                webView = WebView(context!!).apply {
                    settings.javaScriptEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.allowFileAccessFromFileURLs = true
                    settings.allowUniversalAccessFromFileURLs = true
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    
                    webChromeClient = object : WebChromeClient() {
                        override fun onPermissionRequest(request: PermissionRequest) {
                            // Grant camera/mic permissions automatically to WebRTC
                            request.grant(request.resources)
                        }
                        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                            consoleMessage?.let {
                                val msg = "WebView Console [${it.messageLevel()}]: ${it.message()} at ${it.sourceId()}:${it.lineNumber()}"
                                Log.d(TAG, msg)
                                Handler(Looper.getMainLooper()).post {
                                    onLogCallback?.invoke(msg)
                                }
                            }
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            Log.d(TAG, "PeerJS bridge loaded, initializing peer...")
                            evaluateJavascript("initPeer('$myId', '$apiKey', '$databaseUrl', '$projectId')") {}
                        }
                    }
                    addJavascriptInterface(this@PeerJSManager, "AndroidBridge")
                }
                webView?.loadUrl("file:///android_asset/peerjs_app.html")
            } else {
                Log.d(TAG, "Re-initializing existing peer in WebView with new ID: $myId")
                webView?.evaluateJavascript("initPeer('$myId', '$apiKey', '$databaseUrl', '$projectId')", null)
            }
        }
    }

    @JavascriptInterface
    fun log(msg: String) {
        Log.d(TAG, "[JS] $msg")
        Handler(Looper.getMainLooper()).post {
            onLogCallback?.invoke(msg)
        }
    }

    @JavascriptInterface
    fun onConnectionStateChange(state: String) {
        Log.d(TAG, "Connection state changed: $state")
        Handler(Looper.getMainLooper()).post {
            onConnectionStateChangeCallback?.invoke(state)
        }
    }

    @JavascriptInterface
    fun onError(type: String) {
        Log.e(TAG, "PeerJS Error: $type")
        Handler(Looper.getMainLooper()).post {
            onErrorCallback?.invoke(type)
        }
        // Handle reconnect if network fails
        if (type == "network" || type == "disconnected") {
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d(TAG, "Attempting reconnect...")
                webView?.evaluateJavascript("if(peer) peer.reconnect();", null)
            }, 3000)
        }
    }

    @JavascriptInterface
    fun onDataReceived(remoteId: String, jsonString: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = JSONObject(jsonString)
                if (data.getString("type") == "message") {
                    repository?.receiveSecureMessage(
                        senderId = remoteId,
                        receiverId = myId,
                        plainText = data.getString("text"),
                        isDisappearing = data.getBoolean("isDisappearing"),
                        disappearDurationSec = data.getInt("disappearDurationSec")
                    )
                } else if (data.getString("type") == "friend_request") {
                    val name = data.getString("name")
                    // Since we are directly adding friends, we insert directly instead of creating a pending request
                    repository?.insertContact(
                        Contact(
                            id = remoteId,
                            name = name,
                            profilePicUrl = "https://picsum.photos/id/1025/150/150",
                            onlineStatus = "online"
                        )
                    )
                } else if (data.getString("type") == "profile_request") {
                    val profile = repository?.getMyProfileDirect()
                    val myName = profile?.name ?: "Security Agent"
                    sendProfileResponse(remoteId, myName)
                } else if (data.getString("type") == "profile_response") {
                    val name = data.getString("name")
                    Handler(Looper.getMainLooper()).post {
                        onProfileFetched?.invoke(remoteId, name)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse incoming data", e)
            }
        }
    }

    @JavascriptInterface
    fun onIncomingCall(remoteId: String, isVideo: Boolean) {
        Handler(Looper.getMainLooper()).post {
            onIncomingCall?.invoke(remoteId, isVideo)
        }
    }

    @JavascriptInterface
    fun onIncomingCall(remoteId: String) {
        Handler(Looper.getMainLooper()).post {
            onIncomingCall?.invoke(remoteId, true)
        }
    }

    @JavascriptInterface
    fun onCallConnected() {
        Log.d(TAG, "onCallConnected triggered from Javascript")
        Handler(Looper.getMainLooper()).post {
            onCallConnectedCallback?.invoke()
        }
    }

    @JavascriptInterface
    fun onCallDisconnected() {
        Log.d(TAG, "onCallDisconnected triggered from Javascript")
        Handler(Looper.getMainLooper()).post {
            onCallDisconnectedCallback?.invoke()
        }
    }

    @JavascriptInterface
    fun updatePresence(remoteId: String, isOnline: Boolean) {
        val status = if (isOnline) "online" else "offline"
        Log.d(TAG, "updatePresence from Javascript: $remoteId -> $status")
        CoroutineScope(Dispatchers.IO).launch {
            repository?.contactDao?.updateOnlineStatus(remoteId, status, System.currentTimeMillis())
        }
    }

    fun pingContacts(contactIds: List<String>) {
        val jsonArray = org.json.JSONArray(contactIds)
        Handler(Looper.getMainLooper()).post {
            webView?.evaluateJavascript("pingContacts('$jsonArray')", null)
        }
    }

    fun onAppForeground() {
        Log.d(TAG, "App foregrounded, ensuring signaling connection is active...")
        val firebaseApp = try { com.google.firebase.FirebaseApp.getInstance() } catch(e: Exception) { null }
        val options = firebaseApp?.options
        val apiKey = options?.apiKey ?: ""
        var databaseUrl = options?.databaseUrl ?: ""
        if (databaseUrl.isEmpty()) {
            databaseUrl = "https://nsgb-gaming-default-rtdb.firebaseio.com"
        }
        val projectId = options?.projectId ?: ""
        Handler(Looper.getMainLooper()).post {
            webView?.evaluateJavascript("initPeer('$myId', '$apiKey', '$databaseUrl', '$projectId')", null)
        }
    }

    fun sendMessage(remoteId: String, text: String, isDisappearing: Boolean, disappearDurationSec: Int) {
        val payload = JSONObject().apply {
            put("type", "message")
            put("text", text)
            put("isDisappearing", isDisappearing)
            put("disappearDurationSec", disappearDurationSec)
        }
        val base64Payload = android.util.Base64.encodeToString(
            payload.toString().toByteArray(Charsets.UTF_8),
            android.util.Base64.NO_WRAP
        )
        Handler(Looper.getMainLooper()).post {
            webView?.evaluateJavascript("sendDataBase64('$remoteId', '$base64Payload')", null)
        }
    }

    fun sendFriendRequest(remoteId: String, name: String) {
        val payload = JSONObject().apply {
            put("type", "friend_request")
            put("name", name)
        }
        val base64Payload = android.util.Base64.encodeToString(
            payload.toString().toByteArray(Charsets.UTF_8),
            android.util.Base64.NO_WRAP
        )
        Handler(Looper.getMainLooper()).post {
            webView?.evaluateJavascript("sendDataBase64('$remoteId', '$base64Payload')", null)
        }
    }

    fun requestProfile(remoteId: String) {
        val payload = JSONObject().apply {
            put("type", "profile_request")
        }
        val base64Payload = android.util.Base64.encodeToString(
            payload.toString().toByteArray(Charsets.UTF_8),
            android.util.Base64.NO_WRAP
        )
        Handler(Looper.getMainLooper()).post {
            webView?.evaluateJavascript("sendDataBase64('$remoteId', '$base64Payload')", null)
        }
    }

    fun sendProfileResponse(remoteId: String, name: String) {
        val payload = JSONObject().apply {
            put("type", "profile_response")
            put("name", name)
        }
        val base64Payload = android.util.Base64.encodeToString(
            payload.toString().toByteArray(Charsets.UTF_8),
            android.util.Base64.NO_WRAP
        )
        Handler(Looper.getMainLooper()).post {
            webView?.evaluateJavascript("sendDataBase64('$remoteId', '$base64Payload')", null)
        }
    }

    fun startCall(remoteId: String, isVideo: Boolean) {
        Handler(Looper.getMainLooper()).post {
            webView?.evaluateJavascript("startCall('$remoteId', $isVideo)", null)
        }
    }

    fun answerCall(isVideo: Boolean) {
        Handler(Looper.getMainLooper()).post {
            webView?.evaluateJavascript("answerCall($isVideo)", null)
        }
    }

    fun endCall() {
        Handler(Looper.getMainLooper()).post {
            webView?.evaluateJavascript("endCall()", null)
        }
    }

    fun toggleMute(isMuted: Boolean) {
        Handler(Looper.getMainLooper()).post {
            webView?.evaluateJavascript("toggleMute($isMuted)", null)
        }
    }

    fun toggleVideo(isVideoOff: Boolean) {
        Handler(Looper.getMainLooper()).post {
            webView?.evaluateJavascript("toggleVideo($isVideoOff)", null)
        }
    }

    fun setVideoRotations(remoteRot: Int, localRot: Int) {
        Handler(Looper.getMainLooper()).post {
            webView?.evaluateJavascript("setVideoRotations($remoteRot, $localRot)", null)
        }
    }

    @JavascriptInterface
    fun onRoomMatched(remoteId: String, name: String) {
        Log.d(TAG, "Room matched: $remoteId -> $name")
        Handler(Looper.getMainLooper()).post {
            onRoomMatched?.invoke(remoteId, name)
        }
    }

    fun createRoom(roomId: String, myId: String, name: String) {
        Handler(Looper.getMainLooper()).post {
            webView?.evaluateJavascript("createRoomInWebView('$roomId', '$myId', '$name')", null)
        }
    }

    fun joinRoom(roomId: String, myId: String, name: String) {
        Handler(Looper.getMainLooper()).post {
            webView?.evaluateJavascript("joinRoomInWebView('$roomId', '$myId', '$name')", null)
        }
    }

    fun cancelRoom(roomId: String) {
        Handler(Looper.getMainLooper()).post {
            webView?.evaluateJavascript("cancelRoomMatchingInWebView('$roomId')", null)
        }
    }
}
