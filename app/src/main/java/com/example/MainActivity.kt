package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.platform.DefaultLauncherHelper
import com.example.ui.LauncherRoot
import com.example.ui.LauncherViewModel
import com.example.ui.theme.MistLauncherTheme

class MainActivity : ComponentActivity() {

  private val viewModel: LauncherViewModel by viewModels {
    object : androidx.lifecycle.ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return LauncherViewModel(application) as T
      }
    }
  }

  private val requestHomeRoleLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) {
    viewModel.refreshSystemStatus()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.auto(
        lightScrim = android.graphics.Color.TRANSPARENT,
        darkScrim = android.graphics.Color.TRANSPARENT
      ),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
    )

    setContent {
      MistLauncherTheme {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        LauncherRoot(
          uiState = uiState,
          iconCache = viewModel.iconCache,
          viewModel = viewModel,
          onRequestDefaultHome = { launchDefaultHomeRequest() },
          onLightStatusBarChanged = { useDarkIcons ->
            WindowInsetsControllerCompat(window, window.decorView).apply {
              isAppearanceLightStatusBars = useDarkIcons
              isAppearanceLightNavigationBars = false
            }
          }
        )
      }
    }
  }

  override fun onStart() {
    super.onStart()
    viewModel.onAppStart()
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshSystemStatus()
  }

  override fun onStop() {
    super.onStop()
    viewModel.onAppStop()
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (intent.hasCategory(Intent.CATEGORY_HOME)) {
      viewModel.onHomeIntentReceived()
    }
  }

  private fun launchDefaultHomeRequest() {
    val primary = DefaultLauncherHelper.createRequestDefaultHomeIntent(this)
    val candidates = listOf(primary) + DefaultLauncherHelper.createDefaultAppsFallbackIntents()
    for (intent in candidates) {
      val launched = runCatching {
        requestHomeRoleLauncher.launch(intent)
      }.isSuccess
      if (launched) return
    }
  }
}
