package com.example.core.notifications

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuotaNotificationHelperTest {

    @Test
    fun notifications_respectPreferencesAndCreateDeepLinkNotifications() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm = Shadows.shadowOf(nm)

        // Ensure all 4 channels get created
        QuotaNotificationHelper.ensureChannels(app)
        assertEquals(4, shadowNm.notificationChannels.size)

        // Default preferences allow completion, stop, action-required, and quota alerts
        QuotaNotificationHelper.savePrefs(app, AgentNotificationPrefs())
        QuotaNotificationHelper.showCommandCompletedNotification(
            context = app,
            title = "اكتملت المهمة",
            details = "تم تنفيذ 4/4 خطوات",
            targetRoute = "tasks"
        )
        assertEquals(1, shadowNm.allNotifications.size)
        val completionNotif = shadowNm.allNotifications.first()
        val intentRoute = Shadows.shadowOf(completionNotif.contentIntent).savedIntent
            .getStringExtra(QuotaNotificationHelper.EXTRA_NAV_ROUTE)
        assertEquals("tasks", intentRoute)

        QuotaNotificationHelper.showAgentStoppedNotification(
            context = app,
            title = "توقف مؤقت",
            reason = "استنفدت الحصة وسيعاود المحاولة",
            isPausedWillRetry = true,
            targetRoute = "tasks"
        )
        QuotaNotificationHelper.showActionRequiredNotification(
            context = app,
            title = "رفع تعديل",
            reason = "مطلوب موافقة المستوى 2",
            tier = 2,
            targetRoute = "security"
        )
        assertEquals(3, shadowNm.allNotifications.size)

        // Toggling off stop notifications suppresses stop alerts cleanly
        QuotaNotificationHelper.savePrefs(
            app,
            AgentNotificationPrefs(notifyOnStopOrError = false)
        )
        assertFalse(QuotaNotificationHelper.getPrefs(app).notifyOnStopOrError)
        QuotaNotificationHelper.showAgentStoppedNotification(
            context = app,
            title = "توقف آخر",
            reason = "يجب أن لا يظهر",
            targetRoute = "tasks"
        )
        assertEquals(3, shadowNm.allNotifications.size)
        assertTrue(QuotaNotificationHelper.sendTestNotification(app))
        assertEquals(4, shadowNm.allNotifications.size)
    }
}
