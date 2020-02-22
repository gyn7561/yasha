package cn.gongyinan.yasha.devtoolsprotocol.domains

import cn.gongyinan.yasha.devtoolsprotocol.ChromeTab
import com.alibaba.fastjson.JSONObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

typealias ScriptIdentifier = String
typealias TransitionType = String
typealias FrameId = String


class Page(private val chromeTab: ChromeTab) {

    data class AddScriptToEvaluateOnNewDocumentResult(val identifier: ScriptIdentifier)

    suspend fun addScriptToEvaluateOnLoad(scriptSource: String, worldName: String? = null): JSONObject {
        return chromeTab.sendCommand(mapOf("scriptSource" to scriptSource, "worldName" to worldName), "Page.addScriptToEvaluateOnLoad")
    }

    suspend fun addScriptToEvaluateOnNewDocument(source: String, worldName: String? = null): JSONObject {
        return chromeTab.sendCommand(mapOf("source" to source, "worldName" to worldName), "Page.addScriptToEvaluateOnNewDocument")
    }

    suspend fun reload(ignoreCache: Boolean? = null, scriptToEvaluateOnLoad: String? = null) {
        chromeTab.sendCommand(mapOf("ignoreCache" to ignoreCache, "scriptToEvaluateOnLoad" to scriptToEvaluateOnLoad), "Page.reload")
    }

    suspend fun enable() {
        chromeTab.sendCommand(mapOf<String, String>(), "Page.enable")
    }

    suspend fun navigate(url: String, referrer: String? = null, transitionType: TransitionType? = null, frameId: FrameId? = null) {
        chromeTab.sendCommand(mapOf("url" to url, "referrer" to referrer, "transitionType" to transitionType, "frameId" to frameId), "Page.navigate")
    }


//
//    suspend fun getCookies(url: String, referrer: String? = null, transitionType: TransitionType? = null, frameId: FrameId? = null) {
//        chromeTab.sendCommand(mapOf("url" to url, "referrer" to referrer, "transitionType" to transitionType, "frameId" to frameId), "Page.getCookies")
//    }


}