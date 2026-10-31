package app.dozdefteri.core.locale

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi

/** In-app language choice (Android 13+ per-app language; earlier versions follow the system). */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    TURKISH("tr"),
    ENGLISH("en");

    companion object {
        val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

        fun current(context: Context): AppLanguage {
            if (!isSupported) return SYSTEM
            return fromLocales(context)
        }

        fun set(context: Context, language: AppLanguage) {
            if (isSupported) apply(context, language)
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private fun fromLocales(context: Context): AppLanguage {
            val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales ?: return SYSTEM
            if (locales.isEmpty) return SYSTEM
            return when (locales[0].language) {
                "tr" -> TURKISH
                "en" -> ENGLISH
                else -> SYSTEM
            }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private fun apply(context: Context, language: AppLanguage) {
            val manager = context.getSystemService(LocaleManager::class.java) ?: return
            manager.applicationLocales = language.tag?.let { LocaleList.forLanguageTags(it) }
                ?: LocaleList.getEmptyLocaleList()
        }
    }
}
