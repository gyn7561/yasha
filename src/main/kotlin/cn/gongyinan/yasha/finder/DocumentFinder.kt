package cn.gongyinan.yasha.finder

import cn.gongyinan.yasha.modals.FetchResult
import cn.gongyinan.yasha.modals.ResponseType
import cn.gongyinan.yasha.task.TempTaskFilterTask
import cn.gongyinan.yasha.task.filter.ITaskFilter
import org.apache.logging.log4j.LogManager
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI

object DocumentFinder {

    private val logger = LogManager.getLogger(DocumentFinder::class.java)

    fun findUrl(fetchResult: FetchResult, taskFilterList: Array<ITaskFilter>, taskFilterBlackList: Array<ITaskFilter>): HashSet<URI> {
        if (fetchResult.responseType == ResponseType.Html) {
            return findUrl(fetchResult.responseUri, fetchResult.document, taskFilterList, taskFilterBlackList)
        }
        return HashSet<URI>()
    }

    fun findUrl(responseUri: URI, document: Document, taskFilterList: Array<ITaskFilter>, taskFilterBlackList: Array<ITaskFilter>): HashSet<URI> {
        val resultSet = HashSet<URI>()
        val doc = document
        val hrefSet = doc.select("a").map { dom -> dom.attr("href").trim() }.toSet().filter { href -> href.isNotEmpty() }
        for (href in hrefSet) {
            try {
                val realHref = if (href.startsWith("?")) {
                    responseUri.path.split("/").last() + href
                } else {
                    href
                }
                val addr = responseUri.resolve(realHref).toString().split("#").first()
                val uri = URI(addr)

                if (uri.scheme in arrayOf("http", "https") && taskFilterList.any { filter -> filter.filter(TempTaskFilterTask(uri)) } && !taskFilterBlackList.any { filter -> filter.filter(TempTaskFilterTask(uri)) }) {
                    resultSet.add(uri)
                }
            } catch (e: Throwable) {
                logger.debug("解析URL出错 $responseUri $href ", e)
            }
        }
        return resultSet
    }

}