package cn.gongyinan.yasha.core

import cn.gongyinan.yasha.FetchResult
import cn.gongyinan.yasha.ResponseType
import cn.gongyinan.yasha.YashaConfig
import cn.gongyinan.yasha.YashaTask
import cn.gongyinan.yasha.utils.EncodingDetect
import cn.gongyinan.yasha.utils.GZipUtil
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.lang.RuntimeException

import java.nio.charset.Charset

open class Engine(private val yashaConfig: YashaConfig, private val name: String) {

    override fun toString(): String {
        return name
    }

    private val logger = LogManager.getLogger(Engine::class.java)

    var running: Boolean = false
        private set

    var runningTask: YashaTask? = null
        private set

    var lastRunTimestamp: Long = 0L
        private set

    open fun fetchPage(
        task: YashaTask,
        onSuccess: (FetchResult) -> Unit,
        onFailure: (YashaTask, Throwable) -> Unit,
        onFinal: (YashaTask) -> Unit,
        retryCount: Int = 0
    ) {
        runningTask = task
        val requestBuilder = Request.Builder().url(task.uri.toString())
        running = true
        yashaConfig.listener.onRequest(task)
        task.headers?.forEach { (k, v) ->
            requestBuilder.header(k, v)
        }
        requestBuilder.method(
            task.method,
            if (task.requestBody == null) null else task.requestBody!!.toRequestBody(null)
        )
        val okHttpClient = yashaConfig.listener.onCreateHttpClient(task)


        val start = System.currentTimeMillis()
        okHttpClient.newCall(requestBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (retryCount + 1 <= yashaConfig.retryCount) {
                    logger.error("$this 运行 $task 出错 正在重试 ${e.message}")
                    fetchPage(task, onSuccess, onFailure, onFinal, retryCount + 1)
                } else {
                    logger.error("$this 运行 $task 出错 ${e.message}")
                    onFailure(task, e)
                    innerOnFinal(null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    realOnResponse(call, response)
                } catch (e: Throwable) {
                    if (retryCount + 1 <= yashaConfig.retryCount) {
                        logger.error("$this onResponse 出错 ", e)
                        fetchPage(task, onSuccess, onFailure, onFinal, retryCount + 1)
                    } else {
                        logger.error("$this onResponse 出错 ", e)
                        onFailure(task, e)
                        innerOnFinal(response)
                    }
                }
            }

            private fun innerOnFinal(response: Response?) {
                lastRunTimestamp = System.currentTimeMillis()
                running = false
                logger.info("${this@Engine} 耗时${System.currentTimeMillis() - start}ms 完成$task")
                onFinal(task)
                runningTask = null
                response?.body?.close()
                response?.close()
            }

            private fun realOnResponse(call: Call, response: Response) {
                val responseUri = response.request.url.toUri()
                var rawData = response.body!!.bytes()
                var gzipData: ByteArray? = null
                if (response.header("content-encoding") == "gzip") {
                    if (GZipUtil.isGzipData(rawData)) {
                        gzipData = rawData
                        rawData = GZipUtil.uncompress(gzipData)
                    }
                }

                val contentType = response.header("content-type") ?: ""

                val responseType = when {
                    contentType.startsWith("text/html") -> {
                        ResponseType.Html
                    }
                    contentType.startsWith("application/json") -> {
                        ResponseType.Json
                    }
                    contentType.startsWith("text/xml") -> {
                        ResponseType.XML
                    }
                    contentType.startsWith("application/x-javascript") -> {
                        ResponseType.JavaScript
                    }
                    contentType.startsWith("text/plain") -> {
                        ResponseType.Text
                    }
                    else -> {
                        ResponseType.Binary
                    }
                }
                var bodyString: String? = null
                if (responseType != ResponseType.Binary) {
                    val encode = EncodingDetect.detect(rawData)
                    bodyString = String(rawData, Charset.forName(encode))
                }
                val fetchResult = FetchResult(
                    task,
                    response,
                    responseUri,
                    arrayListOf(),
                    responseType,
                    contentType,
                    rawData,
                    gzipData,
                    bodyString
                )
                val valid = yashaConfig.listener.onCheckResponse(fetchResult)
                if (valid) {
                    fetchResult.subTasks = yashaConfig.listener.onTaskFinder(fetchResult)
                    onSuccess(fetchResult)
                    innerOnFinal(response)
                } else {
                    if (retryCount + 1 <= yashaConfig.retryCount) {
                        logger.error("$this 检查 $task 不合法 正在重试 ")
                        fetchPage(task, onSuccess, onFailure, onFinal, retryCount + 1)
                    } else {
                        logger.error("$this 检查 $task 不合法 ${fetchResult.bodyString}")
                        onFailure(task, RuntimeException("检查 $task 不合法"))
                        innerOnFinal(response)
                    }
                }
            }


        })
    }

}