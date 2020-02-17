package cn.gongyinan.yasha.modals

import cn.gongyinan.yasha.task.YashaTask
import cn.gongyinan.yasha.utils.parseFormData
import cn.gongyinan.yasha.utils.parseQuery
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
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

    val gson: JsonObject by lazy {
        JsonParser().parse(bodyString).asJsonObject
    }

    val gsonArray: JsonArray by lazy {
        JsonParser().parse(bodyString).asJsonArray
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
