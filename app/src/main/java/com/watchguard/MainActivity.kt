package com.watchguard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var link: EditText
    private lateinit var title: TextView
    private lateinit var timer: TextView
    private lateinit var status: TextView
    private lateinit var btn: Button

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun btnStyle(b: Button, color: Int) {
        b.setTextColor(Color.WHITE); b.isAllCaps = false; b.textSize = 16f
        b.background = GradientDrawable().apply { setColor(color); cornerRadius = dp(14).toFloat() }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(48), dp(24), dp(24)); setBackgroundColor(Color.parseColor("#0F1115"))
        }
        fun tv(t: String, size: Float, color: String, bold: Boolean = false) = TextView(this).apply {
            text = t; textSize = size; setTextColor(Color.parseColor(color)); if (bold) typeface = Typeface.DEFAULT_BOLD
        }
        root.addView(tv("WatchGuard", 28f, "#FFFFFF", true))
        root.addView(tv("Focus on one YouTube video. Get alerted if you switch.", 14f, "#9AA0A6"))
        link = EditText(this).apply {
            hint = "Paste YouTube link"; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE); setSingleLine()
            background = GradientDrawable().apply { setColor(Color.parseColor("#1B1F27")); cornerRadius = dp(12).toFloat() }
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        root.addView(link, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(28) })
        btn = Button(this).apply { text = "Start monitoring"; btnStyle(this, Color.parseColor("#2E7D32")); setOnClickListener { toggle() } }
        root.addView(btn, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(16) })
        title = tv("", 15f, "#E8EAED").apply { gravity = Gravity.CENTER }
        root.addView(title, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(32) })
        timer = tv("00:00:00", 54f, "#FFFFFF", true).apply { gravity = Gravity.CENTER }
        root.addView(timer, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        status = tv("Idle", 16f, "#9AA0A6").apply { gravity = Gravity.CENTER }
        root.addView(status, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        setContentView(root, ViewGroup.LayoutParams(-1, -1))
    }

    private fun listenerEnabled() =
        Settings.Secure.getString(contentResolver, "enabled_notification_listeners")?.contains(packageName) == true

    private fun toggle() {
        if (Monitor.running) { Monitor.stop(); return }
        if (!listenerEnabled()) {
            Toast.makeText(this, "Enable Notification Access for WatchGuard", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); return
        }
        val url = link.text.toString().trim()
        if (url.isEmpty()) { link.error = "Paste a link"; return }
        Monitor.status = "Fetching video title..."
        Thread {
            try {
                val api = "https://www.youtube.com/oembed?format=json&url=" + URLEncoder.encode(url, "UTF-8")
                val c = URL(api).openConnection() as HttpURLConnection
                val t = JSONObject(c.inputStream.bufferedReader().readText()).getString("title")
                runOnUiThread { Monitor.start(t) }
            } catch (e: Exception) {
                runOnUiThread { Monitor.status = "Invalid link or no internet" }
            }
        }.start()
    }

    private val refresh = object : Runnable {
        override fun run() {
            val s = Monitor.seconds
            timer.text = "%02d:%02d:%02d".format(s / 3600, s % 3600 / 60, s % 60)
            title.text = if (Monitor.target.isNotEmpty()) "🎯 ${Monitor.target}" else ""
            status.text = Monitor.status
            status.setTextColor(Color.parseColor(if (Monitor.alert) "#FF5252" else "#9AA0A6"))
            (timer.parent as LinearLayout).setBackgroundColor(Color.parseColor(if (Monitor.alert) "#3B0D0D" else "#0F1115"))
            btn.text = if (Monitor.running) "Stop" else "Start monitoring"
            btnStyle(btn, Color.parseColor(if (Monitor.running) "#C62828" else "#2E7D32"))
            h.postDelayed(this, 500)
        }
    }
    override fun onResume() { super.onResume(); h.post(refresh) }
    override fun onPause() { super.onPause(); h.removeCallbacks(refresh) }
}
