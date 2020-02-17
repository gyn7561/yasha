package cn.gongyinan.yasha

import cn.gongyinan.yasha.task.db.SimpleFileTaskDb
import cn.gongyinan.yasha.event.AbstractYashaEventListener
import cn.gongyinan.yasha.event.KotlinStyleListener
import cn.gongyinan.yasha.event.OnResponse
import cn.gongyinan.yasha.modals.FetchResult
import cn.gongyinan.yasha.task.db.SimpleJsonFileTaskDb
import cn.gongyinan.yasha.task.filter.RegexTaskFilter
import org.apache.commons.io.FileUtils

import org.junit.jupiter.api.Test
import java.io.File

object GamerSkyTest {

    class Listener() : AbstractYashaEventListener() {

        @OnResponse(["https://www.gamersky.com/news/\\d*/\\d*.shtml"])
        fun onNewsResponse(yashaConfig: YashaConfig, fetchResult: FetchResult) {
            println("on news")
            println(fetchResult)
        }

    }

    @Test
    fun main() {
        val listener = KotlinStyleListener() {
            onRequest { task ->
                println(task)
            }

            onResponse { result ->
                println(result.bodyString)
            }

        }
        val db = SimpleJsonFileTaskDb("爬虫数据/投资者关系")
        var initUrls = mutableListOf<String>()
        for (index in 1..287) {
            initUrls.add("http://irm.cninfo.com.cn/ircs/search/searchResult?stockCodes=&keywords=&infoTypes=4&startDate=2019-07-01+00%3A00%3A00&endDate=2019-12-31+23%3A59%3A59&onlyAttentionCompany=2&pageNum=$index&pageSize=10")
        }
        val yasha = Yasha(
                YashaConfig(
                        1,
                        2000,
                        listener,
                        initUrls.toTypedArray(),
                        taskDb = db
                )
        )
        yasha.start()
        db.writeData()
        yasha.waitFinish()
        println("hello word")
    }

    @Test
    fun testJavaStyle() {
        val listener = Listener()
        val db = SimpleFileTaskDb("爬虫数据/GAMERSKY")
        val yasha = Yasha(
                YashaConfig(
                        1, 2000, listener, arrayOf("https://www.gamersky.com/"),
                        taskFilterList = arrayOf(
                                Regex("https://www.gamersky.com/"),
                                Regex("https://www.gamersky.com/news/\\d*/\\d*.shtml")
                        ).map { regex -> RegexTaskFilter(regex) }.toTypedArray(),
                        taskDb = db,
                        maxDepth = 2
                )
        )
        yasha.start()
        db.writeData()
        yasha.waitFinish()
    }


    @Test
    fun testKotlinStyle() {
        val listener = KotlinStyleListener() {


            onRegex(Regex("https://www.gamersky.com/news/\\d*/\\d*.shtml")) {
                onRequest { task ->
                    println(task)
                }

                onResponse { result ->
                    FileUtils.writeStringToFile(File(result.responseUri.path.split("/").last()), result.bodyString, "utf-8")
                    println(result.document.select("h1").text())
                }
            }

            onRegex(Regex("https://www.gamersky.com/news/\\d*/\\d*.shtml")) {
                onRequest { task ->
                    println(task)
                }

                onResponse { result ->
                    FileUtils.writeStringToFile(File(result.responseUri.path.split("/").last()), result.bodyString, "utf-8")
                    println(result.document.select("h1").text())
                }
            }

        }
        val db = SimpleJsonFileTaskDb("爬虫数据/GAMERSKY")
        val yasha = Yasha(
                YashaConfig(
                        1, 2000, listener, arrayOf("https://www.gamersky.com/"),
                        taskFilterList = arrayOf(
                                Regex("https://www.gamersky.com/"),
                                Regex("https://www.gamersky.com/news/\\d*/\\d*.shtml")
                        ).map { regex -> RegexTaskFilter(regex) }.toTypedArray(),
                        maxDepth = 10,
                        taskDb = db
                )
        )
        yasha.start()
        db.writeData()
        yasha.waitFinish()
    }


}