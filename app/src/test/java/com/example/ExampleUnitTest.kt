package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.ui.SecureViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {

  @Test
  fun testCallStateMachine() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    
    val contact = Contact(
      id = "SEC-TEST-PEER",
      name = "Test Peer",
      profilePicUrl = "https://example.com/pic.png",
      onlineStatus = "online"
    )
    
    val viewModel = SecureViewModel(context)
    
    // Initial state
    assertNull(viewModel.activeCallSession.value)
    
    // Simulate Incoming Call
    viewModel.simulateIncomingCall(contact, CallType.VIDEO)
    var session = viewModel.activeCallSession.value
    assertNotNull(session)
    assertEquals("SEC-TEST-PEER", session?.contactId)
    assertEquals(CallType.VIDEO, session?.callType)
    assertEquals(CallStatus.INCOMING_RINGING, session?.status)
    
    // Accept Call
    viewModel.acceptCall()
    session = viewModel.activeCallSession.value
    assertEquals(CallStatus.CONNECTED, session?.status)
    
    // End Call
    viewModel.endCall()
    session = viewModel.activeCallSession.value
    assertEquals(CallStatus.DISCONNECTED, session?.status)
  }

  @Test
  fun testCryptographySymmetricKey() {
    val keyAB = SecureCrypto.generateSharedKey("userA", "userB")
    val keyBA = SecureCrypto.generateSharedKey("userB", "userA")
    
    // Check that keys are symmetric regardless of who initiates the call
    assertEquals(keyAB, keyBA)
    
    val plainText = "Hello, secure world!"
    val (encrypted, fingerprint) = SecureCrypto.encryptAES(plainText, keyAB)
    
    // Check that keyBA successfully decrypts the ciphertext encrypted with keyAB
    val decrypted = SecureCrypto.decryptAES(encrypted, keyBA)
    assertEquals(plainText, decrypted)
    
    // Check fingerprint formatting
    assert(fingerprint.startsWith("SEC-"))
  }
}
