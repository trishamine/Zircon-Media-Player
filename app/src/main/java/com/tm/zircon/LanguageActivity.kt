package com.tm.zircon

import android.app.Activity
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class LanguageActivity : Activity() {

    private lateinit var languageList: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language)

        title = getString(R.string.language)

        languageList = findViewById(R.id.languageList)

        val languages = listOf(
            LanguageItem("Русский", "ru"),
            LanguageItem("English", "en"),
            LanguageItem("Deutsch", "de")
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, languages.map { it.name })
        languageList.adapter = adapter

        languageList.setOnItemClickListener { _, _, position, _ ->
            val selectedLang = languages[position].code
            val appLocale = LocaleListCompat.forLanguageTags(selectedLang)
            AppCompatDelegate.setApplicationLocales(appLocale)

            Toast.makeText(this, getString(R.string.language_changed), Toast.LENGTH_SHORT).show()
            finish() // Закрываем активити после выбора
        }
    }

    data class LanguageItem(val name: String, val code: String)
}
