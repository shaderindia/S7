# S7 Call 🔒
### *End-to-End Encrypted Secure Texting, Voice, and Video Calling App*

S7 Call is a modern, privacy-focused Android application that provides secure, encrypted peer-to-peer texting, high-fidelity voice calling, and real-time video streaming. By combining robust local AES encryption with a dynamic WebRTC signaling architecture hosted on Firebase, it ensures absolute privacy without requiring centralized, invasive servers.

---

## 🚀 Key Features

*   **P2P Voice & Video Calls:** Real-time WebRTC connections with automatic audio/video track routing.
*   **End-to-End Encryption (E2EE):** All chat messages are encrypted locally using AES-CBC before transmission. Encryption keys are derived symmetrically per chat pair.
*   **Visual Key Fingerprints:** Users can compare Signal-style security numbers (`SEC-XXXX-...`) to verify identity.
*   **Disappearing Messages:** Configurable message-expiration timers (e.g., 10s, 30s, 60s) with automatic background database purging.
*   **Robust Local Database:** Backed by Android Room database for high-performance offline caching, message persistence, and state management.
*   **Signaling Fallback:** Seamless signaling and connection coordination leveraging a secure Firebase Realtime Database.
*   **Modern Jetpack Compose UI:** A responsive, interactive dashboard styled with dynamic dark/light mode switching.

---

## 🛠️ Architecture & Codebase Map

S7 Call follows the **MVVM (Model-View-ViewModel)** architectural pattern. The core components include:

### 📱 User Interface (Compose)
*   **[MainActivity.kt](file:///app/src/main/java/com/example/MainActivity.kt):** Main app entry point. Handles permissions (Camera, Mic, Notifications) and binds the active dashboard theme.
*   **[DashboardScreen.kt](file:///app/src/main/java/com/example/ui/DashboardScreen.kt):** Contains all Jetpack Compose screens including Chat, Call, Status, Settings, and Onboarding.

### ⚙️ State & Logic (ViewModel)
*   **[SecureViewModel.kt](file:///app/src/main/java/com/example/ui/SecureViewModel.kt):** Coordinates calling flows, message dispatch, presence updates, and database actions.

### 💾 Data & Signaling (Repository & WebView Bridge)
*   **[SecureRepository.kt](file:///app/src/main/java/com/example/data/SecureRepository.kt):** Binds the local database DAOs and intercepts outgoing/incoming data with cryptography filters.
*   **[SecureCrypto.kt](file:///app/src/main/java/com/example/data/SecureCrypto.kt):** Generates symmetric encryption keys and handles AES encrypt/decrypt and Signal fingerprint generation.
*   **[PeerJSManager.kt](file:///app/src/main/java/com/example/data/PeerJSManager.kt):** Bridges Kotlin calling methods to the WebView runtime.
*   **[peerjs_app.html](file:///app/src/main/assets/peerjs_app.html):** Headless WebView asset running WebRTC `RTCPeerConnection` and Firebase signaling loops.

---

## ⚙️ Quick Start Setup

### Prerequisites
*   **Android Studio** (Koala or newer)
*   **Android SDK 36**
*   **JDK 17 or JBR**

### Local Configuration
1.  **Clone the Repository** (Done).
2.  **Configure API Secrets:**
    Create a `.env` file in the root directory and specify your Gemini API key (see `.env.example` as a template):
    ```env
    GEMINI_API_KEY=your_gemini_api_key_here
    ```
3.  **Specify SDK Location:**
    Create a `local.properties` file in the root directory specifying your local Android SDK location:
    ```properties
    sdk.dir=/path/to/your/Android/Sdk
    ```

---

## 🧪 Testing and Verification

### Unit Testing
S7 Call uses **Robolectric** to run simulated Android environment tests locally on the host JVM.

To run the unit tests (which verify the cryptographic key symmetry and call session state machine transitions):
```bash
./gradlew.bat test
```

### Manual E2E Calling Test Driver
A manual browser-based WebRTC test driver is included at `test_calling.html`. You can serve and load this page in two adjacent browser iframes to simulate the E2E calling handshakes and verify signaling configurations.
