package cn.gongyinan.yasha.utils

import org.apache.commons.compress.compressors.brotli.BrotliCompressorInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

object BrUtil {
    fun uncompress(bytes: ByteArray): ByteArray {
        if (bytes.isEmpty()) {
            return ByteArray(0)
        }
        val out = ByteArrayOutputStream()
        val byteArrayInputStream = ByteArrayInputStream(bytes)
        val ungzip = BrotliCompressorInputStream(byteArrayInputStream)
        val buffer = ByteArray(256)
        var n = ungzip.read(buffer)
        while (n >= 0) {
            out.write(buffer, 0, n)
            n = ungzip.read(buffer)
        }
        return out.toByteArray()
    }

}