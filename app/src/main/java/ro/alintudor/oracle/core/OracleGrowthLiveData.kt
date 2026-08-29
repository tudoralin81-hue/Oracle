package ro.alintudor.oracle.core

/**
 * Growth live-data enrichment is intentionally disabled for the recommendation cards.
 *
 * Growth is a historical 16:00 snapshot. Recomputing momentum/current price/ADX while
 * the module is open made the visible card change without a new trading-day snapshot.
 * Live market data may be used by other modules, but it must never mutate Growth T0.
 */
object OracleGrowthLiveData {
    fun refresh(items: List<OracleGrowthRecommendation>): List<OracleGrowthRecommendation> = items
}
