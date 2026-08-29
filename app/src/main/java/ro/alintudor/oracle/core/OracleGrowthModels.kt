package ro.alintudor.oracle.core

/** Cached Oracle Growth snapshot. Android displays Oracle values and does not recalculate Oracle formulas. */
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
    val currentPrice: Double? = null
)
