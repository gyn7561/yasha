package cn.gongyinan.yasha

import cn.gongyinan.yasha.db.ITaskDb
import cn.gongyinan.yasha.db.MemoryTaskDb
import cn.gongyinan.yasha.db.SimpleMemoryTaskDb
import cn.gongyinan.yasha.event.AbstractYashaEventListener
import cn.gongyinan.yasha.event.IYashaEventListener
import cn.gongyinan.yasha.finder.DocumentFinder
import okhttp3.OkHttpClient
import java.net.URI
import javax.swing.text.Document

class YashaConfig(
    val threadNum: Int = 1,
    val intervalInMs: Long = 100,
    val listener: IYashaEventListener,
    val initUrl: Array<String> = arrayOf(),
    val filterRegexList: Array<Regex> = arrayOf(),
    val blackListRegexList: Array<Regex> = arrayOf(),
    val maxDepth: Int = 1000,
    val retryCount: Int = 5,
    val taskDb: ITaskDb = SimpleMemoryTaskDb(),
    val defaultUserAgent: String = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36",
    val defaultHeaders: Map<String, String> = mapOf("User-Agent" to defaultUserAgent, "Accept" to "*/*", "Connection" to "keep-alive", "Accept-Encoding" to "gzip, deflate, br")

//        val createHttpHeaderFunc: (YashaTask) -> Map<String, String> = { _ -> mapOf("User-Agent" to defaultUserAgent, "Accept" to "*/*", "Connection" to "keep-alive", "Accept-Encoding" to "gzip, deflate, br") },
//        val createHttpClientFunc: (YashaTask) -> OkHttpClient = { _ -> OkHttpClient() },
//        val onResponseFunc: (FetchResult) -> Unit = { _ -> },
//        val taskFinderFunc: (FetchResult) -> List<YashaTask> =
//                { result -> DocumentFinder.findUrl(result, blackListRegexList, filterRegexList).map { uri -> YashaGetTask(uri, result.task.taskDepth + 1, parentTaskIdentifier = result.task.taskIdentifier) } },
//        val responseCheckFunc: (FetchResult) -> Boolean = { _ -> true },
//        val onErrorFunc: (YashaTask, Throwable) -> Unit = { _, _ -> }
) {
    init {
        listener.yashaConfig = this
    }
}
