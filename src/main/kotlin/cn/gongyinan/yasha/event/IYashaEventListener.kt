package cn.gongyinan.yasha.event

import cn.gongyinan.yasha.*
import cn.gongyinan.yasha.modals.FakeResponse
import cn.gongyinan.yasha.modals.FetchResult
import cn.gongyinan.yasha.task.YashaGetTask
import cn.gongyinan.yasha.task.YashaTask
import okhttp3.OkHttpClient
import java.net.URI

interface IYashaEventListener {

    var yashaConfig: YashaConfig

    var yasha: Yasha

    fun onResponse(fetchResult: FetchResult)

    fun onRequest(yashaTask: YashaTask)

    fun onCreateHttpClient(yashaTask: YashaTask): OkHttpClient

    fun onCheckResponse(fetchResult: FetchResult): Boolean

    fun onCheckCache(yashaTask: YashaTask): FakeResponse?

    fun onTaskFinder(fetchResult: FetchResult): List<YashaTask>

    fun onError(yashaTask: YashaTask, e: Throwable)

    fun onCreateDefaultGetTask(uri: URI, depth: Int, parentTaskIdentifier: String?): YashaTask {
        return YashaGetTask(
                uri, depth,
                parentTaskIdentifier = parentTaskIdentifier
        )
    }

    fun onTaskClassifier(task: YashaTask): String? {
        for (classifier in yashaConfig.taskClassifierList) {
            val tag = classifier.classifier(task)
            if (tag != null) {
                return tag
            }
        }
        return null
    }

}