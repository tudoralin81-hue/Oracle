package ro.alintudor.oracle

/** Native domain model used by Oracle modules. */
data class OraclePosition(
    val ticker: String,
    val shares: Double,
    val avgCost: Double,
    val currentPrice: Double = 0.0,
    val pnl: Double = 0.0,
    val pnlPct: Double = 0.0,
    val status: String = "ACTIVE"
)

data class OracleAlert(
    val ticker: String,
    val type: String,
    val message: String,
    val timestamp: Long
)

data class OracleNews(
    val ticker: String,
    val title: String,
    val source: String,
    val timestamp: Long,
    val breaking: Boolean = false
)

data class OracleHistoricalPoint(val timestamp: Long, val value: Double)

data class OracleModuleState(
    val positions: List<OraclePosition> = emptyList(),
    val alerts: List<OracleAlert> = emptyList(),
    val news: List<OracleNews> = emptyList(),
    val history: List<OracleHistoricalPoint> = emptyList(),
    val lastSync: Long = 0L
)
