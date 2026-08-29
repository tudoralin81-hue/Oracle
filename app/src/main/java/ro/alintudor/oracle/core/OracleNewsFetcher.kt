package ro.alintudor.oracle.core

import android.util.Xml
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.xmlpull.v1.XmlPullParser

/**
 * Lightweight RSS/Atom ingestion for the native News module.
 * Failed feeds are ignored so one unavailable publisher never breaks Oracle.
 */
object OracleNewsFetcher {
    private data class Feed(val name: String, val url: String)

    private val feeds = listOf(
        Feed("CNBC", "https://www.cnbc.com/id/100003114/device/rss/rss.html"),
        Feed("MarketWatch", "https://feeds.marketwatch.com/marketwatch/topstories/"),
        Feed("Investing.com", "https://www.investing.com/rss/news_25.rss"),
        Feed("Google News • Markets", "https://news.google.com/rss/search?q=stock%20market%20OR%20stocks%20OR%20markets&hl=en-US&gl=US&ceid=US:en"),
        Feed("Google News • Economy", "https://news.google.com/rss/search?q=economy%20OR%20inflation%20OR%20Federal%20Reserve&hl=en-US&gl=US&ceid=US:en")
    )

    fun fetch(limit: Int = 150): List<OracleNews> {
        val result = ArrayList<OracleNews>()
        for (feed in feeds) {
            runCatching { result += readFeed(feed) }
        }
        return result
            .filter { it.title.isNotBlank() }
            .groupBy { canonicalKey(it) }
            .values.map { group -> group.maxByOrNull { it.publishedAt }!! }
            .sortedWith(compareByDescending<OracleNews> { it.breaking }.thenByDescending { it.publishedAt })
            .take(limit)
    }

    private fun canonicalKey(n: OracleNews): String =
        (n.rawId.ifBlank { n.title.trim().lowercase(Locale.US) }).replace(Regex("\\s+"), " ")

    private fun readFeed(feed: Feed): List<OracleNews> {
        val connection = (URL(feed.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 7000
            readTimeout = 9000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "OracleStockIntelligence/1.0")
            setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
        }
        return try {
            if (connection.responseCode !in 200..299) return emptyList()
            connection.inputStream.use { input -> parse(feed, input) }
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(feed: Feed, input: java.io.InputStream): List<OracleNews> {
        val parser = Xml.newPullParser()
        parser.setInput(input, null)
        val out = ArrayList<OracleNews>()
        var event = parser.eventType
        var inItem = false
        var title = ""
        var link = ""
        var id = ""
        var published = 0L
        var source = feed.name
        var currentTag = ""

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name.lowercase(Locale.US)
                    if (name == "item" || name == "entry") {
                        inItem = true; title = ""; link = ""; id = ""; published = 0L; source = feed.name
                    } else if (inItem) {
                        currentTag = name
                        if (name == "link") {
                            val href = parser.getAttributeValue(null, "href")
                            if (!href.isNullOrBlank()) link = href
                        }
                    }
                }
                XmlPullParser.TEXT -> if (inItem) {
                    val text = parser.text?.trim().orEmpty()
                    when (currentTag) {
                        "title" -> if (title.isBlank()) title = text
                        "link" -> if (link.isBlank()) link = text
                        "guid", "id" -> if (id.isBlank()) id = text
                        "pubdate", "published", "updated", "dc:date" -> if (published == 0L) published = parseDate(text)
                        "source" -> if (text.isNotBlank()) source = text
                    }
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name.lowercase(Locale.US)
                    if (name == "item" || name == "entry") {
                        if (title.isNotBlank()) {
                            val now = System.currentTimeMillis()
                            val ts = if (published > 0) published else now
                            out += OracleNews(
                                ticker = "",
                                title = clean(title),
                                source = source,
                                url = link.trim(),
                                publishedAt = ts,
                                breaking = isBreaking(title),
                                publisher = source,
                                sourceType = "NEWS",
                                receivedAt = now,
                                timezone = "Europe/Bucharest",
                                rawId = id.ifBlank { link.ifBlank { title } },
                                engineVersion = "NEWS-INGEST-1"
                            )
                        }
                        inItem = false; currentTag = ""
                    }
                }
            }
            event = parser.next()
        }
        return out
    }

    private fun clean(value: String) = value.replace(Regex("\\s+"), " ").trim()

    private fun isBreaking(title: String): Boolean {
        val t = title.lowercase(Locale.US)
        return listOf("breaking", "urgent", "flash", "just in", "fed emergency", "market halt").any { t.contains(it) }
    }

    private fun parseDate(value: String): Long = runCatching {
        ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
    }.getOrElse {
        runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(0L)
    }
}
