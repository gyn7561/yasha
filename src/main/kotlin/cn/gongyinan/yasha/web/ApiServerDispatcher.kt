package cn.gongyinan.yasha.web

import cn.gongyinan.yasha.Yasha
import com.alibaba.fastjson.JSON
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest


class ApiServerDispatcher(yasha: Yasha, port: Int, private val name: String = "yasha") : Dispatcher() {

    private val server = MockWebServer()

    init {
        server.dispatcher = this
        server.start(port)
    }

    private fun jsonResponse(obj: Any?): MockResponse {
        return MockResponse()
                .setStatus("HTTP/1.1 200")
                .addHeader("Access-Control-Allow-Origin: *")
                .addHeader("content-type: application/json; charset=utf-8")
                .setBody(JSON.toJSONString(obj))
    }

    private val finishedTaskSize: (RecordedRequest) -> MockResponse = {
        jsonResponse(mapOf("size" to yasha.yashaConfig.taskDb.finishedTaskCount))
    }

    private val unfinishedTaskSize: (RecordedRequest) -> MockResponse = {
        jsonResponse(mapOf("size" to yasha.yashaConfig.taskDb.unfinishedTaskCount))
    }

    private val lastOneMinSpeed: (RecordedRequest) -> MockResponse = {
        jsonResponse(mapOf("speed" to yasha.yashaConfig.taskDb.lastOneMinSpeed))
    }


    private val totalSpeed: (RecordedRequest) -> MockResponse = {
        jsonResponse(mapOf("speed" to yasha.yashaConfig.taskDb.totalSpeed))
    }

    private val runningTask: (RecordedRequest) -> MockResponse = {
        jsonResponse(yasha.runningTask())
    }


    private val info: (RecordedRequest) -> MockResponse = {
        jsonResponse(mapOf("name" to name))
    }

    private val reqMap = mapOf("/gcp/yasha/finished_task_size" to finishedTaskSize,
            "/gcp/yasha/last_one_min_speed" to lastOneMinSpeed,
            "/gcp/yasha/total_speed" to totalSpeed,
            "/gcp/yasha/running_task" to runningTask,
            "/gcp/yasha/info" to info,
            "/gcp/yasha/unfinished_task_size" to unfinishedTaskSize)

    override fun dispatch(request: RecordedRequest): MockResponse {
        if (reqMap.containsKey(request.path)) {
            return reqMap[request.path]!!.invoke(request)
        }
        return jsonResponse(mapOf("status" to 404))
    }

    fun stop() {
        server.shutdown()
    }

}