package com.example.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUiStringsTest {

    @Test
    fun englishTabsAndTitlesDifferFromArabic() {
        assertEquals("Home", AppUiStrings.tabLabel("dashboard", "en"))
        assertEquals("الرئيسية", AppUiStrings.tabLabel("dashboard", "ar"))
        assertEquals("Studio", AppUiStrings.tabLabel("universal_studio", "en"))
        assertTrue(AppUiStrings.screenTitle("security", "en", "App").contains("Security"))
        assertTrue(AppUiStrings.screenTitle("security", "ar", "App").contains("الأمان"))
        assertTrue(AppUiStrings.isEnglish("en"))
        assertFalse(AppUiStrings.isEnglish("ar"))
    }

    @Test
    fun fileDownloadMessagesAreLocalized() {
        assertTrue(AppUiStrings.fileDownloadSaved("en", "a.kt").contains("Downloaded"))
        assertTrue(AppUiStrings.fileDownloadSaved("ar", "a.kt").contains("تنزيل"))
        assertTrue(AppUiStrings.deleteAccountConfirm("en").contains("Delete"))
        assertTrue(AppUiStrings.deleteAccountConfirm("ar").contains("حذف"))
    }

    @Test
    fun englishModeHasNoArabicScriptForCoreChrome() {
        val arabicLetter = Regex("\\p{IsArabic}")
        val samples = listOf(
            AppUiStrings.tabLabel("dashboard", "en"),
            AppUiStrings.tabLabel("tasks", "en"),
            AppUiStrings.screenTitle("chat", "en", "Demo"),
            AppUiStrings.dashboardOpenStudio("en"),
            AppUiStrings.tasksLaunch("en"),
            AppUiStrings.chatPlaceholder("en"),
            AppUiStrings.securityTitle("en"),
            AppUiStrings.signOut("en"),
            AppUiStrings.deleteAccount("en"),
            AppUiStrings.googleSignIn("en"),
            AppUiStrings.studioLibraryTitle("en"),
            AppUiStrings.logsEmptyCategory("en")
        )
        samples.forEach { text ->
            assertFalse("English copy leaked Arabic: $text", arabicLetter.containsMatchIn(text))
        }
    }

    @Test
    fun authAndStudioChromeAreLocalized() {
        assertTrue(AppUiStrings.googleSignIn("en").contains("Google"))
        assertTrue(AppUiStrings.googleSignIn("ar").contains("Google"))
        assertTrue(AppUiStrings.studioLibraryTitle("en").contains("library"))
        assertTrue(AppUiStrings.studioDeleteBody("en", "Demo").contains("Demo"))
        assertTrue(AppUiStrings.studioDeleteBody("ar", "Demo").contains("Demo"))
        assertTrue(AppUiStrings.studioBuilding("en").contains("Building"))
    }

    @Test
    fun projectsAndDatabaseChromeEnglishHasNoArabic() {
        val arabicLetter = Regex("\\p{IsArabic}")
        val samples = listOf(
            AppUiStrings.projectsManageTitle("en"),
            AppUiStrings.projectSaveActivate("en"),
            AppUiStrings.googleLinkedAccounts("en"),
            AppUiStrings.githubMultiAccounts("en"),
            AppUiStrings.dbLinkNew("en"),
            AppUiStrings.dbSaveConnection("en"),
            AppUiStrings.sqlReviewTitle("en"),
            AppUiStrings.actionActivate("en"),
            AppUiStrings.loadingGeneric("en"),
            AppUiStrings.retryAction("en")
        )
        samples.forEach { text ->
            assertFalse("English copy leaked Arabic: $text", arabicLetter.containsMatchIn(text))
        }
        assertTrue(AppUiStrings.projectsManageTitle("ar").isNotBlank())
        assertTrue(AppUiStrings.dbLinkNew("ar").isNotBlank())
    }


    @Test
    fun repoWorkspaceAndAiConvEnglishHasNoArabic() {
        val arabicLetter = Regex("\\p{IsArabic}")
        listOf(
            AppUiStrings.repoExplorerUnlinked("en"),
            AppUiStrings.repoCreateBranch("en"),
            AppUiStrings.repoActionsLog("en"),
            AppUiStrings.workspaceTitle("en"),
            AppUiStrings.workspaceStartCollab("en"),
            AppUiStrings.aiConvTitle("en"),
            AppUiStrings.aiConvEmpty("en")
        ).forEach { text ->
            assertFalse("English copy leaked Arabic: $text", arabicLetter.containsMatchIn(text))
        }
    }


    @Test
    fun remainingScreensEnglishHasNoArabic() {
        val arabicLetter = Regex("\\p{IsArabic}")
        listOf(
            AppUiStrings.linkedSaveBundle("en"),
            AppUiStrings.setupBackendRunnerTitle("en"),
            AppUiStrings.dbSecurityIsolationTitle("en"),
            AppUiStrings.testsStartReal("en"),
            AppUiStrings.aboutCopyright("en"),
            AppUiStrings.notifOnQuotaExhausted("en")
        ).forEach { text ->
            assertFalse("English copy leaked Arabic: $text", arabicLetter.containsMatchIn(text))
        }
    }


    @Test
    fun dashboardAndNotificationChromeEnglishHasNoArabic() {
        val arabicLetter = Regex("\\p{IsArabic}")
        listOf(
            AppUiStrings.githubConnected("en"),
            AppUiStrings.agentRunning("en"),
            AppUiStrings.overviewTitle("en"),
            AppUiStrings.contentDescExportZip("en"),
            AppUiStrings.notifTaskCompleted("en", "Demo"),
            AppUiStrings.notifQuotaWarning("en")
        ).forEach { text ->
            assertFalse("English copy leaked Arabic: $text", arabicLetter.containsMatchIn(text))
        }
    }


    @Test
    fun studioEnvironmentTitlesLocalizeWithoutChangingEnumNames() {
        val arabicLetter = Regex("\\p{IsArabic}")
        val en = AppUiStrings.environmentTitle("MOBILE_ANDROID", "en")
        val ar = AppUiStrings.environmentTitle("MOBILE_ANDROID", "ar")
        assertTrue(en.contains("Android"))
        assertFalse(arabicLetter.containsMatchIn(en))
        assertTrue(ar.isNotBlank())
        assertTrue(AppUiStrings.environmentCategory("BACKEND_NODE", "en").contains("Backend"))
        // Storage key remains the enum name, independent of display language
        assertEquals("MOBILE_ANDROID", com.example.domain.model.DevelopmentEnvironment.MOBILE_ANDROID.name)
        assertEquals("BACKEND_GO", com.example.domain.model.DevelopmentEnvironment.BACKEND_GO.name)
    }

}
