package me.xiaozhangup.dolphin.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateFormatter {
    /**
     * 将时间戳转换为中文格式日期时间
     * @param timestamp 毫秒级时间戳
     * @param zoneId 时区（默认上海时区）
     * @return 格式示例：2025年7月28号 14:40
     */
    fun formatToChineseDateTime(
        timestamp: Long,
        zoneId: ZoneId = ZoneId.of("Asia/Shanghai")
    ): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")
            .withZone(zoneId)
        return formatter.format(Instant.ofEpochMilli(timestamp))
    }
}