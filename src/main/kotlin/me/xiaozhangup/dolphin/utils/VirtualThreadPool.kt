package me.xiaozhangup.dolphin.utils

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class VirtualThreadPool : AutoCloseable {
    private val executor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()

    fun submit(task: () -> Unit): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        val wrappedTask = Runnable {
            try {
                task()
                future.complete(null)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        executor.execute(wrappedTask)
        return future
    }

    override fun close() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    companion object {
        val pool = VirtualThreadPool()
    }
}

fun submitVirtual(task: () -> Unit): CompletableFuture<Void> {
    return VirtualThreadPool.pool.submit(task)
}