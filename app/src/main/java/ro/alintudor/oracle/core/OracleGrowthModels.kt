package ro.alintudor.oracle.core

/** Oracle Growth snapshot. Android displays persisted Oracle values and never recalculates Growth formulas. */
data class OracleGrowthRecommendation(
    val horizon: String,
    val ticker: String,
    val company: String,
    val sector: String,
    val score: Int,
    val signal: String,
    val risk: String,
    val allocationMax: Double,
    val forecastPct: Double,
    val momentum5D: Double,
    val momentum20D: Double,
    val weights: List<Int> = emptyList(),
    val newsTitle: String = "",
    val newsSource: String = "",
    val referenceTimestamp: Long = 0L,
    val currentActualPct: Double? = null,
    val referencePrice: Double? = null,
    val currentPrice: Double? = null,
    /** Live technical value sourced from OHLCV; separate from the ADX weight. */
    val adx: Double? = null
)
