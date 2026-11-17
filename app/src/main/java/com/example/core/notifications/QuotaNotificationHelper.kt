package com.example.core.notifications

import com.example.ui.i18n.AppUiStrings
import java.util.Locale

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

data class AgentNotificationPrefs(
    val notifyOnCompletion: Boolean = true,
    val notifyOnStopOrError: Boolean = true,
    val notifyOnActionRequired: Boolean = true,
    val notifyOnQuotaFailover: Boolean = true
)

object QuotaNotificationHelper {
    const val EXTRA_NAV_ROUTE = "metra_nav_route"

    private const val PREFS_FILE = "metra_agent_notif_prefs"
    private const val KEY_COMPLETION = "notify_completion"
    private const val KEY_STOP_ERROR = "notify_stop_error"
    private const val KEY_ACTION_REQ = "notify_action_required"
    private const val KEY_QUOTA = "notify_quota_failover"

    const val CHANNEL_QUOTA = "ai_quota_alerts"
    const val CHANNEL_COMPLETION = "agent_completion_alerts"
    const val CHANNEL_STOP = "agent_stop_alerts"
    const val CHANNEL_ACTION_REQUIRED = "agent_action_required_alerts"

    fun getPrefs(context: Context): AgentNotificationPrefs {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return AgentNotificationPrefs(
            notifyOnCompletion = prefs.getBoolean(KEY_COMPLETION, true),
            notifyOnStopOrError = prefs.getBoolean(KEY_STOP_ERROR, true),
            notifyOnActionRequired = prefs.getBoolean(KEY_ACTION_REQ, true),
            notifyOnQuotaFailover = prefs.getBoolean(KEY_QUOTA, true)
        )
    }

    fun savePrefs(context: Context, updated: AgentNotificationPrefs) {
        context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_COMPLETION, updated.notifyOnCompletion)
            .putBoolean(KEY_STOP_ERROR, updated.notifyOnStopOrError)
            .putBoolean(KEY_ACTION_REQ, updated.notifyOnActionRequired)
            .putBoolean(KEY_QUOTA, updated.notifyOnQuotaFailover)
            .apply()
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ACTION_REQUIRED,
                    "طلبات الموافقة والإجراءات العاجلة",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "تنبيه فوري بصوت واهتزاز عندما يتوقف الوكيل بانتظار موافقتك أو إجراء منك"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0L, 250L, 120L, 250L, 120L, 250L)
                    enableLights(true)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                },
                NotificationChannel(
                    CHANNEL_COMPLETION,
                    "إنهاء الأوامر والمهام",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "إشعار عند انتهاء مهمة أو أمر برمجي أو جولة تعاون النماذج أو الاختبارات"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0L, 180L, 90L, 180L)
                    enableLights(true)
                },
                NotificationChannel(
                    CHANNEL_STOP,
                    "توقف الوكيل والأخطاء",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "إشعار فوري عند توقف التنفيذ مؤقتاً أو حدوث خطأ أو حظر أمني"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0L, 300L, 140L, 300L)
                    enableLights(true)
                },
                NotificationChannel(
                    CHANNEL_QUOTA,
                    "تنبيهات استهلاك الذكاء الاصطناعي",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "إشعارات فورية عند انتهاء حصة API والتحويل التلقائي إلى حساب أو مزود آخر"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0L, 200L, 100L, 200L)
                    enableLights(true)
                }
            )
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun showCommandCompletedNotification(
        context: Context,
        title: String,
        details: String,
        targetRoute: String = "tasks"
    ) {
        if (!getPrefs(context).notifyOnCompletion) return
        postNotification(
            context = context,
            channelId = CHANNEL_COMPLETION,
            title = "✅ $title",
            body = details.ifBlank { "انتهى تنفيذ الأمر بنجاح." },
            targetRoute = targetRoute,
            priority = NotificationCompat.PRIORITY_HIGH,
            category = NotificationCompat.CATEGORY_STATUS
        )
    }

    fun showAgentStoppedNotification(
        context: Context,
        title: String,
        reason: String,
        isPausedWillRetry: Boolean = false,
        targetRoute: String = "tasks"
    ) {
        if (!getPrefs(context).notifyOnStopOrError) return
        val prefix = if (isPausedWillRetry) "⏸️" else "⛔"
        postNotification(
            context = context,
            channelId = CHANNEL_STOP,
            title = "$prefix $title",
            body = reason.ifBlank {
                if (isPausedWillRetry) "توقف الوكيل مؤقتاً وسيستأنف تلقائياً." else "توقف التنفيذ ويتطلب مراجعتك."
            },
            targetRoute = targetRoute,
            priority = NotificationCompat.PRIORITY_HIGH,
            category = NotificationCompat.CATEGORY_ERROR
        )
    }

    fun showActionRequiredNotification(
        context: Context,
        title: String,
        reason: String,
        tier: Int = 2,
        targetRoute: String = "security"
    ) {
        if (!getPrefs(context).notifyOnActionRequired) return
        val badge = if (tier >= 3) "🔐 إجراء عاجل (المستوى 3)" else "🔔 مطلوب موافقتك (المستوى $tier)"
        postNotification(
            context = context,
            channelId = CHANNEL_ACTION_REQUIRED,
            title = "$badge: $title",
            body = reason.ifBlank { "الوكيل بانتظار قرارك للمتابعة." },
            targetRoute = targetRoute,
            priority = NotificationCompat.PRIORITY_MAX,
            category = NotificationCompat.CATEGORY_REMINDER
        )
    }

    fun showQuotaFailoverNotification(
        context: Context,
        exhaustedLabel: String,
        switchedToLabel: String?,
        reason: String,
        targetRoute: String = "ai_setup"
    ) {
        if (!getPrefs(context).notifyOnQuotaFailover) return
        val body = if (!switchedToLabel.isNullOrBlank()) {
            "انتهى استخدام «$exhaustedLabel». تم التحويل تلقائياً إلى «$switchedToLabel» لمتابعة العمل. ($reason)"
        } else {
            "انتهى استخدام «$exhaustedLabel» ولا توجد مفاتيح بديلة متاحة حالياً. ($reason)"
        }
        postNotification(
            context = context,
            channelId = CHANNEL_QUOTA,
            title = AppUiStrings.notifQuotaWarning(Locale.getDefault().language),
            body = body,
            targetRoute = targetRoute,
            priority = NotificationCompat.PRIORITY_HIGH,
            category = NotificationCompat.CATEGORY_STATUS
        )
    }

    fun sendTestNotification(context: Context): Boolean {
        if (!hasNotificationPermission(context)) return false
        postNotification(
            context = context,
            channelId = CHANNEL_ACTION_REQUIRED,
            title = AppUiStrings.notifAgentWorking(Locale.getDefault().language),
            body = "سيصلك تنبيه فوري بصوت واهتزاز عند انتهاء أي أمر أو توقف الوكيل أو طلب إجراء منك.",
            targetRoute = "dashboard",
            priority = NotificationCompat.PRIORITY_HIGH,
            category = NotificationCompat.CATEGORY_REMINDER
        )
        return true
    }

    private fun postNotification(
        context: Context,
        channelId: String,
        title: String,
        body: String,
        targetRoute: String,
        priority: Int,
        category: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val tapIntent = Intent(context.applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_ROUTE, targetRoute)
        }
        val notifId = ((System.currentTimeMillis() and 0x7FFFFFFF)).toInt()
        val pendingIntent = PendingIntent.getActivity(
            context.applicationContext,
            notifId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notification = NotificationCompat.Builder(context.applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setCategory(category)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(defaultSound)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS or NotificationCompat.DEFAULT_VIBRATE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context.applicationContext).notify(notifId, notification)
        } catch (_: SecurityException) {
            // Permission revoked mid-flight; UI banners still notify user inside app.
        }
    }
}
