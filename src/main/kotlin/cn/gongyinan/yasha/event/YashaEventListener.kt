package cn.gongyinan.yasha.event

import cn.gongyinan.yasha.Yasha
import cn.gongyinan.yasha.YashaConfig
import cn.gongyinan.yasha.finder.DocumentFinder
import cn.gongyinan.yasha.modals.FakeResponse
import cn.gongyinan.yasha.modals.FetchResult
import cn.gongyinan.yasha.task.YashaTask
import cn.gongyinan.yasha.task.filter.CompositeTaskFilter
import cn.gongyinan.yasha.task.filter.EmptyTaskFilter
import cn.gongyinan.yasha.task.filter.ITaskFilter
import cn.gongyinan.yasha.task.filter.TaskFilter
import okhttp3.Dispatcher
import okhttp3.OkHttpClient


open class YashaEventListener(func: (YashaEventListener.() -> Unit)?) : FilterContext(EmptyTaskFilter()), IYashaEventListener {

    internal val onResponseFuncList = ArrayList<ResponseEventMethod>()
    internal val onRequestFuncList = ArrayList<RequestEventMethod>()
    internal val onCreateHttpClientFuncList = ArrayList<CreateHttpClientEventMethod>()
    internal val onCheckResponseMethodFuncList = ArrayList<CheckResponseMethod>()
    internal val onCheckCacheMethodFuncList = ArrayList<CheckCacheMethod>()
    internal val onTaskFinderEventMethodFuncList = ArrayList<TaskFinderEventMethod>()
    internal val beforeTaskFinderEventMethodFuncList = ArrayList<BeforeTaskFinderEventMethod>()
    internal val afterTaskFinderEventMethodFuncList = ArrayList<AfterTaskFinderEventMethod>()
    internal val onErrorEventMethodFuncList = ArrayList<ErrorEventMethod>()

    init {
        listener(this)
        func?.invoke(this)
    }

    override lateinit var yashaConfig: YashaConfig
    override lateinit var yasha: Yasha

    internal data class ResponseEventMethod(val filter: ITaskFilter, val func: (FetchResult) -> Unit)
    internal data class RequestEventMethod(val filter: ITaskFilter, val func: (YashaTask) -> Unit)
    internal data class CheckCacheMethod(val filter: ITaskFilter, val func: (YashaTask) -> FakeResponse?)
    internal data class CheckResponseMethod(val filter: ITaskFilter, val func: (FetchResult) -> Boolean)
    internal data class CreateHttpClientEventMethod(val filter: ITaskFilter, val func: (YashaTask) -> OkHttpClient)
    internal data class TaskFinderEventMethod(val filter: ITaskFilter, val func: (FetchResult) -> List<YashaTask>)
    internal data class BeforeTaskFinderEventMethod(val filter: ITaskFilter, val func: (FetchResult) -> Unit)
    internal data class AfterTaskFinderEventMethod(val filter: ITaskFilter, val func: (FetchResult, List<YashaTask>) -> List<YashaTask>)
    internal data class ErrorEventMethod(val filter: ITaskFilter, val func: (YashaTask, Throwable) -> Unit)

    override fun onResponse(fetchResult: FetchResult) {
        for (method in onResponseFuncList) {
            if (method.filter.filter(fetchResult.task)) {
                method.func(fetchResult)
                return
            }
        }
    }

    override fun onRequest(yashaTask: YashaTask) {
        val headers = HashMap(yashaTask.headers ?: mapOf())
        headers.putAll(yashaConfig.defaultHeaders)
        yashaTask.headers = headers

        for (method in onRequestFuncList) {
            if (method.filter.filter(yashaTask)) {
                method.func(yashaTask)
                return
            }
        }
    }

    val defaultDispatcher by lazy {
        val dispatcher = Dispatcher()
        dispatcher.maxRequests = yashaConfig.threadNum
        dispatcher.maxRequestsPerHost = yashaConfig.threadNum
        dispatcher
    }

    private val defaultOkHttpClient by lazy {
        OkHttpClient().newBuilder().dispatcher(defaultDispatcher).build()
    }

    override fun onCreateHttpClient(yashaTask: YashaTask): OkHttpClient {
        for (method in onCreateHttpClientFuncList) {
            if (method.filter.filter(yashaTask)) {
                return method.func(yashaTask)
            }
        }
        return defaultOkHttpClient
    }

    override fun onCheckResponse(fetchResult: FetchResult): Boolean {
        for (method in onCheckResponseMethodFuncList) {
            if (method.filter.filter(fetchResult.task)) {
                return method.func(fetchResult)
            }
        }
        return fetchResult.responseCode in 200..299
    }

    override fun onCheckCache(yashaTask: YashaTask): FakeResponse? {
        for (method in onCheckCacheMethodFuncList) {
            if (method.filter.filter(yashaTask)) {
                return method.func(yashaTask)
            }
        }
        return null
    }

    override fun onTaskFinder(fetchResult: FetchResult): List<YashaTask> {
        for (method in onTaskFinderEventMethodFuncList) {
            if (method.filter.filter(fetchResult.task)) {
                return method.func(fetchResult)
            }
        }
        return defaultTaskFinder(fetchResult)
    }

    override fun afterTaskFinder(fetchResult: FetchResult, tasks: List<YashaTask>): List<YashaTask> {
        for (method in afterTaskFinderEventMethodFuncList) {
            if (method.filter.filter(fetchResult.task)) {
                return method.func(fetchResult, tasks)
            }
        }
        return tasks
    }

    override fun beforeTaskFinder(fetchResult: FetchResult) {
        for (method in beforeTaskFinderEventMethodFuncList) {
            if (method.filter.filter(fetchResult.task)) {
                method.func(fetchResult)
                return
            }
        }
    }

    open fun defaultTaskFinder(fetchResult: FetchResult): List<YashaTask> {
        return DocumentFinder.findUrl(fetchResult, yashaConfig.taskFilterList, yashaConfig.taskFilterBlackList)
                .map { uri -> this.onCreateDefaultGetTask(uri, fetchResult.task.taskDepth + 1, fetchResult.task.taskIdentifier) }
    }

    override fun onError(yashaTask: YashaTask, e: Throwable) {
        for (method in onErrorEventMethodFuncList) {
            if (method.filter.filter(yashaTask)) {
                method.func(yashaTask, e)
                return
            }
        }
    }

}