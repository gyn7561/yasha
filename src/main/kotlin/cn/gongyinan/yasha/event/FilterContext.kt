package cn.gongyinan.yasha.event

import cn.gongyinan.yasha.modals.FakeResponse
import cn.gongyinan.yasha.modals.FetchResult
import cn.gongyinan.yasha.task.YashaTask
import cn.gongyinan.yasha.task.filter.*
import okhttp3.OkHttpClient


open class FilterContext(val filter: ITaskFilter) {

    internal lateinit var listener: YashaEventListener

    internal fun listener(listener: YashaEventListener): FilterContext {
        this.listener = listener
        return this
    }

    fun onResponse(func: FetchResult.(FetchResult) -> Unit) {
        listener.onResponseFuncList.add(YashaEventListener.ResponseEventMethod(filter) { fetchResult ->
            func.invoke(fetchResult, fetchResult)
        })
    }

    fun onCheckCache(func: YashaTask.(YashaTask) -> FakeResponse?) {
        listener.onCheckCacheMethodFuncList.add(YashaEventListener.CheckCacheMethod(filter) { task ->
            func(task, task)
        })
    }

    fun onRequest(func: YashaTask.(YashaTask) -> Unit) {
        listener.onRequestFuncList.add(YashaEventListener.RequestEventMethod(filter) { task ->
            func(task, task)
        })
    }

    fun onCheckResponse(func: FetchResult.(FetchResult) -> Boolean) {
        listener.onCheckResponseMethodFuncList.add(YashaEventListener.CheckResponseMethod(filter) { arg ->
            func(arg, arg)
        })
    }

    fun onCreateHttpClient(func: YashaTask.(YashaTask) -> OkHttpClient) {
        listener.onCreateHttpClientFuncList.add(YashaEventListener.CreateHttpClientEventMethod(filter) { arg ->
            func(arg, arg)
        })
    }

    fun onTaskFinder(func: FetchResult.(FetchResult) -> List<YashaTask>) {
        listener.onTaskFinderEventMethodFuncList.add(YashaEventListener.TaskFinderEventMethod(filter) { arg ->
            func(arg, arg)
        })
    }

    fun beforeTaskFinder(func: FetchResult.(FetchResult) -> Unit) {
        listener.beforeTaskFinderEventMethodFuncList.add(YashaEventListener.BeforeTaskFinderEventMethod(filter) { arg ->
            func(arg, arg)
        })
    }


    fun afterTaskFinder(func: FetchResult.(FetchResult, List<YashaTask>) -> List<YashaTask>) {
        listener.afterTaskFinderEventMethodFuncList.add(YashaEventListener.AfterTaskFinderEventMethod(filter) { arg, tasks ->
            func(arg, arg, tasks)
        })
    }

    fun onError(func: YashaTask.(YashaTask, Throwable) -> Unit) {
        listener.onErrorEventMethodFuncList.add(YashaEventListener.ErrorEventMethod(filter) { yashaTask, e ->
            func(yashaTask, yashaTask, e)
        })
    }

    fun on(filter: ITaskFilter, func: FilterContext.() -> Unit) {
        if (this.filter is EmptyTaskFilter) {
            func(FilterContext(filter).listener(listener))
        } else {
            func(FilterContext(CompositeTaskFilter(arrayOf(filter, this.filter))).listener(listener))
        }
    }

    fun on(filterFunc: (YashaTask.() -> Boolean), func: FilterContext.() -> Unit) {
        val filter = TaskFilter(filterFunc)
        if (this.filter is EmptyTaskFilter) {
            func(FilterContext(filter).listener(listener))
        } else {
            func(FilterContext(CompositeTaskFilter(arrayOf(filter, this.filter))).listener(listener))
        }
    }

    fun onRest(func: FilterContext.() -> Unit) {
        on(TaskFilter { true }, func)
    }

    fun onUrlStartsWith(prefix: String, func: FilterContext.() -> Unit) {
        on(TaskFilter { uri.toString().startsWith(prefix) }, func)
    }

    fun onUrlContains(str: String, func: FilterContext.() -> Unit) {
        on(TaskFilter { uri.toString().contains(str) }, func)
    }


    fun onUrlEndsWith(prefix: String, func: FilterContext.() -> Unit) {
        on(TaskFilter { uri.toString().endsWith(prefix) }, func)
    }

    fun onRegex(regex: Regex, func: FilterContext.() -> Unit) {
        on(RegexTaskFilter(regex), func)
    }

}
