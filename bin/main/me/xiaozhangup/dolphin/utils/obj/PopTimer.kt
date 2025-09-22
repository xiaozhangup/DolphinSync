package me.xiaozhangup.dolphin.utils.obj

class PopTimer {
    var startTime: Long = System.currentTimeMillis()

    fun pop(): Long {
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        startTime = endTime
        return duration
    }
}