package com.example

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.ui.HomelyApp
import com.example.ui.theme.HomelyTheme
import java.io.File

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestHighestSupportedRefreshRate()
    configureAppImageLoader()
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )
    setContent {
      HomelyTheme(darkTheme = true) {
        HomelyApp()
      }
    }
  }

  private fun requestHighestSupportedRefreshRate() {
    runCatching {
      @Suppress("DEPRECATION")
      val activeDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display ?: windowManager.defaultDisplay
      } else {
        windowManager.defaultDisplay
      } ?: return

      val currentMode = activeDisplay.mode
      val bestMode = activeDisplay.supportedModes
        ?.filter {
          it.physicalWidth == currentMode.physicalWidth &&
            it.physicalHeight == currentMode.physicalHeight
        }
        ?.maxByOrNull { it.refreshRate }
        ?: activeDisplay.supportedModes?.maxByOrNull { it.refreshRate }

      val params = window.attributes
      if (bestMode != null && bestMode.modeId != params.preferredDisplayModeId) {
        params.preferredDisplayModeId = bestMode.modeId
        params.preferredRefreshRate = bestMode.refreshRate
        window.attributes = params
      }
    }
  }

  private fun configureAppImageLoader() {
    if (isCoilLoaderConfigured) return
    synchronized(CoilConfigLock) {
      if (isCoilLoaderConfigured) return
      val appCtx = applicationContext
      val loader = ImageLoader.Builder(appCtx)
        .memoryCache {
          MemoryCache.Builder(appCtx)
            .maxSizePercent(0.18)
            .build()
        }
        .diskCache {
          DiskCache.Builder()
            .directory(File(appCtx.cacheDir, "coil_image_cache"))
            .maxSizeBytes(32L * 1024L * 1024L)
            .build()
        }
        .crossfade(false)
        .respectCacheHeaders(false)
        .build()
      Coil.setImageLoader(loader)
      isCoilLoaderConfigured = true
    }
  }

  private companion object {
    private val CoilConfigLock = Any()
    @Volatile
    private var isCoilLoaderConfigured = false
  }
}
