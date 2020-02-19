package cn.gongyinan.yasha.event

import cn.gongyinan.yasha.Yasha
import cn.gongyinan.yasha.YashaConfig
import cn.gongyinan.yasha.finder.DocumentFinder
import cn.gongyinan.yasha.modals.FakeResponse
import cn.gongyinan.yasha.modals.FetchResult
import cn.gongyinan.yasha.task.YashaTask
import cn.gongyinan.yasha.task.filter.ITaskFilter
import cn.gongyinan.yasha.task.filter.TaskFilter
import okhttp3.OkHttpClient


class YashaEventListener(func: (YashaEventListener.() -> Unit)?) : IYashaEventListener {

    internal val onResponseFuncList = ArrayList<ResponseEventMethod>()
    internal val onRequestFuncList = ArrayList<RequestEventMethod>()
    internal val onCreateHttpClientFuncList = ArrayList<CreateHttpClientEventMethod>()
    internal val onCheckResponseMethodFuncList = ArrayList<CheckResponseMethod>()
    internal val onCheckCacheMethodFuncList = ArrayList<CheckCacheMethod>()
    internal val onTaskFinderEventMethodFuncList = ArrayList<TaskFinderEventMethod>()
    internal val onErrorEventMethodFuncList = ArrayList<ErrorEventMethod>()

    init {
        func?.invoke(this)

    }



    class FilterContext(val filter: ITaskFilter, private val listener: YashaEventListener) {

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

    fun on(filter: ITaskFilter, func: FilterContext.() -> Unit) {
        func(FilterContext(filter, this))
    }

    /**
     * 如果要用 一定要写最后面
     */
    fun onRest(filter: ITaskFilter, func: FilterContext.() -> Unit) {
        on(TaskFilter { true }, func)
    }

    fun onUrlStartsWith(prefix: String, func: FilterContext.() -> Unit) {
        on(TaskFilter { uri.toString().startsWith(prefix) }, func)
    }

    fun onUrlEndsWith(prefix: String, func: FilterContext.() -> Unit) {
        on(TaskFilter { uri.toString().endsWith(prefix) }, func)
    }

    fun on(filterFuc: YashaTask.() -> Boolean, func: FilterContext.() -> Unit) {
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