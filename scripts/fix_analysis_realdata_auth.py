from pathlib import Path

# ANALYSIS_REALDATA_AUTH_V1
# Yahoo quoteSummary is crumb/cookie protected. The app previously called it
# without the required session handshake, so the code silently fell back to
# stale/calculated fundamentals. This patch adds the real Yahoo session
# handshake and makes quoteSummary the authoritative source for raw values.

p = Path('app/src/main/java/ro/alintudor/oracle/core/OracleRealData.kt')
s = p.read_text(encoding='utf-8')

marker = 'object OracleRealData {'
if marker not in s:
    raise SystemExit('OracleRealData object anchor missing')

if 'ANALYSIS_REALDATA_AUTH_V1' not in s:
    s = s.replace(marker, marker + '\n    // ANALYSIS_REALDATA_AUTH_V1', 1)

old = '''        val summary= listOf(
            "https://query2.finance.yahoo.com/v10/finance/quoteSummary/$symbol?modules=$modules&formatted=false&lang=en-US&region=US",
            "https://query1.finance.yahoo.com/v10/finance/quoteSummary/$symbol?modules=$modules&formatted=false&lang=en-US&region=US"
        ).mapNotNull { url -> runCatching { getJson(url) }.getOrNull() }
            .mapNotNull { parseQuoteSummary(it,symbol) }.firstOrNull()
'''
new = '''        val summary=runCatching { yahooQuoteSummary(symbol,modules) }
            .mapNotNull { parseQuoteSummary(it,symbol) }.getOrNull()
'''
if old in s:
    s = s.replace(old, new, 1)

# Replace the quote fallback with a crumb-authenticated quote request too.
old_quote = '''        val quote= listOf(
            "https://query1.finance.yahoo.com/v7/finance/quote?symbols=$symbol",
            "https://query2.finance.yahoo.com/v7/finance/quote?symbols=$symbol"
        ).mapNotNull { url -> runCatching { getJson(url) }.getOrNull() }
            .mapNotNull { parseQuoteFallback(it,symbol) }.firstOrNull()
'''
new_quote = '''        val quote=runCatching { yahooQuote(symbol) }
            .mapNotNull { parseQuoteFallback(it,symbol) }.getOrNull()
'''
if old_quote in s:
    s = s.replace(old_quote, new_quote, 1)

# Add the Yahoo session helpers immediately before the timeseries fetch.
anchor = '    private fun fetchTimeseries(symbol:String):JSONObject {'
if 'private fun yahooQuoteSummary(' not in s:
    idx = s.find(anchor)
    if idx < 0:
        raise SystemExit('Timeseries anchor missing')
    helpers = r'''    private data class YahooSession(val crumb:String,val cookie:String)

    private fun yahooSession():YahooSession {
        val ua="Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/126.0.0.0 Mobile Safari/537.36"
        var cookie=""
        val seed=(URL("https://fc.yahoo.com").openConnection() as HttpURLConnection).apply {
            connectTimeout=TIMEOUT; readTimeout=TIMEOUT; requestMethod="GET"; setRequestProperty("User-Agent",ua)
        }
        try {
            cookie=seed.headerFields.entries.firstOrNull { it.key?.equals("Set-Cookie",true)==true }
                ?.value?.firstOrNull()?.substringBefore(';') ?: ""
        } finally { runCatching { seed.inputStream.close() }; seed.disconnect() }
        if(cookie.isBlank()) throw IllegalStateException("Yahoo session cookie unavailable")

        val crumbConn=(URL("https://query1.finance.yahoo.com/v1/test/getcrumb").openConnection() as HttpURLConnection).apply {
            connectTimeout=TIMEOUT; readTimeout=TIMEOUT; requestMethod="GET"
            setRequestProperty("User-Agent",ua); setRequestProperty("Cookie",cookie); setRequestProperty("Accept","text/plain")
        }
        val crumb=try { crumbConn.inputStream.bufferedReader().use{it.readText()}.trim() } finally { crumbConn.disconnect() }
        if(crumb.isBlank() || crumb.contains("Too Many Requests",true) || crumb.startsWith("<")) throw IllegalStateException("Yahoo crumb unavailable")
        return YahooSession(crumb,cookie)
    }

    private fun yahooGetJson(url:String):JSONObject {
        val session=yahooSession()
        val sep=if(url.contains('?')) '&' else '?'
        val target=url+sep+"crumb="+URLEncoder.encode(session.crumb,"UTF-8")
        val ua="Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/126.0.0.0 Mobile Safari/537.36"
        val c=(URL(target).openConnection() as HttpURLConnection).apply {
            connectTimeout=TIMEOUT; readTimeout=TIMEOUT; requestMethod="GET"
            setRequestProperty("User-Agent",ua); setRequestProperty("Cookie",session.cookie); setRequestProperty("Accept","application/json"); setRequestProperty("Referer","https://finance.yahoo.com/")
        }
        return try { JSONObject(c.inputStream.bufferedReader().use{it.readText()}) } finally { c.disconnect() }
    }

    private fun yahooQuoteSummary(symbol:String,modules:String):JSONObject = yahooGetJson(
        "https://query2.finance.yahoo.com/v10/finance/quoteSummary/$symbol?modules=$modules&formatted=false&lang=en-US&region=US"
    )

    private fun yahooQuote(symbol:String):JSONObject = yahooGetJson(
        "https://query1.finance.yahoo.com/v7/finance/quote?symbols=$symbol&formatted=false&lang=en-US&region=US"
    )

'''
    s = s[:idx] + helpers + s[idx:]

p.write_text(s, encoding='utf-8')
print('Yahoo crumb/cookie authentication patch applied to Analysis fundamentals')
