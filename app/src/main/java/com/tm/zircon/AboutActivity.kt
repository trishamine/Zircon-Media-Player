package com.tm.zircon

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class AboutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textView = TextView(this)
        textView.text = "Zircon v1.0\n\n(Заглушка экрана About)"
        textView.textSize = 20f
        textView.setPadding(32, 32, 32, 32)
        setContentView(textView)
    }
}
