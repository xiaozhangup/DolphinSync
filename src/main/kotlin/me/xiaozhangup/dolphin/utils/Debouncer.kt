package me.xiaozhangup.dolphin.utils

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class Debouncer(private val delayMillis: Long) {
    private val tasks = ConcurrentHashMap<String, ScheduledFuture<*>>()
    private val scheduler = Executors.newSingleThreadScheduledExecutor {
        Thread(it, "DebouncerThread").apply { isDaemon = true }
    }

    fun submit(key: String, action: () -> Unit) {
        tasks.remove(key)?.cancel(false)

        val future = scheduler.schedule({
            try {
                action()
            } finally {
                tasks.remove(key)
            }
        }, delayMillis, TimeUnit.MILLISECONDS)

        tasks[key] = future
    }

    fun shutdown() {
        scheduler.shutdownNow()
        tasks.clear()
    }
}
