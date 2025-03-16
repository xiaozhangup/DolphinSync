package me.xiaozhangup.dolphin.utils

import kotlinx.coroutines.*

object CoroutineTask {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun submit(period: Long = 0L, delay: Long = 0L, block: suspend TaskScope.() -> Unit): Job {
        val job = scope.launch {
            if (delay > 0) delay(delay)
            val taskScope = TaskScope(this)
            if (period > 0) {
                while (isActive) {
                    taskScope.block()
                    delay(period)
                }
            } else {
                taskScope.block()
            }
        }
        return job
    }

    fun Job.cancelTask() {
        this.cancel()
    }
}

class TaskScope(private val job: CoroutineScope) {
    fun cancel() {
        job.cancel()
    }
}

fun submitScope(period: Long = 0L, delay: Long = 0L, block: suspend TaskScope.() -> Unit): Job {
    return CoroutineTask.submit(period, delay, block)
}