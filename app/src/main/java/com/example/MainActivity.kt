package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.SecureDashboardScreen
import com.example.ui.SecureViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Request permissions required for P2P calling and notifications
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= 33) { // Build.VERSION_CODES.TIRAMISU
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissions(permissions.toTypedArray(), 101)
    }

    setContent {
      val viewModel: SecureViewModel = viewModel()
      val profile by viewModel.myProfile.collectAsState()
      
      // Live dynamic dark/light theme switcher bound directly to database settings!
      val isDarkTheme = profile?.isDarkMode ?: false

      MyApplicationTheme(darkTheme = isDarkTheme) {
        SecureDashboardScreen(
          viewModel = viewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    com.example.data.PeerJSManager.onAppForeground()
  }
}
