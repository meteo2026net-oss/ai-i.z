package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.core.notifications.QuotaNotificationHelper
import com.example.ui.navigation.MetraRootApp

class MainActivity : ComponentActivity() {

    private var pendingNotificationRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        QuotaNotificationHelper.ensureChannels(this)
        extractNotificationRoute(intent)

        enableEdgeToEdge()
        setContent {
            MetraRootApp(
                pendingNotificationRoute = pendingNotificationRoute,
                onNotificationRouteConsumed = { pendingNotificationRoute = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractNotificationRoute(intent)
    }

    private fun extractNotificationRoute(intent: Intent?) {
        val route = intent?.getStringExtra(QuotaNotificationHelper.EXTRA_NAV_ROUTE)
        if (!route.isNullOrBlank()) {
            pendingNotificationRoute = route
            intent.removeExtra(QuotaNotificationHelper.EXTRA_NAV_ROUTE)
        }
    }
}
