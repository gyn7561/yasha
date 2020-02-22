package cn.gongyinan.yasha.devtoolsprotocol

import com.alibaba.fastjson.JSON
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.tls.OkHostnameVerifier
import java.io.IOException

import java.net.InetAddress
import java.net.Socket


class ChromeDriver(private val chromePath: String, private val commandLine: String = "") {
    private fun isPortUsing(host: String?, port: Int): Boolean {
        var flag = false
        val theAddress = InetAddress.getByName(host)
        try {
            val socket = Socket(theAddress, port)
            flag = true
        } catch (e: IOException) {
        }
        return flag
    }

    fun getPageIdList(): ArrayList<String> {
        val jsonArray = JSON.parseArray(okHttpClient.newCall(Request.Builder().url("http://localhost:$port/json/list").build()).execute().body!!.string())
        val allId = ArrayList<String>()
        for (i in jsonArray.indices) {
            if (jsonArray.getJSONObject(i).getString("type") == "page") {
                allId.add(jsonArray.getJSONObject(i).getString("id"))
            }
        }
        return allId
    }

    private val tabMap = HashMap<String, ChromeTab>()

    fun getTab(id: String): ChromeTab {
        return if (tabMap.containsKey(id)) {
            tabMap[id]!!
        } else {
            tabMap[id] = ChromeTab("ws://localhost:$port/devtools/page/$id")
            tabMap[id]!!
        }
    }

    lateinit var process: Process
    var port: Int = 0
        private set

    private val okHttpClient = OkHttpClient()

    fun start() {
        while (true) {
            val randomPort = (30000..50000).random()
            if (!isPortUsing("localhost", randomPort)) {
                port = randomPort
                break
            }
        }
        process = Runtime.getRuntime().exec("$chromePath --user-data-dir=${"O:\\temp\\${(0..10000).random()}"} --remote-debugging-port=$port $commandLine")
        println("port:$port")
        println(okHttpClient.newCall(Request.Builder().url("http://localhost:$port/json/version").build()).execute().body!!.string())
    }

    fun kill() {
        if (process.isAlive) {
            process.destroy()
        }
    }
}

fun main(args: Array<String>) {
    val d = ChromeDriver("C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe")
    d.start()
    println(d.getPageIdList())
    val tab = d.getTab(d.getPageIdList().first())
    GlobalScope.launch {
        delay(100)
        val sc = "Object.defineProperty(navigator, 'webdriver', { get: () => false, });\n" +
                "\n" +
                "\n" +
                "window.chrome.webstore = {\n" +
                "    fake: true\n" +
                "};" +
                "console.log('write')"
        tab.page.addScriptToEvaluateOnNewDocument(sc)
        println(tab.page.navigate("https://m.dianping.com/"))
        delay(1000)
        println(tab.network.getCookies())
    }

}