package ro.alintudor.oracle.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URI
import java.util.concurrent.Executors
import java.util.regex.Pattern

data class OracleKnowledgeArticle(
    val title: String,
    val url: String,
    val excerpt: String,
    val content: String,
    val publishedAt: Long,
    val refreshedAt: Long
)

object OracleKnowledgeSync {
    const val SOURCE_URL = "https://alintudor.ro/knowledge/"
    private const val PREFS = "oracle_knowledge"
    private const val ITEMS = "articles"
    private const val LAST_SUCCESS = "last_success"
    private const val LAST_ERROR = "last_error"
    private const val MAX_ARTICLES = 100
    private const val STALE_MS = 20L * 60L * 60L * 1000L
    private const val REQUEST_TIMEOUT = 12000
    private val executor = Executors.newSingleThreadExecutor()

    fun load(context: Context): List<OracleKnowledgeArticle> = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ITEMS, "[]") ?: "[]"
        val a = JSONArray(raw)
        List(a.length()) { i -> val o = a.getJSONObject(i); OracleKnowledgeArticle(o.optString("title"), o.optString("url"), o.optString("excerpt"), o.optString("content"), o.optLong("publishedAt"), o.optLong("refreshedAt")) }
    }.getOrDefault(emptyList())

    fun lastSuccess(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(LAST_SUCCESS, 0L)
    fun lastError(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(LAST_ERROR, "") ?: ""
    fun isStale(context: Context): Boolean = System.currentTimeMillis() - lastSuccess(context) >= STALE_MS

    fun refreshAsync(context: Context, onDone: (Boolean, String?) -> Unit = { _, _ -> }) {
        val app = context.applicationContext
        executor.execute {
            val result = runCatching { refreshBlocking(app) }
            Handler(Looper.getMainLooper()).post { result.fold({ onDone(true, null) }, { onDone(false, it.message ?: it.javaClass.simpleName) }) }
        }
    }

    fun refreshBlocking(context: Context): List<OracleKnowledgeArticle> {
        val html = get(SOURCE_URL)
        val links = extractArticleLinks(html)
        val now = System.currentTimeMillis()
        val articles = ArrayList<OracleKnowledgeArticle>()
        for (url in links.take(MAX_ARTICLES)) {
            val page = runCatching { get(url) }.getOrNull() ?: continue
            val title = extractTitle(page)
            if (title.isBlank()) continue
            val content = extractContent(page)
            articles += OracleKnowledgeArticle(title, url, content.replace(Regex("\\s+"), " ").trim().take(420), content.take(12000), extractPublishedAt(page), now)
        }
        val finalItems = articles.distinctBy { it.url }.sortedByDescending { it.publishedAt }.take(MAX_ARTICLES)
        if (finalItems.isEmpty()) throw IllegalStateException("Nu au fost găsite articole pe $SOURCE_URL")
        val json = JSONArray().apply { finalItems.forEach { a -> put(JSONObject().apply { put("title", a.title); put("url", a.url); put("excerpt", a.excerpt); put("content", a.content); put("publishedAt", a.publishedAt); put("refreshedAt", a.refreshedAt) }) } }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(ITEMS, json.toString()).putLong(LAST_SUCCESS, now).remove(LAST_ERROR).apply()
        return finalItems
    }

    fun scheduleDaily(context: Context) {
        val app = context.applicationContext
        val alarm = app.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(app, OracleKnowledgeRefreshReceiver::class.java)
        val pending = android.app.PendingIntent.getBroadcast(app, 7107, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        alarm.cancel(pending)
        val first = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
        if (android.os.Build.VERSION.SDK_INT >= 23) alarm.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, first, pending) else alarm.set(android.app.AlarmManager.RTC_WAKEUP, first, pending)
    }

    private fun get(url: String): String {
        val separator = if (url.contains("?")) "&" else "?"
        val c = (URL(url + separator + "oracle_knowledge_refresh=" + System.currentTimeMillis()).openConnection() as HttpURLConnection).apply {
            connectTimeout = REQUEST_TIMEOUT; readTimeout = REQUEST_TIMEOUT; useCaches = false; requestMethod = "GET"
            setRequestProperty("User-Agent", "OracleKnowledge/1.0"); setRequestProperty("Accept", "text/html,application/xhtml+xml")
            setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0"); setRequestProperty("Pragma", "no-cache")
        }
        return try { if (c.responseCode !in 200..299) throw IllegalStateException("HTTP ${c.responseCode} pentru $url"); c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() } } finally { c.disconnect() }
    }

    private fun extractArticleLinks(html: String): List<String> {
        val out = LinkedHashSet<String>()
        val p = Pattern.compile("<a\\s+[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val m = p.matcher(html)
        while (m.find()) { val href = normalizeUrl(m.group(1) ?: continue); val text = cleanText(m.group(2) ?: ""); if (href.startsWith(SOURCE_URL) && href != SOURCE_URL && text.length >= 4 && !href.contains("/page/")) out.add(href) }
        return out.toList()
    }

    private fun normalizeUrl(raw: String): String = runCatching { val u = URI(raw.trim()); if (u.isAbsolute) u.toString().substringBefore('#') else URI(SOURCE_URL).resolve(u).toString().substringBefore('#') }.getOrDefault(raw.trim().substringBefore('#'))

    private fun extractTitle(html: String): String {
        val patterns = listOf("<meta\\s+property=[\"']og:title[\"'][^>]*content=[\"'](.*?)[\"']", "<title[^>]*>(.*?)</title>", "<h1[^>]*>(.*?)</h1>")
        for (x in patterns) { val m = Pattern.compile(x, Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(html); if (m.find()) return cleanText(m.group(1) ?: "") }
        return ""
    }

    private fun extractContent(html: String): String {
        val candidates = listOf("<article[^>]*>(.*?)</article>", "<main[^>]*>(.*?)</main>", "<div[^>]*class=[\"'][^\"']*(?:entry-content|post-content|article-content)[^\"']*[\"'][^>]*>(.*?)</div>")
        for (x in candidates) { val m = Pattern.compile(x, Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(html); if (m.find()) { val cleaned = cleanText(m.group(m.groupCount()) ?: ""); if (cleaned.length > 80) return cleaned } }
        return cleanText(html).take(1200)
    }

    private fun extractPublishedAt(html: String): Long {
        val p = Pattern.compile("<meta\\s+(?:property|name)=[\"'](?:article:published_time|date|pubdate)[\"'][^>]*content=[\"'](.*?)[\"']", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val m = p.matcher(html)
        return if (m.find()) runCatching { java.time.Instant.parse(m.group(1) ?: "").toEpochMilli() }.getOrDefault(0L) else 0L
    }

    private fun cleanText(raw: String): String {
        var s = raw.replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ").replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ").replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), " ").replace(Regex("</p>|</div>|</li>|</h[1-6]>", RegexOption.IGNORE_CASE), " ").replace(Regex("<[^>]+>"), " ")
        mapOf("&nbsp;" to " ", "&amp;" to "&", "&quot;" to "\"", "&#8211;" to "–", "&#8212;" to "—", "&#8217;" to "’", "&#8220;" to "“", "&#8221;" to "”").forEach { (a,b) -> s = s.replace(a,b,ignoreCase=true) }
        return s.replace(Regex("\\s+"), " ").trim()
    }
}

class OracleKnowledgeRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            OracleKnowledgeSync.scheduleDaily(context.applicationContext)
            return
        }
        val pending = goAsync()
        Thread {
            try {
                runCatching { OracleKnowledgeSync.refreshBlocking(context.applicationContext) }
                    .onFailure { context.applicationContext.getSharedPreferences("oracle_knowledge", Context.MODE_PRIVATE).edit().putString("last_error", it.message ?: it.javaClass.simpleName).apply() }
            } finally {
                OracleKnowledgeSync.scheduleDaily(context.applicationContext)
                pending.finish()
            }
        }.start()
    }
}
