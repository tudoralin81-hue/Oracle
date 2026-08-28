package ro.alintudor.oracle.core

data class OraclePortfolioSummary(
    val value: Double,
    val pnl: Double,
    val pnlPercent: Double,
    val riskScore: Double,
    val winners: Int,
    val losers: Int,
    val alerts: Int
)

object OraclePortfolioSummaryBuilder {
    fun build(positions: List<OraclePosition>, alerts: List<OracleAlert>): OraclePortfolioSummary {
        val invested = positions.sumOf { it.shares * it.avgCost }
        val pnl = positions.sumOf { it.pnl }
        val value = positions.sumOf { it.marketValue }
        val risk = if (positions.isEmpty() || value == 0.0) 0.0 else positions.maxOf { it.marketValue / value * 100.0 }
        return OraclePortfolioSummary(value,pnl,if(invested==0.0)0.0 else pnl/invested*100.0, risk,
            positions.count { it.pnl > 0 }, positions.count { it.pnl < 0 }, alerts.count { it.active })
    }
}
