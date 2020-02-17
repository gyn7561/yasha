package cn.gongyinan.yasha

import cn.gongyinan.yasha.task.db.ITaskDb
import cn.gongyinan.yasha.task.db.SimpleMemoryTaskDb
import cn.gongyinan.yasha.event.IYashaEventListener
import cn.gongyinan.yasha.task.classifier.ITaskClassifier
import cn.gongyinan.yasha.task.filter.ITaskFilter


class YashaConfig(
        val threadNum: Int = 1,
        val intervalInMs: Long = 100,
        val listener: IYashaEventListener,
        val initUrl: Array<String> = arrayOf(),
        val taskClassifierList: Array<ITaskClassifier> = arrayOf(),
//        val filterRegexList: Array<Regex> = arrayOf(),
//        val blackListRegexList: Array<Regex> = arrayOf(),
        val taskFilterList: Array<ITaskFilter> = arrayOf(),
        val taskFilterBlackList: Array<ITaskFilter> = arrayOf(),
        val maxDepth: Int = 1000,
        val retryCount: Int = 5,
        val taskDb: ITaskDb = SimpleMemoryTaskDb(),
        val defaultUserAgent: String = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36",
        val defaultHeaders: Map<String, String> = mapOf("User-Agent" to defaultUserAgent, "Accept" to "*/*", "Connection" to "keep-alive", "Accept-Encoding" to "gzip")
) {
    init {
        listener.yashaConfig = this
    }
}
