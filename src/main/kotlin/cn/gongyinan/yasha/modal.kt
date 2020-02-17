package cn.gongyinan.yasha

import cn.gongyinan.yasha.task.YashaTask
import cn.gongyinan.yasha.utils.parseFormData
import cn.gongyinan.yasha.utils.parseQuery
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.lang.StringBuilder
import java.net.URI

enum class ResponseType {
    Html, Text, Json, XML, Binary, JavaScript
}

class FetchResult(val task: YashaTask, val responseHeaders: List<Pair<String, String>>, val responseCode: Int, val responseUri: URI, var subTasks: List<YashaTask>,
                  var responseType: ResponseType, val contentType: String,
                  var rawData: ByteArray, var zippedData: ByteArray?, var bodyString: String?, val useTime: Long) {

    val document: Document by lazy {
        if (bodyString != null) {
            Jsoup.parse(bodyString)
        } else {
            Jsoup.parse("")
        }
    }
    val json: JSONObject by lazy {
        JSON.parseObject(bodyString)
    }
    val jsonArray: JSONArray by lazy {
        JSON.parseArray(bodyString)
    }

    val requestUriQueryParams: HashMap<String, String> by lazy {
        this.task.uri.parseQuery()
    }

    val responseUriQueryParams: HashMap<String, String> by lazy {
        this.responseUri.parseQuery()
    }

    val requestFormDataParams: HashMap<String, String> by lazy {
        String(this.task.requestBody!!).parseFormData()
    }


}

class FakeResponse(val uri: URI, val code: Int, val headers: List<Pair<String, String>>, val body: ByteArray) {
    fun header(key: String): String? {
        return headers.firstOrNull { pair -> pair.first.equals(key, true) }?.second
    }
}

class YashaDbModal(val taskIdentifier: String, val taskDepth: Int,
                   val requestUrl: String, val requestMethod: String, var requestHeadersData: String? = null, val requestBody: ByteArray? = null, val taskCommand: String,
                   var subTaskCommands: Array<String>? = null, var responseUrl: String? = null, var contentType: String? = null, var responseBody: ByteArray? = null, var responseCode: Int? = null, var responseHeadersData: String? = null,
                   var success: Boolean = false, var ready: Boolean = true, val createTime: Long = System.currentTimeMillis(), var updateTime: Long = System.currentTimeMillis(),
                   var parentTaskIdentifier: String? = null, var extraData: ByteArray? = null) {
    //响应头的key可能重复
    var responseHeaders: List<Pair<String, String>>?
        get() {
            if (responseHeadersData != null) {
                val result = ArrayList<Pair<String, String>>()
                for (line in responseHeadersData!!.split("\n")) {
                    val kv = line.split(Regex(":"), 2)
                    if (kv.size == 2) {
                        result.add(Pair(kv[0], kv[1]))
                    }
                }
                return result
            } else {
                return null
            }
        }
        set(value) {
            if (value == null) {
                this.responseHeadersData = null
                return
            }
            val stringBuilder = StringBuilder()
            for (i in value.indices) {
                val kv = value[i]
                stringBuilder.append("${kv.first}:${kv.second}")
                if (i != value.size - 1) {
                    stringBuilder.append("\n")
                }
            }
            this.responseHeadersData = stringBuilder.toString()
        }

    var requestHeaders: Map<String, String>?
        get() {
            if (requestHeadersData != null) {
                val result = HashMap<String, String>()
                for (line in requestHeadersData!!.split("\n")) {
                    val kv = line.split(Regex(":"), 2)
                    if (kv.size == 2) {
                        result[kv[0]] = kv[1]
                    }
                }
                return result
            } else {
                return null
            }
        }
        set(value) {
            if (value == null) {
                this.requestHeadersData = null
                return
            }
            val headersLines = value.keys.map { key ->
                "$key:${value[key]}"
            }
            this.requestHeadersData = headersLines.joinToString("\n")
        }

    @Deprecated("用转换器")
    fun toYashaTask(): YashaTask {
        return YashaTask(
                URI(requestUrl),
                taskDepth,
                requestBody,
                requestMethod,
                requestHeaders,
                extraData,
                parentTaskIdentifier,
                taskIdentifier,
                taskCommand,
                createTime
        )
    }

}
