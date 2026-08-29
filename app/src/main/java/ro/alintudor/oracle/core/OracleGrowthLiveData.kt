package ro.alintudor.oracle.core

/**
 * Live market-data enrichment for the persisted Growth snapshots.
 *
 * Oracle's score, forecast, risk, horizon weights and catalyst fields remain
 * authoritative snapshot values; this class only refreshes fields that can be
 * derived directly and reproducibly from live OHLCV data.
 */
object OracleGrowthLiveData {
    fun refresh(items: List<OracleGrowthRecommendation>): List<OracleGrowthRecommendation> =
        items.map { item ->
            val candles = OracleMarketData.fetchDaily(item.ticker, range = "6mo")
            if (candles.isEmpty()) return@map item

            val closes = candles.map { it.close }.filter { it.isFinite() && it > 0.0 }
            if (closes.isEmpty()) return@map item

            val current = closes.last()
            fun momentum(days: Int): Double? {
                if (closes.size <= days) return null
                val base = closes[closes.size - days - 1]
                return if (base > 0.0) (current / base - 1.0) * 100.0 else null
            }

            val m5 = momentum(5) ?: item.momentum5D
            val m20 = momentum(20) ?: item.momentum20D
            val adx = OracleTechnicalIndicators.adx14(candles)
            val actual = item.referencePrice?.let { ref ->
                if (ref > 0.0) (current / ref - 1.0) * 100.0 else null
            }

            item.copy(
                momentum5D = m5,
                momentum20D = m20,
                currentPrice = current,
                currentActualPct = actual,
                adx = adx
            )
        }
}
