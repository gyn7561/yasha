package cn.gongyinan.yasha.event

import cn.gongyinan.yasha.*
import cn.gongyinan.yasha.finder.DocumentFinder
import okhttp3.OkHttpClient

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
        return fetchResult.response.code in 200..299
    }

    override fun onTaskFinder(fetchResult: FetchResult): List<YashaTask> {
        return DocumentFinder.findUrl(fetchResult, yashaConfig.blackListRegexList, yashaConfig.filterRegexList)
            .map { uri ->
                YashaGetTask(
                    uri,
                    fetchResult.task.taskDepth + 1,
                    parentTaskIdentifier = fetchResult.task.taskIdentifier
                )
            }
    }

    override fun onError(yashaTask: YashaTask, e: Throwable) {

    }
}