package com.tm.zircon

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.*
import java.util.*

class LanguageActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language)

        val listView: ListView = findViewById(R.id.languageListView)
        val languages = listOf("Русский", "English", "Deutsch")
        val codes = listOf("ru", "en", "de")

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, languages)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedLang = codes[position]
            setLocale(selectedLang)
        }
    }

    private fun setLocale(languageCode: String) {
        val prefs = getSharedPreferences("ZirconPrefs", MODE_PRIVATE)
        prefs.edit().putString("app_lang", languageCode).apply()

        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)

        // Перезапускаем MainActivity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
