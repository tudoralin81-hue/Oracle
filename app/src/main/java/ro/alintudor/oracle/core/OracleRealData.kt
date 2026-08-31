package ro.alintudor.oracle.core

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/** Real non-OHLC data used by Analysis/Growth. */
data class OracleFundamentals(
    val sector: String?, val industry: String?, val trailingPe: Double?, val forwardPe: Double?,
    val revenueGrowth: Double?, val earningsGrowth: Double?, val profitMargin: Double?,
    val operatingMargin: Double?, val returnOnEquity: Double?, val debtToEquity: Double?,
    val marketCap: Double?, val rawText: String
)
data class OracleNewsContext(val score:Int,val headlineCount:Int,val positiveHits:Int,val negativeHits:Int,val topHeadline:String?)
data class OracleMarketContext(val market5D:Double?,val market20D:Double?,val sector5D:Double?,val sector20D:Double?,val sectorEtf:String?,val rawText:String)

object OracleRealData {
    private const val TIMEOUT=7000

    /** Yahoo assetProfile is primary; known tickers provide a deterministic fallback. */
    fun resolvedSector(ticker:String, remoteSector:String?=null):String? {
        remoteSector?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        return knownSector(ticker.trim().uppercase(Locale.US))
    }

    /**
     * Fundamentals are real Yahoo Finance fields. Try both Yahoo quoteSummary hosts,
     * then the quote endpoint for the subset it exposes. Never synthesize fundamentals.
     */
    fun fundamentals(ticker:String):OracleFundamentals? {
        val symbol=ticker.uppercase(Locale.US)
        val modules="price,summaryDetail,defaultKeyStatistics,financialData,assetProfile"
        val roots=listOf(
            "https://query2.finance.yahoo.com/v10/finance/quoteSummary/$symbol?modules=$modules",
            "https://query1.finance.yahoo.com/v10/finance/quoteSummary/$symbol?modules=$modules"
        ).mapNotNull { url -> runCatching { getJson(url) }.getOrNull() }

        roots.mapNotNull { parseQuoteSummary(it,symbol) }.firstOrNull()?.let { return it }

        val quoteRoots=listOf(
            "https://query1.finance.yahoo.com/v7/finance/quote?symbols=$symbol",
            "https://query2.finance.yahoo.com/v7/finance/quote?symbols=$symbol"
        ).mapNotNull { url -> runCatching { getJson(url) }.getOrNull() }
        quoteRoots.mapNotNull { parseQuoteFallback(it,symbol) }.firstOrNull()?.let { return it }

        val sector=resolvedSector(symbol) ?: return null
        return OracleFundamentals(
            sector,null,null,null,null,null,null,null,null,null,null,
            "Sector=$sector; Industry=—; P/E=—; Fwd P/E=—; Revenue growth=—; Earnings growth=—; Net margin=—; Op margin=—; ROE=—; D/E=—; Market cap=—"
        )
    }

    private fun parseQuoteSummary(root:JSONObject,ticker:String):OracleFundamentals? = try {
        val r=root.optJSONObject("quoteSummary")?.optJSONArray("result")?.optJSONObject(0) ?: return null
        val profile=r.optJSONObject("assetProfile")
        val sd=r.optJSONObject("summaryDetail")
        val ks=r.optJSONObject("defaultKeyStatistics")
        val fd=r.optJSONObject("financialData")
        val price=r.optJSONObject("price")
        val sector=resolvedSector(ticker,profile?.optString("sector")?.takeIf{it.isNotBlank()})
        val industry=profile?.optString("industry")?.takeIf{it.isNotBlank()}
        val pe=num(sd,"trailingPE")?:num(ks,"trailingPE")
        val fpe=num(sd,"forwardPE")?:num(ks,"forwardPE")
        val rg=num(fd,"revenueGrowth")
        val eg=num(fd,"earningsGrowth")
        val pm=num(fd,"profitMargins")
        val om=num(fd,"operatingMargins")
        val roe=num(fd,"returnOnEquity")
        val de=num(fd,"debtToEquity")
        val cap=num(sd,"marketCap")?:num(price,"marketCap")
        val text=buildFundamentalText(sector,industry,pe,fpe,rg,eg,pm,om,roe,de,cap)
        OracleFundamentals(sector,industry,pe,fpe,rg,eg,pm,om,roe,de,cap,text)
    } catch(_:Exception) { null }

    private fun parseQuoteFallback(root:JSONObject,ticker:String):OracleFundamentals? = try {
        val q=root.optJSONObject("quoteResponse")?.optJSONArray("result")?.optJSONObject(0) ?: return null
        val sector=resolvedSector(ticker)
        val industry=q.optString("industry").takeIf{it.isNotBlank()}
        val pe=num(q,"trailingPE")
        val fpe=num(q,"forwardPE")
        val cap=num(q,"marketCap")
        val text=buildFundamentalText(sector,industry,pe,fpe,null,null,null,null,null,null,cap)
        OracleFundamentals(sector,industry,pe,fpe,null,null,null,null,null,null,cap,text)
    } catch(_:Exception) { null }

    private fun num(o:JSONObject?,key:String):Double? {
        val value=o?.opt(key) ?: return null
        val x=when(value){
            is Number -> value.toDouble()
            is JSONObject -> value.optDouble("raw",Double.NaN)
            else -> Double.NaN
        }
        return x.takeIf { it.isFinite() }
    }

    private fun buildFundamentalText(
        sector:String?,industry:String?,pe:Double?,fpe:Double?,rg:Double?,eg:Double?,pm:Double?,om:Double?,roe:Double?,de:Double?,cap:Double?
    ):String = buildString {
        append("Sector=${sector?:"—"}; Industry=${industry?:"—"}; ")
        append("P/E=${pe?.let{"%.2f".format(Locale.US,it)}?:"—"}; ")
        append("Fwd P/E=${fpe?.let{"%.2f".format(Locale.US,it)}?:"—"}; ")
        append("Revenue growth=${pct(rg)}; Earnings growth=${pct(eg)}; ")
        append("Net margin=${pct(pm)}; Op margin=${pct(om)}; ROE=${pct(roe)}; ")
        append("D/E=${de?.let{"%.1f".format(Locale.US,it)}?:"—"}; Market cap=${moneyCap(cap)}")
    }

    private fun knownSector(ticker:String):String?=when(ticker){
        "NVDA","AMD","AVGO","QCOM","MU","MRVL","ARM","INTC","TSM","ASML","LRCX","AMAT","KLAC","ON","MPWR","ADI","TXN","NXPI","MCHP","SWKS","STM","WDC","STX","SMCI","CRDO","AAOI","AAPL","MSFT","ORCL","CRM","NOW","ADBE","INTU","SNOW","PLTR","PANW","CRWD","NET","DDOG","MDB","SHOP","TEAM","VEEV","SNPS","CDNS","FTNT","ZS","WDAY","ROP","ACN","IBM","SAP","CSCO","ANET","DELL","HPE"->"Information Technology"
        "GOOGL","GOOG","META","NFLX","DIS","CMCSA","TMUS","VZ","T","CHTR","WBD","SPOT"->"Communication Services"
        "AMZN","TSLA","HD","LOW","MCD","NKE","SBUX","BKNG","ABNB","TJX","TGT","GM","F","LULU"->"Consumer Discretionary"
        "WMT","COST","PG","KO","PEP","PM","MO","CL","KMB"->"Consumer Staples"
        "LLY","JNJ","UNH","MRK","PFE","ABBV","TMO","DHR","ABT","ISRG","VRTX","REGN","GILD","AMGN","MRNA","CRSP"->"Health Care"
        "JPM","BAC","WFC","C","GS","MS","BLK","SCHW","COF","AXP","V","MA","PYPL","HOOD","COIN"->"Financials"
        "GE","CAT","DE","HON","RTX","BA","LMT","NOC","GD","ETN","EMR","UNP","UPS","FDX","RHM"->"Industrials"
        "XOM","CVX","COP","SLB","EOG","OXY","MPC","VLO","HAL","FANG"->"Energy"
        "LIN","APD","SHW","FCX","NEM","NUE","DOW","DD","ALB"->"Materials"
        "NEE","DUK","SO","AEP","EXC","SRE","D"->"Utilities"
        "PLD","AMT","EQIX","CCI","O","SPG","WELL","DLR"->"Real Estate"
        else->null
    }

    fun newsContext(ticker:String):OracleNewsContext=try{
        val q=URLEncoder.encode("\"${ticker.uppercase(Locale.US)}\" stock when:7d","UTF-8"); val body=getText("https://news.google.com/rss/search?q=$q&hl=en-US&gl=US&ceid=US:en")
        val titles=Regex("<title>(.*?)</title>",RegexOption.IGNORE_CASE).findAll(body).map{it.groupValues[1].replace("&amp;","&").replace("&quot;","\"")}.filter{!it.equals("Google News",true)}.take(8).toList()
        val positive=listOf("beat","upgrade","buy","bullish","record","strong","surge","contract","partnership","deal","approval","launch","growth","profit"); val negative=listOf("miss","downgrade","sell","bearish","lawsuit","investigation","warning","cut guidance","recall","layoff","fraud","delay","loss","decline","plunge","offering","dilution","bankruptcy")
        val pos=titles.sumOf{t->positive.count{t.contains(it,true)}}; val neg=titles.sumOf{t->negative.count{t.contains(it,true)}}
        OracleNewsContext((50+pos*5-neg*7).coerceIn(0,100),titles.size,pos,neg,titles.firstOrNull())
    }catch(_:Exception){OracleNewsContext(50,0,0,0,null)}

    fun marketContext(sector:String?):OracleMarketContext{val etf=sectorEtf(sector);val spy=returns("SPY");val sec=etf?.let{returns(it)};return OracleMarketContext(spy?.first,spy?.second,sec?.first,sec?.second,etf,"SPY 5D=${pct(spy?.first)}; SPY 20D=${pct(spy?.second)}; ${etf?:"Sector ETF"} 5D=${pct(sec?.first)}; ${etf?:"Sector ETF"} 20D=${pct(sec?.second)}")}
    fun sectorScore(ctx:OracleMarketContext):Double?{val v=listOfNotNull(ctx.market5D,ctx.market20D,ctx.sector5D,ctx.sector20D);if(v.isEmpty())return null;return(50.0+v.map{it*100.0*2.2}.average()).coerceIn(0.0,100.0)}
    fun fundamentalScore(f:OracleFundamentals?):Double?{if(f==null)return null;val p=mutableListOf<Double>();f.revenueGrowth?.let{p+=(50+it*180).coerceIn(0.0,100.0)};f.earningsGrowth?.let{p+=(50+it*150).coerceIn(0.0,100.0)};f.profitMargin?.let{p+=(50+it*220).coerceIn(0.0,100.0)};f.operatingMargin?.let{p+=(50+it*180).coerceIn(0.0,100.0)};f.returnOnEquity?.let{p+=(50+it*100).coerceIn(0.0,100.0)};f.debtToEquity?.let{p+=(75-it*0.12).coerceIn(0.0,100.0)};f.forwardPe?.let{p+=when{it<=0->35.0;it<=15->85.0;it<=25->70.0;it<=40->55.0;else->35.0}};return p.takeIf{it.isNotEmpty()}?.average()?.coerceIn(0.0,100.0)}
    private fun returns(ticker:String):Pair<Double,Double>?{val d=OracleMarketData.fetchDaily(ticker,"3mo").sortedByDescending{it.timestamp};if(d.size<=20)return null;val p=d[0].close;val p5=d.getOrNull(5)?.close?:return null;val p20=d.getOrNull(20)?.close?:return null;return Pair(p/p5-1,p/p20-1)}
    private fun sectorEtf(sector:String?):String?{val s=sector?.lowercase(Locale.US)?:return null;return when{ "semiconductor" in s||"technology" in s||"software" in s->"XLK";"communication" in s||"telecom" in s->"XLC";"health" in s||"biotech" in s->"XLV";"financial" in s||"bank" in s->"XLF";"industrial" in s->"XLI";"energy" in s->"XLE";"consumer" in s&&"cyclical" in s->"XLY";"consumer" in s&&"discretionary" in s->"XLY";"consumer" in s||"staples" in s->"XLP";"utility" in s->"XLU";"real estate" in s->"XLRE";"material" in s->"XLB";else->null}}
    private fun getText(url:String):String{val c=(URL(url).openConnection() as HttpURLConnection).apply{connectTimeout=TIMEOUT;readTimeout=TIMEOUT;requestMethod="GET";setRequestProperty("User-Agent","Oracle-Stock-Intelligence/1.0")};return try{c.inputStream.bufferedReader().use{it.readText()}}finally{c.disconnect()}}
    private fun getJson(url:String):JSONObject{val c=(URL(url).openConnection() as HttpURLConnection).apply{connectTimeout=TIMEOUT;readTimeout=TIMEOUT;requestMethod="GET";setRequestProperty("User-Agent","Oracle-Stock-Intelligence/1.0");setRequestProperty("Accept","application/json")};return try{JSONObject(c.inputStream.bufferedReader().use{it.readText()})}finally{c.disconnect()}}
    private fun pct(v:Double?):String=v?.let{"%.2f%%".format(Locale.US,it*100)}?:"—"; private fun moneyCap(v:Double?):String=when{v==null->"—";v>=1e12->"%.2fT".format(Locale.US,v/1e12);v>=1e9->"%.2fB".format(Locale.US,v/1e9);v>=1e6->"%.2fM".format(Locale.US,v/1e6);else->"%.0f".format(Locale.US,v)}
}
