package cn.gongyinan.yasha.task

import cn.gongyinan.yasha.YashaDbModal
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

    fun toDbModal(): YashaDbModal {
        val dbModal = YashaDbModal(
                taskIdentifier,
                taskDepth = taskDepth,
                requestUrl = uri.toString(),
                taskCommand = taskCommand,
                requestBody = requestBody,
                ready = true,
                requestMethod = method,
                parentTaskIdentifier = parentTaskIdentifier,
                extraData = extraData,
                createTime = createTime
        )
        dbModal.requestHeaders = headers
        return dbModal
    }
}

open class YashaGetTask(override val uri: URI, override val taskDepth: Int = 0, override var headers: Map<String, String>? = null,
                        override val taskIdentifier: String = DigestUtils.md5Hex(uri.toString()),
                        override var taskCommand: String = "GET $uri",
                        override val parentTaskIdentifier: String? = null) : YashaTask(uri, taskDepth, null, "GET", headers) {

}

