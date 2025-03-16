package me.xiaozhangup.dolphin.utils

import java.lang.System.currentTimeMillis

class ExecutionTimer {
    var startTime: Long = currentTimeMillis()

    fun pop(): Long {
        val endTime = currentTimeMillis()
        val duration = endTime - startTime
        startTime = endTime
        return duration
    }
}