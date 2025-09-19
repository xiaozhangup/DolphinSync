package me.xiaozhangup.dolphin.utils.obj

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object CoroutineTask {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val tagMutexes = ConcurrentHashMap<String, TagMutexInfo>()

    @Volatile
    private var forceSync = false

    fun setForceSync(value: Boolean) { forceSync = value }
    fun isForceSync(): Boolean = forceSync

    fun submit(
        tag: String,
        period: Long = 0L,
        block: suspend TaskScope.() -> Unit
    ): Job {
        return scope.launch {
            val mi = acquireTagMutex(tag)
            try {
                mi.mutex.withLock {
                    val job = coroutineContext[Job]!!
                    val ts = TaskScope(job)
                    if (period > 0L) {
                        while (isActive) {
                            val start = System.nanoTime()
                            ts.block()
                            val elapsedMs = (System.nanoTime() - start) / 1_000_000
                            val wait = period - elapsedMs
                            if (wait > 0) delay(wait)
                        }
                    } else {
                        ts.block()
                    }
                }
            } finally {
                releaseTagMutex(tag)
            }
        }
    }

    private fun acquireTagMutex(tag: String): TagMutexInfo {
        return tagMutexes.compute(tag) { _, existing ->
            if (existing == null) TagMutexInfo() else {
                existing.refCount.incrementAndGet()
                existing
            }
        }!!
    }

    private fun releaseTagMutex(tag: String) {
        tagMutexes.computeIfPresent(tag) { _, existing ->
            if (existing.refCount.decrementAndGet() <= 0) null else existing
        }
    }

    private class TagMutexInfo(
        val mutex: Mutex = Mutex(),
        val refCount: AtomicInteger = AtomicInteger(1)
    )
}

class TaskScope(private val job: Job) {
    fun cancel() = job.cancel()
}

fun submitScope(
    tag: String = "default",
    period: Long = 0L,
    block: suspend TaskScope.() -> Unit
): Job {
    val job = CoroutineTask.submit(tag, period, block)
    if (CoroutineTask.isForceSync()) runBlocking { job.join() }
    return job
}