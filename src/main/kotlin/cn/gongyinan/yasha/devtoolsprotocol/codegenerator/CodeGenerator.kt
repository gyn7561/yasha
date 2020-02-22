package cn.gongyinan.yasha.devtoolsprotocol.codegenerator

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.Request
import java.lang.StringBuilder
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

internal object CodeGenerator {
    private const val dataJsonUrl = "https://chromedevtools.github.io/devtools-protocol/_data/tot/protocol.json"

    private val globalJson by lazy {
        val json = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(100, TimeUnit.SECONDS).proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", 11080))).build().newCall(Request.Builder().url(dataJsonUrl).build()).execute().body!!.string()
        println(json)
        JsonParser().parse(json).asJsonObject
    }

    data class Item(@SerializedName("\$ref") val ref: String?)

    data class Property(val name: String, val type: String?, @SerializedName("\$ref") val ref: String?, val optional: Boolean, val description: String, val items: Item?)
    data class Type(val id: String, val type: String, val enum: Array<String>?, val properties: Array<Property>?, val description: String)

    private val typeMap: HashMap<String, Type> by lazy {
        val result = HashMap<String, Type>()
        val domains = globalJson.getAsJsonArray("domains")
        for (i in 0 until domains.size()) {
            val domain = domains.get(i).asJsonObject
            val domainName = domain.get("domain").asString
            val types = domain.get("types")?.asJsonArray
            println(types)
            println(Gson().toJson(types))
            val typeList = Gson().fromJson<Array<Type>>(Gson().toJson(types), Array<Type>::class.java)
            typeList?.forEach { type ->
                result["$domainName.${type.id}"] = type
                result["${type.id}"] = type
            }
        }
        result
    }

    private const val basePackage = "cn.gongyinan.yasha.devtoolsprotocol.domains"


    private fun getAllDomains(): List<String> {
        val domains = globalJson.get("domains").asJsonArray
        val result = ArrayList<String>()
        for (i in 0 until domains.size()) {
            result.add(domains.get(i).asJsonObject.get("domain").asString)
        }
        return result
    }

    private fun output(dir: String) {
        val domains = globalJson.get("domains").asJsonArray
        val allDomains = getAllDomains()

        for (i in 0 until domains.size()) {
            val sb = StringBuilder()
            val domain = domains.get(i).asJsonObject
            val domainName = domain.get("domain").asString
            val packageName = "$basePackage.$domainName"

            sb.appendln("package $packageName")

            val domainExperimental = domain.get("experimental")?.asBoolean ?: false
            val domainDependencies = domain.get("dependencies")?.asJsonArray
            val domainCommands = domain.get("commands").asJsonArray
            for (j in 0 until domainCommands.size()) {
                parseCommand(domainCommands.get(j).asJsonObject, sb)
            }
            println(sb.toString())
        }
    }


    fun parseCommand(json: JsonObject, stringBuilder: StringBuilder) {
        val name = json.get("name").asString
        val parameters = json.getAsJsonArray("parameters")

        val parameterList = Gson().fromJson<Array<Property>>(Gson().toJson(parameters), Array<Property>::class.java)
                ?: arrayOf()


        val returns = Gson().fromJson<Array<Type>>(Gson().toJson(json.getAsJsonArray("returns")), Array<Type>::class.java)
                ?: arrayOf()

        stringBuilder.appendln("fun $name(${parameterList.map { p ->
            "${p.name} : ${p.type ?: p.ref}"
        }.joinToString(",")}){}")
    }

    @JvmStatic
    fun main(args: Array<String>) {

        typeMap.forEach { k, v ->
            println(v)
        }
        output("output")
    }

}