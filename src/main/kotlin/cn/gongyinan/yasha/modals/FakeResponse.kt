package cn.gongyinan.yasha.modals

import java.net.URI

class FakeResponse(val uri: URI, val code: Int, val headers: List<Pair<String, String>>, val body: ByteArray, val statusMessage: String = "") {
    fun header(key: String): String? {
        return headers.firstOrNull { pair -> pair.first.equals(key, true) }?.second
    }
}
