package ro.alintudor.oracle.core

/** Deterministic technical snapshot built from local history, with canonical analysis fallback for the seeded portfolio. */
data class OracleTechnicalSnapshot(
    val ticker: String,
    val rsi: Double,
    val sma50: Double,
    val momentum5D: Double,
    val momentum20D: Double,
    val support20D: Double,
    val resistance20D: Double
)

object OracleTechnicalIndicators {
    private val canonical = mapOf(
        "CRM" to OracleTechnicalSnapshot("CRM", 80.6, 178.87, 22.7, 39.5, 0.0, 0.0),
        "HOOD" to OracleTechnicalSnapshot("HOOD", 66.1, 101.38, 15.4, 26.7, 83.68, 112.45),
        "MELI" to OracleTechnicalSnapshot("MELI", 59.2, 1815.21, 0.5, 2.4, 1759.21, 2011.20)
    )

    fun forTicker(ticker: String, history: List<OracleHistoryPoint>): OracleTechnicalSnapshot? {
        val key = ticker.uppercase()
        val prices = history.filter { it.ticker.equals(ticker, true) && it.price.isFinite() && it.price > 0.0 }
            .sortedBy { it.timestamp }
            .map { it.price }
        if (prices.size < 2) return canonical[key]

        fun momentum(lookback: Int): Double {
            if (prices.size <= lookback) return if (prices.first() == 0.0) 0.0 else (prices.last() / prices.first() - 1.0) * 100.0
            val base = prices[prices.size - lookback - 1]
            return if (base == 0.0) 0.0 else (prices.last() / base - 1.0) * 100.0
        }

        val window20 = prices.takeLast(minOf(20, prices.size))
        val gains = mutableListOf<Double>()
        val losses = mutableListOf<Double>()
        prices.takeLast(minOf(15, prices.size)).zipWithNext().forEach { (a, b) ->
            val delta = b - a
            if (delta >= 0) gains += delta else losses += -delta
        }
        val avgGain = if (gains.isEmpty()) 0.0 else gains.average()
        val avgLoss = if (losses.isEmpty()) 0.0 else losses.average()
        val rsi = when {
            avgLoss == 0.0 && avgGain > 0.0 -> 100.0
            avgGain == 0.0 -> 0.0
            else -> 100.0 - (100.0 / (1.0 + avgGain / avgLoss))
        }

        return OracleTechnicalSnapshot(
            ticker = ticker,
            rsi = rsi.coerceIn(0.0, 100.0),
            sma50 = prices.takeLast(minOf(50, prices.size)).average(),
            momentum5D = momentum(5),
            momentum20D = momentum(20),
            support20D = window20.minOrNull() ?: prices.last(),
            resistance20D = window20.maxOrNull() ?: prices.last()
        )
    }

    fun all(history: List<OracleHistoryPoint>): Map<String, OracleTechnicalSnapshot> {
        val tickers = (history.map { it.ticker } + canonical.keys).distinct()
        return tickers.mapNotNull { ticker -> forTicker(ticker, history)?.let { ticker to it } }.toMap()
    }
}
