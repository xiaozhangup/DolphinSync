package me.xiaozhangup.dolphin.utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object GzipUtils {
    fun compress(input: String): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val gzipOutputStream = GZIPOutputStream(byteArrayOutputStream)
        gzipOutputStream.write(input.toByteArray())
        gzipOutputStream.close()
        return byteArrayOutputStream.toByteArray()
    }

    fun decompress(compressed: ByteArray): String? {
        if (compressed.isEmpty()) {
            return null
        }
        val byteArrayInputStream = ByteArrayInputStream(compressed)
        val gzipInputStream = GZIPInputStream(byteArrayInputStream)
        val byteArrayOutputStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var len: Int
        while (gzipInputStream.read(buffer).also { len = it } != -1) {
            byteArrayOutputStream.write(buffer, 0, len)
        }
        gzipInputStream.close()
        byteArrayOutputStream.close()
        return byteArrayOutputStream.toString("UTF-8")
    }
}