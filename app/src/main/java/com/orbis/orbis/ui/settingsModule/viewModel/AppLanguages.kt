package com.orbis.orbis.ui.settingsModule.viewModel

/**
 * Single source of truth for the languages Orbis can be displayed in.
 *
 * Each [Language] maps an ISO language code to the language's endonym.
 *
 * [DEVICE] is a sentinel meaning "follow the device language" and is handled separately
 */
object AppLanguages {
    const val DEVICE = "dd"

    val supported: List<Language> = listOf(
        Language("English", "en"),
        Language("中文", "zh"),
        Language("हिन्दी", "hi"),
        Language("Español", "es"),
        Language("Français", "fr"),
        Language("العربية", "ar"),
        Language("বাংলা", "bn"),
        Language("Português", "pt"),
        Language("Русский", "ru"),
        Language("اردو", "ur"),
        Language("Bahasa Indonesia", "in"),
        Language("Deutsch", "de"),
        Language("日本語", "ja"),
        Language("Kiswahili", "sw"),
        Language("मराठी", "mr"),
        Language("తెలుగు", "te"),
        Language("Türkçe", "tr"),
        Language("தமிழ்", "ta"),
        Language("Tiếng Việt", "vi"),
        Language("한국어", "ko"),
        Language("فارسی", "fa"),
        Language("Hausa", "ha"),
        Language("Basa Jawa", "jv"),
        Language("Italiano", "it"),
        Language("ਪੰਜਾਬੀ", "pa"),
        Language("ગુજરાતી", "gu"),
        Language("ಕನ್ನಡ", "kn"),
        Language("ไทย", "th"),
        Language("አማርኛ", "am"),
        Language("മലയാളം", "ml"),
        Language("Polski", "pl"),
        Language("Українська", "uk"),
        Language("Tagalog", "tl"),
        Language("Yorùbá", "yo"),
        Language("ଓଡ଼ିଆ", "or"),
        Language("မြန်မာ", "my"),
        Language("Basa Sunda", "su"),
        Language("Română", "ro"),
        Language("Nederlands", "nl"),
        Language("Igbo", "ig"),
        Language("नेपाली", "ne"),
        Language("සිංහල", "si"),
        Language("ខ្មែរ", "km"),
        Language("پښتو", "ps"),
        Language("Ελληνικά", "el"),
        Language("Magyar", "hu"),
        Language("Bahasa Melayu", "ms"),
        Language("isiZulu", "zu"),
    )
    fun displayNameFor(code: String?): String? =
        supported.firstOrNull { it.key.equals(code, ignoreCase = true) }?.name
    
    fun indexOf(code: String?): Int =
        supported.indexOfFirst { it.key.equals(code, ignoreCase = true) }
}
