package cn.gongyinan.yasha.devtoolsprotocol.domains

import cn.gongyinan.yasha.devtoolsprotocol.ChromeTab
import com.alibaba.fastjson.JSONObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class Network(private val chromeTab: ChromeTab) {

    data class Cookie(var name: String, var value: String, var domain: String, var path: String, var expires: Double, var size: Int, var httpOnly: Boolean, var secure: Boolean, var session: Boolean)

    suspend fun getCookies(urls: Array<String>? = null): Array<Cookie> {
        return chromeTab.sendCommand<Array<Cookie>>(Array<Cookie>::class.java, "cookies", mapOf("urls" to urls), "Network.getCookies")
    }

}