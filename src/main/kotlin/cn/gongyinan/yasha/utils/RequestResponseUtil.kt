package cn.gongyinan.yasha.utils

import java.net.URI

fun URI.parseQuery(): HashMap<String, String> {
    val query = this.query
    val result = HashMap<String, String>()
    if (query == null) {
        return result
    }

    for (s in query.split("&")) {
        if (s.any()) {
            if (s.contains("=")) {
                val sp = s.split("=")
                result[sp[0]] = sp[1]
            } else {
                result[s] = ""
            }
        }
    }
    return result
}

fun String.parseFormData() : HashMap<String, String>{
    val query = this
    val result = HashMap<String, String>()
    for (s in query.split("&")) {
        if (s.any()) {
            if (s.contains("=")) {
                val sp = s.split("=")
                result[sp[0]] = sp[1]
            } else {
                result[s] = ""
            }
        }
    }
    return result
}