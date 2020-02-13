package cn.gongyinan.yasha.event

import cn.gongyinan.yasha.FakeResponse
import cn.gongyinan.yasha.FetchResult
import cn.gongyinan.yasha.task.YashaTask
import okhttp3.OkHttpClient


abstract class AbstractYashaEventListener : SimpleYashaEventListener() {

    override fun onResponse(fetchResult: FetchResult) {
        for (responseEventMethod in onResponseFuncList) {
            if (responseEventMethod.regex.matches(fetchResult.task.uri.toString())) {
                responseEventMethod.func(fetchResult)
                return
            }
        }
    }

    override fun onRequest(yashaTask: YashaTask) {

        val headers = HashMap(yashaTask.headers ?: mapOf())
        headers.putAll(yashaConfig.defaultHeaders)
        yashaTask.headers = headers
        for (requestEventMethod in onRequestFuncList) {
            if (requestEventMethod.regex.matches(yashaTask.uri.toString())) {
                requestEventMethod.func(yashaTask)
                return
            }
        }
    }

    override fun onCreateHttpClient(yashaTask: YashaTask): OkHttpClient {
        for (createHttpClientMethod in onCreateHttpClientFuncList) {
            if (createHttpClientMethod.regex.matches(yashaTask.uri.toString())) {
                return createHttpClientMethod.func(yashaTask)
            }
        }
        return super.onCreateHttpClient(yashaTask)
    }

    override fun onCheckResponse(fetchResult: FetchResult): Boolean {
        for (method in onCheckResponseMethodFuncList) {
            if (method.regex.matches(fetchResult.task.uri.toString())) {
                return method.func(fetchResult)
            }
        }
        return super.onCheckResponse(fetchResult)
    }

    override fun onTaskFinder(fetchResult: FetchResult): List<YashaTask> {
        for (method in onTaskFinderEventMethodFuncList) {
            if (method.regex.matches(fetchResult.task.uri.toString())) {
                return method.func(fetchResult)
            }
        }
        return super.onTaskFinder(fetchResult)
    }

    override fun onError(yashaTask: YashaTask, e: Throwable) {
        for (method in onErrorEventMethodFuncList) {
            if (method.regex.matches(yashaTask.uri.toString())) {
                return method.func(yashaTask, e)
            }
        }
    }

    override fun onCheckCache(yashaTask: YashaTask): FakeResponse? {
        for (method in onCheckCacheMethodFuncList) {
            if (method.regex.matches(yashaTask.uri.toString())) {
                return method.func(yashaTask)
            }
        }
        return null
    }

    internal data class ResponseEventMethod(val regex: Regex, val func: (FetchResult) -> Unit)
    internal data class RequestEventMethod(val regex: Regex, val func: (YashaTask) -> Unit)
    internal data class CheckCacheMethod(val regex: Regex, val func: (YashaTask) -> FakeResponse?)
    internal data class CheckResponseMethod(val regex: Regex, val func: (FetchResult) -> Boolean)
    internal data class CreateHttpClientEventMethod(val regex: Regex, val func: (YashaTask) -> OkHttpClient)
    internal data class TaskFinderEventMethod(val regex: Regex, val func: (FetchResult) -> List<YashaTask>)
    internal data class ErrorEventMethod(val regex: Regex, val func: (YashaTask, Throwable) -> Unit)

    internal val onResponseFuncList = ArrayList<ResponseEventMethod>()
    internal val onRequestFuncList = ArrayList<RequestEventMethod>()
    internal val onCreateHttpClientFuncList = ArrayList<CreateHttpClientEventMethod>()
    internal val onCheckResponseMethodFuncList = ArrayList<CheckResponseMethod>()
    internal val onCheckCacheMethodFuncList = ArrayList<CheckCacheMethod>()
    internal val onTaskFinderEventMethodFuncList = ArrayList<TaskFinderEventMethod>()
    internal val onErrorEventMethodFuncList = ArrayList<ErrorEventMethod>()

    fun getRegexList(): List<Regex> {
        val set = HashSet<Regex>()
        set.addAll(onResponseFuncList.map { f -> f.regex })
        set.addAll(onRequestFuncList.map { f -> f.regex })
        set.addAll(onCreateHttpClientFuncList.map { f -> f.regex })
        set.addAll(onCheckResponseMethodFuncList.map { f -> f.regex })
        set.addAll(onErrorEventMethodFuncList.map { f -> f.regex })
        set.addAll(onTaskFinderEventMethodFuncList.map { f -> f.regex })
        set.addAll(onCheckCacheMethodFuncList.map { f -> f.regex })
        return set.filter { regex -> regex.toString() != "[\\w\\W]*" }
    }

    init {
        for (method in this.javaClass.methods) {
            if (method.isAnnotationPresent(OnResponse::class.java)) {
                if (method.parameters.size == 1 &&
                        method.parameters[0].type == FetchResult::class.java) {
                    val regexStringArray = method.getAnnotation(OnResponse::class.java).value
                    onResponseFuncList.addAll(regexStringArray.map { regexString ->
                        ResponseEventMethod(
                                Regex(
                                        regexString
                                )
                        ) { fetchResult ->
                            method.invoke(this, fetchResult)
                        }
                    })
                } else {
                    throw RuntimeException("OnResponse $method not valid")
                }
            }

            if (method.isAnnotationPresent(OnRequest::class.java)) {
                if (method.parameters.size == 1 &&
                        method.parameters[0].type == YashaTask::class.java) {
                    val regexStringArray = method.getAnnotation(OnRequest::class.java).value

                    onRequestFuncList.addAll(regexStringArray.map { regexString ->
                        RequestEventMethod(
                                Regex(
                                        regexString
                                )
                        ) { yashaTask ->
                            method.invoke(this, yashaTask)
                        }
                    })

                } else {
                    throw RuntimeException("OnRequest $method not valid")
                }
            }

            if (method.isAnnotationPresent(OnCreateHttpClient::class.java)) {
                if (method.parameters.size == 1 &&
                        method.parameters[0].type == YashaTask::class.java &&
                        method.returnType == OkHttpClient::class.java) {
                    val regexStringArray = method.getAnnotation(OnCreateHttpClient::class.java).value
                    onCreateHttpClientFuncList.addAll(regexStringArray.map { regexString ->
                        CreateHttpClientEventMethod(
                                Regex(regexString)
                        ) { yashaTask ->
                            method.invoke(this, yashaTask) as OkHttpClient
                        }
                    })
                } else {
                    throw RuntimeException("OnCreateHttpClient $method not valid")
                }
            }


            if (method.isAnnotationPresent(OnCheckResponse::class.java)) {
                if (method.parameters.size == 1 &&
                        method.parameters[0].type == FetchResult::class.java &&
                        method.returnType == Boolean::class.java) {
                    val regexStringArray = method.getAnnotation(OnCreateHttpClient::class.java).value

                    onCheckResponseMethodFuncList.addAll(regexStringArray.map { regexString ->
                        CheckResponseMethod(
                                Regex(
                                        regexString
                                )
                        ) { fetchResult ->
                            method.invoke(this, fetchResult) as Boolean
                        }
                    })

                } else {
                    throw RuntimeException("OnCheckResponse $method not valid")
                }
            }


            if (method.isAnnotationPresent(OnTaskFinder::class.java)) {
                if (method.parameters.size == 1 &&
                        method.parameters[0].type == FetchResult::class.java &&
                        List::class.java.isAssignableFrom(method.returnType)) {
                    val regexStringArray = method.getAnnotation(OnTaskFinder::class.java).value

                    onTaskFinderEventMethodFuncList.addAll(regexStringArray.map { regexString ->
                        TaskFinderEventMethod(
                                Regex(regexString)
                        ) { fetchResult ->
                            method.invoke(this, fetchResult) as List<YashaTask>
                        }
                    })

                } else {
                    throw RuntimeException("OnTaskFinder $method not valid")
                }
            }

            if (method.isAnnotationPresent(OnCheckCache::class.java)) {
                if (method.parameters.size == 1 &&
                        method.parameters[0].type == YashaTask::class.java &&
                        FakeResponse::class.java == method.returnType) {
                    val regexStringArray = method.getAnnotation(OnTaskFinder::class.java).value

                    onCheckCacheMethodFuncList.addAll(regexStringArray.map { regexString ->
                        CheckCacheMethod(
                                Regex(regexString)
                        ) { yashaTask ->
                            method.invoke(this, yashaTask) as FakeResponse?
                        }
                    })
                } else {
                    throw RuntimeException("OnTaskFinder $method not valid")
                }
            }

            if (method.isAnnotationPresent(OnError::class.java)) {
                if (method.parameters.size == 2 &&
                        method.parameters[0].type == YashaTask::class.java &&
                        method.parameters[1].type == Throwable::class.java) {
                    val regexStringArray = method.getAnnotation(OnError::class.java).value

                    onErrorEventMethodFuncList.addAll(regexStringArray.map { regexString ->
                        ErrorEventMethod(
                                Regex(
                                        regexString
                                )
                        ) { yashaTask, e ->
                            method.invoke(this, yashaTask, e)
                        }
                    })

                } else {
                    throw RuntimeException("OnError $method not valid")
                }
            }

        }
    }

}