package com.watchguard

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.media.RingtoneManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.*
import android.service.notification.NotificationListenerService

class WatchListener : NotificationListenerService() {
    private val handler = Handler(Looper.getMainLooper())
    private val ytPackages = setOf("com.google.android.youtube", "com.google.android.apps.youtube.music", "app.revanced.android.youtube")

    private val tick = object : Runnable {
        override fun run() { try { check() } catch (_: Exception) {}; handler.postDelayed(this, 1000) }
    }

    override fun onListenerConnected() { handler.removeCallbacks(tick); handler.post(tick) }
    override fun onListenerDisconnected() { handler.removeCallbacks(tick) }
    override fun onDestroy() { handler.removeCallbacks(tick); super.onDestroy() }

    private fun check() {
        if (!Monitor.running) return
        val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val sessions: List<MediaController> = msm.getActiveSessions(ComponentName(this, WatchListener::class.java))
        val playing = sessions.filter { it.packageName in ytPackages && it.playbackState?.state == PlaybackState.STATE_PLAYING }
        if (playing.isEmpty()) { Monitor.status = "Paused / not playing"; return }
        val title = playing[0].metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
        if (title.isNullOrBlank()) { Monitor.status = "Loading / ad"; return }
        if (Monitor.matches(title)) { Monitor.seconds++; Monitor.status = "Watching target video" }
        else { Monitor.stop(); Monitor.alert = true; Monitor.status = "RED ALERT: $title"; fireAlert(title) }
    }

    private fun fireAlert(title: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("alert", "Red Alert", NotificationManager.IMPORTANCE_HIGH))
        val pi = PendingIntent.getActivity(this, 0, android.content.Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, "alert").setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("\uD83D\uDD34 RED ALERT").setContentText("Different video playing: $title")
            .setColor(0xFFD32F2F.toInt()).setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(pi, true).setContentIntent(pi).setAutoCancel(true).build()
        nm.notify(1, n)
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 300, 500, 300, 500), -1))
        val r = RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
        r?.play(); handler.postDelayed({ r?.stop() }, 5000)
    }
}
