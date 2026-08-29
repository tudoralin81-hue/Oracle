package ro.alintudor.oracle.core

import kotlin.math.max

/** Deterministic technical snapshot built only from the local price history. */
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
    fun forTicker(ticker: String, history: List<OracleHistoryPoint>): OracleTechnicalSnapshot? {
        val prices = history.filter { it.ticker.equals(ticker, true) && it.price.isFinite() && it.price > 0.0 }
            .sortedBy { it.timestamp }
            .map { it.price }
        if (prices.isEmpty()) return null

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

    fun all(history: List<OracleHistoryPoint>): Map<String, OracleTechnicalSnapshot> =
        history.map { it.ticker }.distinct().associateWith { forTicker(it, history)!! }
}
