package cn.gongyinan.yasha.finder

import cn.gongyinan.yasha.FetchResult
import cn.gongyinan.yasha.ResponseType
import org.apache.logging.log4j.LogManager
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI

object DocumentFinder {

    private val logger = LogManager.getLogger(DocumentFinder::class.java)

    fun findUrl(fetchResult: FetchResult, blackListRegexList: Array<Regex>, filterRegexList: Array<Regex>): HashSet<URI> {
        if (fetchResult.responseType == ResponseType.Html) {
            return findUrl(fetchResult.responseUri, fetchResult.document, blackListRegexList, filterRegexList)
        }
        return HashSet<URI>()
    }

    fun findUrl(responseUri: URI, document: Document, blackListRegexList: Array<Regex>, filterRegexList: Array<Regex>): HashSet<URI> {
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
                if (uri.scheme in arrayOf("http", "https") && !blackListRegexList.any { regex -> regex.matches(uri.toString()) } && filterRegexList.any { regex -> regex.matches(uri.toString()) }) {
                    resultSet.add(uri)
                }
            } catch (e: Throwable) {
                logger.debug("解析URL出错 ${responseUri} $href ", e)
            }
        }
        return resultSet
    }
}