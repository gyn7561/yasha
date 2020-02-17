package cn.gongyinan.yasha

import cn.gongyinan.yasha.event.KotlinStyleListener
import cn.gongyinan.yasha.task.db.MongoDbTaskDb
import cn.gongyinan.yasha.task.db.SimpleFileTaskDb
import cn.gongyinan.yasha.task.filter.RegexTaskFilter
import com.mongodb.MongoClient
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Test
import java.io.File

object MongoDbTest {

    @Test
    fun a() {
        val mongoClient = MongoClient("127.0.0.1")

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

        val db = MongoDbTaskDb(mongoClient.getDatabase("YASHA-TEST"), "GAMERSKY")
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
        yasha.waitFinish()
    }
}