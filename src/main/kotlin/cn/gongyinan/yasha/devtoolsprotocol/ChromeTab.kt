package cn.gongyinan.yasha.devtoolsprotocol

import cn.gongyinan.yasha.devtoolsprotocol.domains.Network
import cn.gongyinan.yasha.devtoolsprotocol.domains.Page
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString

class ChromeTab(val wsUrl: String) {

    private val okHttpClient = OkHttpClient()

    private lateinit var websocket: WebSocket

    val page = Page(this)
    val network = Network(this)


    init {
        val request: Request = Request.Builder().url(wsUrl).build()

        println("connect $wsUrl")
        websocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                println("onMessage$text")
                val jsonObject = JSON.parseObject(text)
                val id = jsonObject.getIntValue("id")
                if (channelMap.containsKey(id)) {
                    val channel = channelMap[id]!!
                    GlobalScope.launch {
                        channel.send(jsonObject)
                        channelMap.remove(id)
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                super.onMessage(webSocket, bytes)
                println(bytes)
            }

            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                println("onOpen $wsUrl")

                //Page.reload
//                webSocket.send("{\"id\":1,\"method\":\"Page.reload\",\"params\":{}}")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                println("onFailure$t")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                println("onClosed")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosing(webSocket, code, reason)
                println("onClosing")
            }
        })
    }

    private var innerId = 0

    private val channelMap = HashMap<Int, Channel<JSONObject>>()

    suspend fun sendCommand(params: Any?, method: String): JSONObject {
        val channel = Channel<JSONObject>()
        val command = synchronized(this) {
            val command = JSON.toJSONString(mapOf("id" to innerId, "method" to method, "params" to params))
            channelMap[innerId] = channel
            innerId++
            command
        }
        println("send command $command")
        websocket.send(command)
        return channel.receive()
    }

    suspend fun <T> sendCommand(returnClass: Class<T>, resultField: String, params: Any?, method: String): T {
        val channel = Channel<JSONObject>()
        val command = synchronized(this) {
            val command = JSON.toJSONString(mapOf("id" to innerId, "method" to method, "params" to params))
            channelMap[innerId] = channel
            innerId++
            command
        }
        println("send command $command")
        websocket.send(command)
        val jsonObj = channel.receive()
        return Gson().fromJson(jsonObj.getJSONObject("result")[resultField].toString(), returnClass)
    }
}

fun main() {
    val tab = ChromeTab("ws://localhost:922/devtools/page/FC5BC578AD36D6491D421EA1DB16BA6F")
    GlobalScope.launch {
        delay(100)
        tab.page.reload()
        delay(6 * 60 * 1000)
        tab.page.reload()
    }
}