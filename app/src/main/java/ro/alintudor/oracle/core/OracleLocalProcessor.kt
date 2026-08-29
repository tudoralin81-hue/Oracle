package ro.alintudor.oracle.core

/**
 * Local orchestration layer. Refreshes the complete Oracle state without any
 * network dependency: normalize positions, record a price snapshot, calculate
 * only missing actions/technical data, and maintain the alert journal.
 *
 * Existing canonical analysis decisions are preserved. The portfolio must not
 * replace a valid Oracle HOLD/BUY/SELL with a synthetic decision caused by an
 * incomplete one-point local history.
 */
object OracleLocalProcessor {
    fun refresh(repository: OracleRepository): OracleModuleData {
        OracleBootstrap.ensure(repository)
        val current = repository.snapshot()
        val normalized = OracleAnalytics.normalize(current.positions)
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

        val computedActions = OracleAnalytics.actions(normalized, history).associateBy { it.ticker }
        val actions = normalized.mapNotNull { p ->
            current.actions.firstOrNull { it.ticker.equals(p.ticker, true) }
                ?: computedActions[p.ticker]
        }

        val computedTechnical = OracleTechnicalIndicators.all(history)
        val technical = normalized.mapNotNull { p ->
            current.technical.firstOrNull { it.ticker.equals(p.ticker, true) }
                ?: computedTechnical[p.ticker]
        }

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

        val journal = OracleActivityJournal.merge(current.journal, actions)
        repository.savePositions(normalized)
        repository.saveActions(actions)
        repository.saveTechnical(technical)
        repository.saveHistory(history)
        repository.saveAlerts(alertsByTicker)
        repository.saveJournal(journal)

        return repository.snapshot()
    }
}
