package cn.gongyinan.yasha.utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object GZipUtil {
    //0x1f8b
    private val MAGIC_NUMBER = arrayOf((0x1f).toByte(), (0x8b).toByte())

    fun isGzipData(bytes: ByteArray): Boolean {
        return bytes.size > 2 && bytes[0] == MAGIC_NUMBER[0] && bytes[1] == MAGIC_NUMBER[1]
    }

    fun uncompress(bytes: ByteArray): ByteArray {
        if (bytes.isEmpty()) {
            return ByteArray(0)
        }
        val out = ByteArrayOutputStream()
        val byteArrayInputStream = ByteArrayInputStream(bytes)
        val ungzip = GZIPInputStream(byteArrayInputStream)
        val buffer = ByteArray(256)
        var n = ungzip.read(buffer)
        while (n >= 0) {
            out.write(buffer, 0, n)
            n = ungzip.read(buffer)
        }
        return out.toByteArray()
    }

    fun compress(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        val gzip = GZIPOutputStream(out)
        gzip.write(data)
        gzip.close()
        return out.toByteArray()!!
    }

}