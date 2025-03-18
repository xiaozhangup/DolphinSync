package me.xiaozhangup.dolphin.utils

import java.lang.System.currentTimeMillis
import java.util.concurrent.TimeUnit

object BackupFilter {
    fun determineBackupsToRemove(timestamps: List<Long>): List<Long> {
        val sortedTimestamps = timestamps.sortedDescending()

        val currentTime = currentTimeMillis()
        val cutoff1Day = currentTime - TimeUnit.DAYS.toMillis(1)
        val cutoff7Days = currentTime - TimeUnit.DAYS.toMillis(7)
        val cutoff30Days = currentTime - TimeUnit.DAYS.toMillis(30)

        val (group1, remaining1) = partitionGroup(sortedTimestamps, cutoff1Day)
        val (group2, remaining2) = partitionGroup(remaining1, cutoff7Days)
        val (group3, remaining3) = partitionGroup(remaining2, cutoff30Days)

        return remaining3 +
                group1.drop(16) +
                group2.drop(7) +
                group3.drop(6)
    }

    private fun partitionGroup(
        timestamps: List<Long>,
        cutoff: Long
    ): Pair<List<Long>, List<Long>> {
        val group = timestamps.takeWhile { it >= cutoff }
        val remaining = timestamps.drop(group.size)
        return Pair(group, remaining)
    }
}