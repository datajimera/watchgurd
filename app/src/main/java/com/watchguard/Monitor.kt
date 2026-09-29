package com.watchguard

object Monitor {
    @Volatile var target: String = ""
    @Volatile var running = false
    @Volatile var seconds = 0
    @Volatile var status = "Idle"
    @Volatile var alert = false

    fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() }
    fun matches(current: String) = norm(current).let { c ->
        val t = norm(target); t.isNotEmpty() && c.isNotEmpty() && (c.contains(t) || t.contains(c))
    }
    fun start(title: String) { target = title; seconds = 0; alert = false; running = true; status = "Waiting for video" }
    fun stop() { running = false; status = "Stopped" }
}
