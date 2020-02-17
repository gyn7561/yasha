package cn.gongyinan.yasha.task

import org.apache.commons.codec.digest.DigestUtils
import java.net.URI

open class YashaTask(open val uri: URI, open val taskDepth: Int,
                     open var requestBody: ByteArray?, open var method: String,
                     open var headers: Map<String, String>?,
                     open var extraData: ByteArray? = null,
                     open val parentTaskIdentifier: String? = null,
                     open val taskIdentifier: String = DigestUtils.md5Hex(uri.toString()),
                     open var taskCommand: String = "GET $uri",
                     open val createTime: Long = System.currentTimeMillis(), var tag: Any? = null) {

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("[$method] [depth:$taskDepth] $taskIdentifier $taskCommand")
        return stringBuilder.toString()
    }
}

internal class TempTaskFilterTask(uri: URI) : YashaTask(uri, 0, null, "GET", null, null, null, "", "")

open class YashaGetTask(override val uri: URI, override val taskDepth: Int = 0, override var headers: Map<String, String>? = null,
                        override val taskIdentifier: String = DigestUtils.md5Hex(uri.toString()),
                        override var taskCommand: String = "GET $uri",
                        override val parentTaskIdentifier: String? = null) : YashaTask(uri, taskDepth, null, "GET", headers) {

}

