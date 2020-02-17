package cn.gongyinan.yasha.event

import cn.gongyinan.yasha.FakeResponse
import cn.gongyinan.yasha.FetchResult
import cn.gongyinan.yasha.Yasha
import cn.gongyinan.yasha.YashaConfig
import cn.gongyinan.yasha.finder.DocumentFinder
import cn.gongyinan.yasha.task.YashaTask
import cn.gongyinan.yasha.task.filter.ITaskFilter
import cn.gongyinan.yasha.task.filter.TaskFilter
import okhttp3.OkHttpClient


class YashaEventListener : IYashaEventListener {


    class FilterProxy(val filter: ITaskFilter, private val listener: YashaEventListener) {

        fun onResponse(func: FetchResult.(FetchResult) -> Unit) {
            listener.onResponseFuncList.add(ResponseEventMethod(filter) { fetchResult ->
                func.invoke(fetchResult, fetchResult)
            })
        }

        fun onCheckCache(func: YashaTask.(YashaTask) -> FakeResponse?) {
            listener.onCheckCacheMethodFuncList.add(CheckCacheMethod(filter) { task ->
                func(task, task)
            })
        }

        fun onRequest(func: YashaTask.(YashaTask) -> Unit) {
            listener.onRequestFuncList.add(RequestEventMethod(filter) { task ->
                func(task, task)
            })
        }

        fun onCheckResponse(func: FetchResult.(FetchResult) -> Boolean) {
            listener.onCheckResponseMethodFuncList.add(CheckResponseMethod(filter) { fetchResult ->
                func(fetchResult, fetchResult)
            })
        }

        fun onCreateHttpClient(func: YashaTask.(YashaTask) -> OkHttpClient) {
            listener.onCreateHttpClientFuncList.add(CreateHttpClientEventMethod(filter) { yashaTask ->
                func(yashaTask, yashaTask)
            })
        }

        fun onTaskFinder(func: FetchResult.(FetchResult) -> List<YashaTask>) {
            listener.onTaskFinderEventMethodFuncList.add(TaskFinderEventMethod(filter) { yashaTask ->
                func(yashaTask, yashaTask)
            })
        }

        fun onError(func: YashaTask.(YashaTask, Throwable) -> Unit) {
            listener.onErrorEventMethodFuncList.add(ErrorEventMethod(filter) { yashaTask, e ->
                func(yashaTask, yashaTask, e)
            })
        }

    }

    fun on(filter: ITaskFilter, func: FilterProxy.() -> Unit) {
        func(FilterProxy(filter, this))
    }

    fun on(filterFuc: YashaTask.() -> Boolean, func: FilterProxy.() -> Unit) {
        on(TaskFilter(filterFuc), func)
    }

    override lateinit var yashaConfig: YashaConfig
    override lateinit var yasha: Yasha

    internal data class ResponseEventMethod(val filter: ITaskFilter, val func: (FetchResult) -> Unit)
    internal data class RequestEventMethod(val filter: ITaskFilter, val func: (YashaTask) -> Unit)
    internal data class CheckCacheMethod(val filter: ITaskFilter, val func: (YashaTask) -> FakeResponse?)
    internal data class CheckResponseMethod(val filter: ITaskFilter, val func: (FetchResult) -> Boolean)
    internal data class CreateHttpClientEventMethod(val filter: ITaskFilter, val func: (YashaTask) -> OkHttpClient)
    internal data class TaskFinderEventMethod(val filter: ITaskFilter, val func: (FetchResult) -> List<YashaTask>)
    internal data class ErrorEventMethod(val filter: ITaskFilter, val func: (YashaTask, Throwable) -> Unit)

    internal val onResponseFuncList = ArrayList<ResponseEventMethod>()
    internal val onRequestFuncList = ArrayList<RequestEventMethod>()
    internal val onCreateHttpClientFuncList = ArrayList<CreateHttpClientEventMethod>()
    internal val onCheckResponseMethodFuncList = ArrayList<CheckResponseMethod>()
    internal val onCheckCacheMethodFuncList = ArrayList<CheckCacheMethod>()
    internal val onTaskFinderEventMethodFuncList = ArrayList<TaskFinderEventMethod>()
    internal val onErrorEventMethodFuncList = ArrayList<ErrorEventMethod>()

    override fun onResponse(fetchResult: FetchResult) {
        for (method in onResponseFuncList) {
            if (method.filter.filter(fetchResult.task)) {
                method.func(fetchResult)
                return
            }
        }
    }

    override fun onRequest(yashaTask: YashaTask) {
        for (method in onRequestFuncList) {
            if (method.filter.filter(yashaTask)) {
                method.func(yashaTask)
                return
            }
        }
    }

    private val defaultOkHttpClient = OkHttpClient().newBuilder().build()

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
        return DocumentFinder.findUrl(fetchResult, yashaConfig.blackListRegexList, yashaConfig.filterRegexList)
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