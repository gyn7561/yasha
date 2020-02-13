package cn.gongyinan.yasha

import cn.gongyinan.yasha.task.db.SimpleFileTaskDb
import cn.gongyinan.yasha.event.AbstractYashaEventListener
import cn.gongyinan.yasha.event.KotlinStyleListener
import cn.gongyinan.yasha.event.OnResponse

import org.junit.jupiter.api.Test

object GamerSkyTest {

    class Listener() : AbstractYashaEventListener() {

        @OnResponse(["https://www.gamersky.com/news/\\d*/\\d*.shtml"])
        fun onNewsResponse(yashaConfig: YashaConfig, fetchResult: FetchResult) {
            println("on news")
            println(fetchResult)
        }

    }

    @Test
    fun testJavaStyle() {
        val listener = Listener()
        val db = SimpleFileTaskDb("爬虫数据/GAMERSKY")
        val yasha = Yasha(
                YashaConfig(
                        1, 2000, listener, arrayOf("https://www.gamersky.com/"),
                        filterRegexList = arrayOf(
                                Regex("https://www.gamersky.com/"),
                                Regex("https://www.gamersky.com/news/\\d*/\\d*.shtml")
                        ),
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
                    println(result.document.select("h1").text())
                }
            }
        }
        val db = SimpleFileTaskDb("爬虫数据/GAMERSKY")
        val yasha = Yasha(
                YashaConfig(
                        1, 2000, listener, arrayOf("https://www.gamersky.com/"),
                        filterRegexList = arrayOf(
                                Regex("https://www.gamersky.com/"),
                                Regex("https://www.gamersky.com/news/\\d*/\\d*.shtml")
                        ),
                        maxDepth = 2,
                        taskDb = db
                )
        )
        yasha.start()
        db.writeData()
        yasha.waitFinish()
    }


}