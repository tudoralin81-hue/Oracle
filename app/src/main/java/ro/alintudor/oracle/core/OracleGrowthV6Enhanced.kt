package ro.alintudor.oracle.core

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Oracle Growth V6 Enhanced.
 *
 * Build 234 / V5.9.7 remains the regression reference. V6 is deliberately
 * implemented as a separate engine so the baseline is never mutated.
 *
 * Pipeline:
 * 1) deterministic 1,000-symbol universe
 * 2) concurrent 3mo liquidity/momentum pre-screen
 * 3) concurrent full 1y technical evaluation on the best 250 candidates
 * 4) news/catalyst enrichment on the best 60 candidates
 * 5) independent SHORT/MEDIUM/LONG ranking
 */
object OracleGrowthV6Enhanced {
    private const val MAX_SYMBOLS = 1000
    private const val PREFILTER_SIZE = 250
    private const val NEWS_SIZE = 60
    private const val WORKERS = 12

    private val weights = mapOf(
        "SHORT" to intArrayOf(21,18,12,16,12,8,3,4,2,2,1,1),
        "MEDIUM" to intArrayOf(12,12,16,12,9,9,9,5,6,5,4,1),
        "LONG" to intArrayOf(6,6,20,6,5,8,18,4,9,7,9,2)
    )
    private val keys = listOf(
        "news","breakout","trend","momentum","volume","support_resistance",
        "fundamentals","bollinger","ichimoku","market_sector","risk_reward","adx"
    )

    private data class C(
        val ticker: String,
        val price: Double,
        val rsi: Double,
        val mom5: Double,
        val mom20: Double,
        val vr: Double,
        val macdLine: Double,
        val macdSignal: Double,
        val macdHist: Double,
        val macdSlope: Double,
        val ichi: Double,
        val sma200: Double?,
        val sma50: Double?,
        val adx: Double,
        val diPlus: Double,
        val diMinus: Double,
        val atrPct: Double,
        val components: Map<String,Double>,
        val forecast: Map<String,Double>,
        val risk: String,
        val allocation: Double,
        val confidence: Int,
        val dataQuality: Int,
        val regime: String
    )

    fun run(seed: List<OracleGrowthRecommendation> = emptyList()): List<OracleGrowthRecommendation> {
        val byTicker = seed.associateBy { it.ticker.uppercase(Locale.US) }
        val symbols = OracleGrowthUniverse.all.take(MAX_SYMBOLS)

        val pre = concurrent(symbols) { ticker ->
            val candles = OracleMarketData.fetchDaily(ticker, "3mo")
            if (candles.size < 45) null else {
                val c = candles.sortedByDescending { it.timestamp }
                val closes = c.map { it.close }
                val volumes = c.map { it.volume }
                val price = closes.firstOrNull() ?: return@concurrent null
                val m5 = if (closes.size > 5) (price / closes[5] - 1.0) * 100.0 else 0.0
                val m20 = if (closes.size > 20) (price / closes[20] - 1.0) * 100.0 else 0.0
                val vr = if (volumes.size >= 20 && volumes.take(20).average() > 0)
                    volumes[0] / volumes.take(20).average() else 1.0
                val quick = 50.0 + m5 * 1.6 + m20 * .45 + (vr - 1.0) * 20.0
                ticker to quick
            }
        }.filterNotNull().sortedByDescending { it.second }.take(PREFILTER_SIZE).map { it.first }

        val evaluated = concurrent(pre) { ticker ->
            val candles = OracleMarketData.fetchDaily(ticker, "1y")
            if (candles.size < 60) null else evaluate(ticker, candles)
        }.filterNotNull()

        if (evaluated.isEmpty()) return emptyList()

        val newsCandidates = evaluated.sortedByDescending { baseRank(it, "SHORT") }
            .take(NEWS_SIZE).map { it.ticker }
        val newsMap = concurrent(newsCandidates) { ticker -> ticker to newsScore(ticker) }
            .filterNotNull().toMap()

        val enriched = evaluated.map { c ->
            val news = newsMap[c.ticker] ?: 0
            val newsFactor = (50.0 + news * 5.0).coerceIn(0.0, 100.0)
            val components = c.components.toMutableMap()
            components["news"] = newsFactor
            val confidence = confidence(components, c.dataQuality)
            c.copy(components = components, confidence = confidence)
        }

        val out = mutableListOf<OracleGrowthRecommendation>()
        val used = mutableSetOf<String>()
        for (horizon in listOf("SHORT","MEDIUM","LONG")) {
            val ranked = enriched.sortedWith(
                compareByDescending<C> { enhancedScore(it, horizon) }
                    .thenByDescending { tie(it, horizon) }
                    .thenByDescending { it.confidence }
            )
            val pick = ranked.firstOrNull { it.ticker !in used } ?: continue
            used += pick.ticker
            val score = enhancedScore(pick, horizon)
            val meta = byTicker[pick.ticker]
            out += OracleGrowthRecommendation(
                horizon = horizon,
                ticker = pick.ticker,
                company = meta?.company?.takeIf { it.isNotBlank() } ?: companyName(pick.ticker),
                sector = meta?.sector?.takeIf { it.isNotBlank() } ?: "US Equity",
                score = score,
                signal = rating(score),
                risk = pick.risk,
                allocationMax = pick.allocation,
                forecastPct = pick.forecast[horizon.lowercase(Locale.US)] ?: 0.0,
                momentum5D = pick.mom5,
                momentum20D = pick.mom20,
                weights = weights[horizon]!!.toList(),
                newsTitle = meta?.newsTitle ?: "",
                newsSource = meta?.newsSource ?: "",
                referenceTimestamp = meta?.referenceTimestamp ?: 0L,
                currentPrice = pick.price,
                adx = pick.adx,
                factorValues = keys.map { pick.components[it] ?: 50.0 },
                factorScore = score.toDouble(),
                generatedAt = System.currentTimeMillis(),
                source = "ORACLE_ENGINE_V6_ENHANCED",
                confidence = pick.confidence,
                dataQuality = pick.dataQuality,
                regime = pick.regime
            )
        }
        return out
    }

    private fun evaluate(ticker: String, candles: List<OracleOhlcvPoint>): C? {
        val r = candles.sortedByDescending { it.timestamp }
        val close = r.map { it.close }
        val high = r.map { it.high }
        val low = r.map { it.low }
        val volume = r.map { it.volume }
        if (close.size < 60 || close.firstOrNull() ?: 0.0 <= 0.0) return null

        val p = close[0]
        fun avg(n: Int): Double? = if (close.size >= n) close.take(n).average() else null
        fun std(n: Int): Double? {
            if (close.size < n) return null
            val a = close.take(n)
            val m = a.average()
            return sqrt(a.sumOf { (it - m) * (it - m) } / n)
        }
        fun mom(n: Int): Double = if (close.size > n) (p / close[n] - 1.0) * 100.0 else 0.0

        val sma20 = avg(20)
        val sma50 = avg(50)
        val sma200 = avg(200)
        val m5 = mom(5)
        val m20 = mom(20)

        val gains = close.dropLast(1).take(14).mapIndexed { i, x -> max(0.0, x - close[i + 1]) }.average()
        val losses = close.dropLast(1).take(14).mapIndexed { i, x -> max(0.0, close[i + 1] - x) }.average()
        val rsi = if (losses == 0.0) 100.0 else 100.0 - 100.0 / (1.0 + gains / losses)

        val v20 = if (volume.size >= 20) volume.take(20).average() else 0.0
        val vr = if (v20 > 0.0) volume[0] / v20 else 1.0

        val prior20 = if (close.size >= 21) close.drop(1).take(20).maxOrNull() ?: p else p
        val breakout = when {
            p > prior20 && vr >= 1.25 -> 100.0
            p > prior20 -> 68.0
            p >= prior20 * .97 -> 50.0
            else -> 25.0
        }

        val lo20 = close.take(20).minOrNull() ?: p
        val hi20 = close.take(20).maxOrNull() ?: p
        val sr = if (hi20 > lo20) (30.0 + 70.0 * (p - lo20) / (hi20 - lo20)).coerceIn(0.0, 100.0) else 50.0

        val mid = sma20
        val sd = std(20)
        val bbPos = if (mid != null && sd != null && sd > 0.0) (p - (mid - 2.0 * sd)) / (4.0 * sd) else .5
        val bbWidth = if (mid != null && mid > 0.0) 100.0 * 4.0 * (sd ?: 0.0) / mid else 0.0
        val bollinger = (50.0 + (bbPos - .5) * 80.0 + if (bbWidth in 0.0..8.0) 10.0 else 0.0).coerceIn(0.0, 100.0)

        val ema12 = ema(close, 12)
        val ema26 = ema(close, 26)
        val macdLine = (ema12 ?: p) - (ema26 ?: p)
        val signal = macdSignal(close)
        val hist = macdLine - signal
        val prevMacd = if (close.size > 2) {
            val e12 = ema(close.drop(1), 12) ?: ema12 ?: p
            val e26 = ema(close.drop(1), 26) ?: ema26 ?: p
            e12 - e26
        } else macdLine
        val prevSignal = if (close.size > 3) macdSignal(close.drop(1)) else signal
        val macdSlope = (macdLine - prevMacd) - (signal - prevSignal)
        val macdNorm = if (p > 0) 50.0 + (hist / p) * 25000.0 + (macdSlope / p) * 12500.0 else 50.0

        val atrValue = atr(high, low, close, 14) ?: p * .01
        val atrPct = 100.0 * atrValue / p
        val adxData = adx(high, low, close, 14)
        val adx = adxData.first
        val diPlus = adxData.second
        val diMinus = adxData.third
        val ichimoku = ichimokuScore(high, low, close)

        val trend = (
            50.0 +
                (if (sma20 != null && p > sma20) 12.0 else -12.0) +
                (if (sma50 != null && p > sma50) 18.0 else -18.0) +
                (if (sma200 != null && p > sma200) 20.0 else -20.0) +
                (if (sma20 != null && sma50 != null && sma20 > sma50) 8.0 else -8.0)
        ).coerceIn(0.0, 100.0)

        val momentum = (
            50.0 + m5 * 1.7 + m20 * .55 + (macdNorm - 50.0) * .28 +
                (if (rsi in 52.0..72.0) 4.0 else if (rsi > 80.0) -6.0 else 0.0)
        ).coerceIn(0.0, 100.0)

        val volumeFactor = (50.0 + (vr - 1.0) * 45.0).coerceIn(0.0, 100.0)
        val adxFactor = (45.0 + adx * 1.15 + (if (diPlus > diMinus) 10.0 else -10.0)).coerceIn(0.0, 100.0)
        val riskReward = (72.0 - atrPct * 5.0 + (if (breakout >= 90.0) 10.0 else 0.0) + (if (rsi in 45.0..68.0) 5.0 else 0.0)).coerceIn(0.0, 100.0)

        val regime = when {
            atrPct >= 7.0 -> "HIGH VOLATILITY"
            trend >= 72.0 && momentum >= 60.0 && adx >= 22.0 -> "BULL"
            trend <= 38.0 && momentum <= 42.0 && adx >= 22.0 -> "BEAR"
            else -> "NEUTRAL"
        }

        val dataQuality = (
            40 +
                (if (close.size >= 200) 25 else 10) +
                (if (volume.count { it > 0.0 } >= 40) 15 else 5) +
                (if (adx > 0.0) 10 else 0) +
                (if (ema12 != null && ema26 != null) 10 else 0)
        ).coerceIn(0, 100)

        val components = mapOf(
            "news" to 50.0,
            "breakout" to breakout,
            "trend" to trend,
            "momentum" to momentum,
            "volume" to volumeFactor,
            "support_resistance" to sr,
            "fundamentals" to 50.0,
            "bollinger" to bollinger,
            "ichimoku" to ichimoku,
            "market_sector" to 50.0,
            "risk_reward" to riskReward,
            "adx" to adxFactor
        )

        val confidence = confidence(components, dataQuality)
        val base = baseRank(components, "SHORT")
        val risk = when {
            atrPct > 7.0 || rsi > 78.0 || vr > 2.5 -> "RIDICAT"
            regime == "BULL" && riskReward >= 60.0 -> "MEDIU"
            else -> "RIDICAT"
        }

        var allocation = when {
            base >= 88 -> 8.0
            base >= 82 -> 7.0
            base >= 76 -> 6.0
            base >= 70 -> 5.0
            base >= 64 -> 4.0
            else -> 2.0
        }
        if (risk == "RIDICAT") allocation = min(allocation, 4.0)
        if (confidence < 65) allocation = min(allocation, 3.0)
        if (atrPct > 7.0) allocation = min(allocation, 3.0)
        if (rsi > 80.0) allocation = min(allocation, 3.0)

        val forecast = mapOf(
            "short" to forecast(p, atrValue, m5, trend, adx, regime, 1.6, 35.0),
            "medium" to forecast(p, atrValue, m20, trend, adx, regime, 3.8, 60.0),
            "long" to forecast(p, atrValue, m20, trend, adx, regime, 6.5, 90.0)
        )

        return C(
            ticker, p, rsi, m5, m20, vr, macdLine, signal, hist, macdSlope,
            ichimoku, sma200, sma50, adx, diPlus, diMinus, atrPct,
            components, forecast, risk, allocation, confidence, dataQuality, regime
        )
    }

    private fun <T> concurrent(tickers: List<String>, block: (String) -> T?): List<T> {
        val executor = Executors.newFixedThreadPool(WORKERS)
        return try {
            val futures = tickers.map { ticker -> executor.submit(Callable { runCatching { block(ticker) }.getOrNull() }) }
            futures.mapNotNull { runCatching { it.get(20, TimeUnit.SECONDS) }.getOrNull() }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun baseRank(c: C, horizon: String): Int = baseRank(c.components, horizon)

    private fun baseRank(c: Map<String, Double>, horizon: String): Int {
        val w = weights[horizon] ?: weights["SHORT"]!!
        return keys.indices.sumOf { i -> (c[keys[i]] ?: 50.0) * w[i] / 100.0 }.toInt().coerceIn(0, 100)
    }

    private fun enhancedScore(c: C, horizon: String): Int {
        var score = baseRank(c, horizon).toDouble()
        val trend = c.components["trend"] ?: 50.0
        val momentum = c.components["momentum"] ?: 50.0
        val volume = c.components["volume"] ?: 50.0
        val breakout = c.components["breakout"] ?: 50.0
        val rr = c.components["risk_reward"] ?: 50.0
        val adx = c.components["adx"] ?: 50.0
        val ichi = c.components["ichimoku"] ?: 50.0
        val bullish = listOf(trend, momentum, volume, ichi, adx).count { it >= 60.0 }
        val bearish = listOf(trend, momentum, volume, ichi, adx).count { it <= 40.0 }
        if (bullish >= 4) score += 3.0
        if (bearish >= 4) score -= 3.0
        if (breakout >= 85.0 && volume < 45.0) score -= 3.0
        if (trend >= 70.0 && momentum < 42.0) score -= 2.5
        if (rr < 40.0) score -= 2.0
        if (adx >= 65.0 && trend >= 65.0 && c.diPlus > c.diMinus) score += 1.5
        score += when (horizon) {
            "SHORT" -> (momentum - 50.0) * .035 + (volume - 50.0) * .02
            "MEDIUM" -> (trend - 50.0) * .03 + (ichi - 50.0) * .025
            else -> (trend - 50.0) * .035 + (rr - 50.0) * .025
        }
        if (c.confidence < 60) score -= 2.0
        return score.roundToInt().coerceIn(0, 100)
    }

    private fun tie(c: C, horizon: String): Double = when (horizon) {
        "SHORT" -> c.mom5 * .12 + (c.vr - 1.0) * .8 + if (c.macdHist > 0) .7 else -.7
        "MEDIUM" -> c.mom20 * .07 + (if (c.ichi >= 60) .7 else -.3) + (if (c.adx >= 20) .5 else 0.0)
        else -> c.mom20 * .05 + (if (c.sma200 != null && c.price > c.sma200) .8 else 0.0) +
            (if (c.sma50 != null && c.price > c.sma50) .4 else 0.0) + (if (c.ichi >= 60) .8 else -.2)
    }

    private fun confidence(components: Map<String, Double>, dataQuality: Int): Int {
        val vals = keys.map { components[it] ?: 50.0 }
        val mean = vals.average()
        val dispersion = vals.map { abs(it - mean) }.average()
        val agreement = (100.0 - dispersion * 1.35).coerceIn(0.0, 100.0)
        return (.65 * dataQuality + .35 * agreement).roundToInt().coerceIn(0, 100)
    }

    private fun forecast(price: Double, atrValue: Double, momentum: Double, trend: Double, adx: Double, regime: String, multiplier: Double, cap: Double): Double {
        if (price <= 0.0) return 0.0
        var move = atrValue * multiplier / price * 100.0
        move *= 1.0 + momentum.coerceIn(-50.0, 50.0) / 250.0
        move *= 1.0 + (trend - 50.0) / 500.0
        move *= 1.0 + (adx.coerceIn(0.0, 50.0) - 20.0) / 500.0
        if (regime == "BEAR") move *= .65
        if (regime == "HIGH VOLATILITY") move *= .82
        return move.coerceIn(0.0, cap)
    }

    private fun companyName(ticker: String): String = when (ticker.uppercase(Locale.US)) {
        "NOW" -> "ServiceNow, Inc."
        "CRM" -> "Salesforce, Inc."
        "VEEV" -> "Veeva Systems Inc."
        "NVDA" -> "NVIDIA Corporation"
        "AMD" -> "Advanced Micro Devices, Inc."
        "MSFT" -> "Microsoft Corporation"
        "AMZN" -> "Amazon.com, Inc."
        "META" -> "Meta Platforms, Inc."
        "GOOGL" -> "Alphabet Inc."
        "AAPL" -> "Apple Inc."
        "TSLA" -> "Tesla, Inc."
        "AVGO" -> "Broadcom Inc."
        "PLTR" -> "Palantir Technologies Inc."
        "CRWD" -> "CrowdStrike Holdings, Inc."
        "PANW" -> "Palo Alto Networks, Inc."
        "ADBE" -> "Adobe Inc."
        "SNOW" -> "Snowflake Inc."
        "DDOG" -> "Datadog, Inc."
        "UBER" -> "Uber Technologies, Inc."
        "SHOP" -> "Shopify Inc."
        "COIN" -> "Coinbase Global, Inc."
        "HOOD" -> "Robinhood Markets, Inc."
        else -> ticker
    }

    private fun rating(score: Int) = when {
        score >= 85 -> "STRONG BUY"
        score >= 75 -> "BUY"
        score >= 65 -> "HOLD"
        score >= 55 -> "WATCH"
        else -> "AVOID"
    }

    private fun ema(values: List<Double>, n: Int): Double? {
        if (values.size < n) return null
        var e = values.takeLast(n).average()
        val k = 2.0 / (n + 1.0)
        for (i in values.size - n until values.size) e = values[i] * k + e * (1.0 - k)
        return e
    }

    private fun macdSignal(values: List<Double>): Double {
        if (values.size < 35) return 0.0
        val hist = ArrayList<Double>()
        for (i in 0..min(values.size - 1, 80)) {
            val sub = values.drop(i)
            val e12 = ema(sub, 12) ?: continue
            val e26 = ema(sub, 26) ?: continue
            hist += e12 - e26
        }
        if (hist.isEmpty()) return 0.0
        var e = hist.takeLast(min(9, hist.size)).average()
        val k = 2.0 / 10.0
        for (i in max(0, hist.size - 9) until hist.size) e = hist[i] * k + e * (1.0 - k)
        return e
    }

    private fun atr(h: List<Double>, l: List<Double>, c: List<Double>, n: Int): Double? {
        if (c.size < n + 1) return null
        val tr = (0 until c.size - 1).map { maxOf(h[it] - l[it], abs(h[it] - c[it + 1]), abs(l[it] - c[it + 1])) }
        return tr.take(n).average()
    }

    private fun adx(h: List<Double>, l: List<Double>, c: List<Double>, n: Int): Triple<Double, Double, Double> {
        if (c.size < n * 2 + 2) return Triple(0.0, 0.0, 0.0)
        val tr = mutableListOf<Double>()
        val pd = mutableListOf<Double>()
        val md = mutableListOf<Double>()
        for (i in 0 until c.size - 1) {
            val up = h[i] - h[i + 1]
            val down = l[i + 1] - l[i]
            tr += maxOf(h[i] - l[i], abs(h[i] - c[i + 1]), abs(l[i] - c[i + 1]))
            pd += if (up > down && up > 0) up else 0.0
            md += if (down > up && down > 0) down else 0.0
        }
        var atr = tr.take(n).average()
        var plus = pd.take(n).average()
        var minus = md.take(n).average()
        val dx = mutableListOf<Double>()
        var lastPlus = 0.0
        var lastMinus = 0.0
        for (i in 0 until n) {
            if (i > 0) {
                atr = (atr * (n - 1) + tr[i]) / n
                plus = (plus * (n - 1) + pd[i]) / n
                minus = (minus * (n - 1) + md[i]) / n
            }
            val diP = if (atr > 0) 100.0 * plus / atr else 0.0
            val diM = if (atr > 0) 100.0 * minus / atr else 0.0
            lastPlus = diP
            lastMinus = diM
            dx += if (diP + diM > 0) 100.0 * abs(diP - diM) / (diP + diM) else 0.0
        }
        return Triple(dx.average(), lastPlus, lastMinus)
    }

    private fun ichimokuScore(h: List<Double>, l: List<Double>, c: List<Double>): Double {
        if (c.size < 52) return 50.0
        fun mid(n: Int): Double = (h.take(n).max() + l.take(n).min()) / 2.0
        val tenkan = mid(9)
        val kijun = mid(26)
        val spanA = (tenkan + kijun) / 2.0
        val spanB = mid(52)
        val cloudTop = max(spanA, spanB)
        val cloudBottom = min(spanA, spanB)
        val priceScore = when {
            c[0] > cloudTop -> 80.0
            c[0] < cloudBottom -> 20.0
            else -> 50.0
        }
        val tkScore = when {
            tenkan > kijun -> 70.0
            tenkan < kijun -> 30.0
            else -> 50.0
        }
        val cloudScore = if (spanA > spanB) 65.0 else 35.0
        return (priceScore * .5 + tkScore * .3 + cloudScore * .2).coerceIn(0.0, 100.0)
    }

    private fun newsScore(ticker: String): Int {
        return try {
            val q = URLEncoder.encode("\"$ticker\" stock when:7d", "UTF-8")
            val u = URL("https://news.google.com/rss/search?q=$q&hl=en-US&gl=US&ceid=US:en")
            val con = u.openConnection() as HttpURLConnection
            con.connectTimeout = 3500
            con.readTimeout = 5000
            val body = con.inputStream.bufferedReader().use { it.readText() }
            con.disconnect()
            val positive = listOf("beat","upgrade","buy","bullish","record","strong","surge","contract","partnership","deal","approval","launch","growth","profit","guidance")
            val negative = listOf("miss","downgrade","sell","bearish","lawsuit","investigation","warning","cut guidance","recall","layoff","fraud","delay","loss","decline","plunge","offering","dilution","bankruptcy")
            val titles = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).findAll(body)
                .map { it.groupValues[1].replace("&amp;", "&").lowercase() }.take(8).toList()
            titles.sumOf { title -> 2 * positive.count { title.contains(it) } - 3 * negative.count { title.contains(it) } }.coerceIn(-10, 10)
        } catch (_: Exception) { 0 }
    }

    private fun Double.roundToInt() = kotlin.math.round(this).toInt()
}
