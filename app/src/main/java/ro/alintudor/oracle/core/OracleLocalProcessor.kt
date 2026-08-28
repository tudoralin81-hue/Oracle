package ro.alintudor.oracle.core

/**
 * Local orchestration layer. Refreshes the complete Oracle state without any
 * network dependency: normalize positions, record a price snapshot, calculate
 * actions, and maintain the alert journal.
 */
object OracleLocalProcessor {
    fun refresh(repository: OracleRepository): OracleModuleData {
        val current = repository.snapshot()
        val normalized = OracleAnalytics.normalize(current.positions)
        val actions = OracleAnalytics.actions(normalized, current.history)
        val now = System.currentTimeMillis()

        val recentHistory = current.history.filter { now - it.timestamp < 30L * 24L * 60L * 60L * 1000L }
        val newPoints = normalized.map {
            OracleHistoryPoint(it.ticker, now, it.currentPrice, it.marketValue, it.pnl)
        }
        val history = (recentHistory + newPoints)
            .groupBy { "${it.ticker}:${it.timestamp}" }
            .values.map { it.first() }
            .sortedBy { it.timestamp }
            .takeLast(5000)

        val oldAlerts = current.alerts.filter { it.active }
        val generated = actions.filter { it.action == "BUY" || it.action == "SELL" }.map {
            OracleAlert(
                ticker = it.ticker,
                level = if (it.action == "SELL") "HIGH" else "INFO",
                title = "${it.action} signal",
                message = "Score ${"%.1f".format(it.score)} — ${it.reason}",
                timestamp = now,
                active = true
            )
        }
        val alertsByTicker = (oldAlerts + generated)
            .groupBy { it.ticker }
            .mapValues { (_, values) -> values.maxByOrNull { it.timestamp }!! }
            .values.sortedByDescending { it.timestamp }
            .take(100)

        repository.savePositions(normalized)
        repository.saveActions(actions)
        repository.saveHistory(history)
        repository.saveAlerts(alertsByTicker)

        return repository.snapshot()
    }
}
