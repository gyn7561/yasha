package cn.gongyinan.yasha.event

import cn.gongyinan.yasha.FakeResponse
import cn.gongyinan.yasha.FetchResult
import cn.gongyinan.yasha.task.YashaTask
import okhttp3.OkHttpClient
@Deprecated("改用YashaEventListener")
open class KotlinStyleListener(func: (KotlinStyleListener.() -> Unit)?) : AbstractYashaEventListener() {

    class RegexProxy(val regex: Regex, private val listener: KotlinStyleListener) {

        fun onResponse(func: FetchResult.(FetchResult) -> Unit) {
            listener.onResponseFuncList.add(ResponseEventMethod(regex) { fetchResult ->
                func.invoke(fetchResult, fetchResult)
            })
        }

        fun onCheckCache(func: YashaTask.(YashaTask) -> FakeResponse?) {
            listener.onCheckCacheMethodFuncList.add(CheckCacheMethod(regex) { task ->
                func(task, task)
            })
        }

        fun onRequest(func: YashaTask.(YashaTask) -> Unit) {
            listener.onRequestFuncList.add(RequestEventMethod(regex) { task ->
                func(task, task)
            })
        }

        fun onCheckResponse(func: FetchResult.(FetchResult) -> Boolean) {
            listener.onCheckResponseMethodFuncList.add(CheckResponseMethod(regex) { fetchResult ->
                func(fetchResult, fetchResult)
            })
        }

        fun onCreateHttpClient(func: YashaTask.(YashaTask) -> OkHttpClient) {
            listener.onCreateHttpClientFuncList.add(CreateHttpClientEventMethod(regex) { yashaTask ->
                func(yashaTask, yashaTask)
            })
        }

        fun onTaskFinder(func: FetchResult.(FetchResult) -> List<YashaTask>) {
            listener.onTaskFinderEventMethodFuncList.add(
                    TaskFinderEventMethod(
                            regex
                    ) { yashaTask ->
                        func(yashaTask, yashaTask)
                    })
        }

        fun onError(func: YashaTask.(YashaTask, Throwable) -> Unit) {
            listener.onErrorEventMethodFuncList.add(
                    ErrorEventMethod(
                            regex
                    ) { yashaTask, e ->
                        func(yashaTask, yashaTask, e)
                    })
        }

    }

    init {
        func?.invoke(this)
    }

    fun onRegex(regex: Regex = Regex("[\\w\\W]*"), func: RegexProxy.() -> Unit) {
        func(RegexProxy(regex, this))
    }

//    fun on(cls: (YashaTask.() -> Boolean), func: RegexProxy.() -> Unit) {
//        func(RegexProxy(Regex(""), this))
//    }

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

    fun onCheckCache(regex: Regex = Regex("[\\w\\W]*"), func: IYashaEventListener.(YashaTask) -> FakeResponse?) {
        onRegex(regex) {
            onCheckCache { task ->
                func(task)
            }
        }
    }

    fun onCheckResponse(regex: Regex = Regex("[\\w\\W]*"), func: IYashaEventListener.(FetchResult) -> Boolean) {
        onRegex(regex) {
            onCheckResponse { arg ->
                func(arg)
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