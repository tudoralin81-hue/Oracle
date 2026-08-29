package ro.alintudor.oracle.core

/** Local orchestration layer. */
object OracleLocalProcessor {
    fun refresh(repository: OracleRepository): OracleModuleData {
        OracleBootstrap.ensure(repository)
        val current = repository.snapshot()
        val normalized = OracleAnalytics.normalize(current.positions)
        val now = System.currentTimeMillis()
        val recentHistory = current.history.filter { now - it.timestamp < 30L * 24L * 60L * 60L * 1000L }
        val newPoints = normalized.map { OracleHistoryPoint(it.ticker, now, it.currentPrice, it.marketValue, it.pnl) }
        val history = (recentHistory + newPoints).groupBy { "${it.ticker}:${it.timestamp}" }.values.map { it.first() }.sortedBy { it.timestamp }.takeLast(5000)
        val computedActions = OracleAnalytics.actions(normalized, history).associateBy { it.ticker }
        val actions = normalized.mapNotNull { p -> current.actions.firstOrNull { it.ticker.equals(p.ticker, true) } ?: computedActions[p.ticker] }
        val computedTechnical = OracleTechnicalIndicators.all(history).toMutableMap()
        val marketTickers = (normalized.map { it.ticker } + current.growth.map { it.ticker }).distinct()
        for (ticker in marketTickers) {
            val adx = OracleTechnicalIndicators.adx14(OracleMarketData.fetchDaily(ticker))
            if (adx != null) computedTechnical[ticker]?.let { computedTechnical[ticker] = it.copy(adx = adx) }
        }
        val technical = normalized.mapNotNull { p ->
            val existing = current.technical.firstOrNull { it.ticker.equals(p.ticker, true) }
            val computed = computedTechnical[p.ticker]
            when { existing != null && computed?.adx != null -> existing.copy(adx = computed.adx); existing != null -> existing; else -> computed }
        }
        val generatedGrowth = OracleGrowthEngine.run(current.growth)
        val growth = if (generatedGrowth.isNotEmpty()) generatedGrowth else OracleGrowthLiveData.refresh(current.growth)
        val oldAlerts = current.alerts.filter { it.active }
        val generated = actions.filter { it.action == "BUY" || it.action == "SELL" }.map { OracleAlert(it.ticker, if (it.action == "SELL") "HIGH" else "INFO", "${it.action} signal", "Score ${"%.1f".format(it.score)} — ${it.reason}", now, true) }
        val alertsByTicker = (oldAlerts + generated).groupBy { it.ticker }.mapValues { (_, v) -> v.maxByOrNull { it.timestamp }!! }.values.sortedByDescending { it.timestamp }.take(100)
        val journal = OracleActivityJournal.merge(current.journal, actions)
        repository.savePositions(normalized); repository.saveActions(actions); repository.saveTechnical(technical); repository.saveHistory(history); repository.saveAlerts(alertsByTicker); repository.saveJournal(journal); repository.saveGrowth(growth)
        return repository.snapshot()
    }
}
