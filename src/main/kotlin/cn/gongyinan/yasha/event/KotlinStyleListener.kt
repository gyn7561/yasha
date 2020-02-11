package cn.gongyinan.yasha.event

import cn.gongyinan.yasha.FetchResult
import cn.gongyinan.yasha.YashaTask
import okhttp3.OkHttpClient

open class KotlinStyleListener(func: (KotlinStyleListener.() -> Unit)?) : AbstractYashaEventListener() {

    class RegexProxy(private val regex: Regex, private val listener: KotlinStyleListener) {

        fun onResponse(func: RegexProxy.(FetchResult) -> Unit) {
            listener.onResponseFuncList.add(
                ResponseEventMethod(
                    regex
                ) { fetchResult ->
                    func.invoke(this, fetchResult)
                })
        }

        fun onRequest(func: RegexProxy.(YashaTask) -> Unit) {
            listener.onRequestFuncList.add(
                RequestEventMethod(
                    regex
                ) { task ->
                    func(this, task)
                })
        }

        fun onCheckResponse(func: RegexProxy.(FetchResult) -> Boolean) {
            listener.onCheckResponseMethodFuncList.add(
                CheckResponseMethod(
                    regex
                ) { fetchResult ->
                    func(this, fetchResult)
                })
        }

        fun onCreateHttpClient(func: RegexProxy.(YashaTask) -> OkHttpClient) {
            listener.onCreateHttpClientFuncList.add(
                CreateHttpClientEventMethod(
                    regex
                ) { yashaTask ->
                    func(this, yashaTask)
                })
        }

        fun onTaskFinder(func: RegexProxy.(FetchResult) -> List<YashaTask>) {
            listener.onTaskFinderEventMethodFuncList.add(
                TaskFinderEventMethod(
                    regex
                ) { yashaTask ->
                    func(this, yashaTask)
                })
        }

        fun onError(func: RegexProxy.(YashaTask, Throwable) -> Unit) {
            listener.onErrorEventMethodFuncList.add(
                ErrorEventMethod(
                    regex
                ) { yashaTask, e ->
                    func(this, yashaTask, e)
                })
        }

    }

    init {
        func?.invoke(this)
    }

    fun onRegex(regex: Regex = Regex("[\\w\\W]*"), func: RegexProxy.() -> Unit) {
        func(RegexProxy(regex, this))
    }

    fun onResponse(regex: Regex = Regex("[\\w\\W]*"), func: IYashaEventListener.(FetchResult) -> Unit) {
        onRegex(regex) {
            onResponse { fetchResult ->
                func(fetchResult)
            }
        }
    }

    fun onRequest(regex: Regex = Regex("[\\w\\W]*"), func: IYashaEventListener.(YashaTask) -> Unit) {
        onRegex(regex) {
            onRequest { task ->
                func(task)
            }
        }
    }

    fun onCheckResponse(regex: Regex = Regex("[\\w\\W]*"), func: IYashaEventListener.(FetchResult) -> Boolean) {
        onRegex(regex) {
            onCheckResponse { task ->
                func(task)
            }
        }
    }

    fun onCreateHttpClient(regex: Regex = Regex("[\\w\\W]*"), func: IYashaEventListener.(YashaTask) -> OkHttpClient) {
        onRegex(regex) {
            onCreateHttpClient { task ->
                func(task)
            }
        }
    }

    fun onTaskFinder(regex: Regex = Regex("[\\w\\W]*"), func: IYashaEventListener.(FetchResult) -> List<YashaTask>) {
        onRegex(regex) {
            onTaskFinder { result ->
                func(result)
            }
        }
    }

    fun onError(regex: Regex = Regex("[\\w\\W]*"), func: IYashaEventListener.(YashaTask, Throwable) -> Unit) {
        onRegex(regex) {
            onError { task, e ->
                func(task, e)
            }
        }
    }

}