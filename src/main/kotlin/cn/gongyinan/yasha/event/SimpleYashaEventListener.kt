package cn.gongyinan.yasha.event

import cn.gongyinan.yasha.*
import cn.gongyinan.yasha.finder.DocumentFinder
import cn.gongyinan.yasha.modals.FakeResponse
import cn.gongyinan.yasha.modals.FetchResult
import cn.gongyinan.yasha.task.YashaTask
import okhttp3.OkHttpClient
@Deprecated("弃用")
open class SimpleYashaEventListener : IYashaEventListener {

    override lateinit var yashaConfig: YashaConfig
    override lateinit var yasha: Yasha

    override fun onResponse(fetchResult: FetchResult) {

    }

    override fun onRequest(yashaTask: YashaTask) {
        yashaTask.headers = yashaConfig.defaultHeaders
    }

    private val okHttpClient = OkHttpClient()
    override fun onCreateHttpClient(yashaTask: YashaTask): OkHttpClient {
        return okHttpClient
    }

    override fun onCheckResponse(fetchResult: FetchResult): Boolean {
        return fetchResult.responseCode in 200..299
    }

    override fun onCheckCache(yashaTask: YashaTask): FakeResponse? {
        return null
    }

    override fun onTaskFinder(fetchResult: FetchResult): List<YashaTask> {
        return DocumentFinder.findUrl(fetchResult, yashaConfig.taskFilterList, yashaConfig.taskFilterBlackList)
                .map { uri -> this.onCreateDefaultGetTask(uri, fetchResult.task.taskDepth + 1, fetchResult.task.taskIdentifier) }
    }

    override fun onError(yashaTask: YashaTask, e: Throwable) {

    }
}