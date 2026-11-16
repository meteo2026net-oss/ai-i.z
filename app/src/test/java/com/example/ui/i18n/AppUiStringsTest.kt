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
    fun authAndStudioChromeAreLocalized() {
        assertTrue(AppUiStrings.googleSignIn("en").contains("Google"))
        assertTrue(AppUiStrings.googleSignIn("ar").contains("Google"))
        assertTrue(AppUiStrings.studioLibraryTitle("en").contains("library"))
        assertTrue(AppUiStrings.studioDeleteBody("en", "Demo").contains("Demo"))
        assertTrue(AppUiStrings.studioDeleteBody("ar", "Demo").contains("Demo"))
        assertTrue(AppUiStrings.studioBuilding("en").contains("Building"))
    }
}
