package com.godwin.orbitlauncher.ui.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.godwin.orbitlauncher.ui.theme.OrbitLauncherTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Registered as the device HOME app. Phase 1 renders only a live
 * clock/date placeholder to confirm the Compose + MVVM + Room + DataStore
 * stack boots correctly end to end. The full home screen (search bar,
 * dock, drawer button) is built in Phase 2; the radial wheel in Phase 3.
 */
class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OrbitLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)
                ) {
                    PlaceholderHomeScreen()
                }
            }
        }
    }

    // Home screen should not be exitable via back press.
    override fun onBackPressed() {
        // Intentionally empty.
    }
}

@Composable
private fun PlaceholderHomeScreen() {
    var timeText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        while (true) {
            val now = Date()
            timeText = timeFormat.format(now)
            dateText = dateFormat.format(now)
            delay(1000L)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = timeText,
            style = androidx.compose.material3.MaterialTheme.typography.displayLarge,
            color = androidx.compose.ui.graphics.Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = dateText,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            color = androidx.compose.ui.graphics.Color(0xFFAAAAAA),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Orbit Launcher \u2014 Phase 1 architecture",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = androidx.compose.ui.graphics.Color(0xFF666666),
            modifier = Modifier.padding(top = 32.dp),
            textAlign = TextAlign.Center
        )
    }
}
