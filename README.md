# yasha
yasha crawler
一个简单的【游民星空】全站新闻爬虫DEMO

```
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

    }
    val db = SimpleFileTaskDb("爬虫数据/GAMERSKY")
    val yasha = Yasha(
            YashaConfig(
                    1, 2000, listener, arrayOf("https://www.gamersky.com/"),
                    filterRegexList = arrayOf(
                            Regex("https://www.gamersky.com/"),
                            Regex("https://www.gamersky.com/news/\\d*/\\d*.shtml")
                    ),
                    maxDepth = 10,
                    taskDb = db
            )
    )
    yasha.start()
    db.writeData()
    yasha.waitFinish()
}
```
