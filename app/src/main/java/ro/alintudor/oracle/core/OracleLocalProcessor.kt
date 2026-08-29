package ro.alintudor.oracle.core

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Local orchestration layer. */
object OracleLocalProcessor {
    private val BUCHAREST = ZoneId.of("Europe/Bucharest")

    /**
     * Growth is a market-session snapshot, not a live ranking.
     * The snapshot is created exactly at 16:00 Europe/Bucharest for a trading day
     * and remains authoritative until the next trading-day 16:00 anchor.
     * Saturday/Sunday (and Monday before 16:00) therefore continue to use Friday's snapshot.
     */
    private fun growthSnapshotAnchor(nowMillis: Long): Long {
        var z = Instant.ofEpochMilli(nowMillis).atZone(BUCHAREST)
        var date = if (z.hour < 16) z.toLocalDate().minusDays(1) else z.toLocalDate()

        while (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
            date = date.minusDays(1)
        }

        return ZonedDateTime.of(date, java.time.LocalTime.of(16, 0), BUCHAREST)
            .toInstant().toEpochMilli()
    }

    private fun normalizeGrowthSnapshot(items: List<OracleGrowthRecommendation>, anchor: Long): List<OracleGrowthRecommendation> =
        items.map { it.copy(referenceTimestamp = anchor, generatedAt = anchor) }

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

        // IMPORTANT: Growth recommendations are frozen for the whole 16:00→next-trading-day-16:00 session.
        // Opening a module and pressing Refresh both pass through here, but neither may recompute the
        // ranking while the current persisted snapshot still belongs to the current anchor.
        val growthAnchor = growthSnapshotAnchor(now)
        val snapshotIsCurrent = current.growth.isNotEmpty() &&
            current.growth.all { it.referenceTimestamp == growthAnchor }
        val growth = if (snapshotIsCurrent) {
            current.growth
        } else {
            val generated = OracleGrowthEngine.run(current.growth)
            if (generated.isNotEmpty()) normalizeGrowthSnapshot(generated, growthAnchor)
            else current.growth
        }

        val oldAlerts = current.alerts.filter { it.active }
        val generated = actions.filter { it.action == "BUY" || it.action == "SELL" }.map { OracleAlert(it.ticker, if (it.action == "SELL") "HIGH" else "INFO", "${it.action} signal", "Score ${"%.1f".format(it.score)} — ${it.reason}", now, true) }
        val alertsByTicker = (oldAlerts + generated).groupBy { it.ticker }.mapValues { (_, v) -> v.maxByOrNull { it.timestamp }!! }.values.sortedByDescending { it.timestamp }.take(100)
        val journal = OracleActivityJournal.merge(current.journal, actions)
        repository.savePositions(normalized); repository.saveActions(actions); repository.saveTechnical(technical); repository.saveHistory(history); repository.saveAlerts(alertsByTicker); repository.saveJournal(journal); repository.saveGrowth(growth)
        return repository.snapshot()
    }
}
