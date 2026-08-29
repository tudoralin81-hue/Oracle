package ro.alintudor.oracle.core

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/** Local orchestration layer. */
object OracleLocalProcessor {
    private val BUCHAREST = ZoneId.of("Europe/Bucharest")

    private fun currentGrowthAnchor(nowMillis: Long): Long {
        val z = Instant.ofEpochMilli(nowMillis).atZone(BUCHAREST)
        var date = if (z.hour < 16) z.toLocalDate().minusDays(1) else z.toLocalDate()
        while (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) date = if (date.dayOfWeek == DayOfWeek.SATURDAY) date.minusDays(1) else date.minusDays(2)
        return ZonedDateTime.of(date, java.time.LocalTime.of(16, 0), BUCHAREST).toInstant().toEpochMilli()
    }

    private fun normalizeGrowthSnapshot(items: List<OracleGrowthRecommendation>, anchor: Long) = items.map { it.copy(referenceTimestamp = anchor, generatedAt = anchor) }

    private fun newsKey(n: OracleNews): String {
        val url = n.url.trim().lowercase(Locale.US).substringBefore("?").removeSuffix("/")
        if (url.isNotBlank()) return "url:$url"
        return "title:" + n.title.trim().lowercase(Locale.US).replace(Regex("\\s+"), " ").replace(Regex("[^a-z0-9 ]"), "")
    }

    private fun mergeNews(fetched: List<OracleNews>, cached: List<OracleNews>): List<OracleNews> =
        (fetched + cached).filter { it.title.isNotBlank() }.groupBy(::newsKey)
            .values.map { group -> group.maxByOrNull { it.receivedAt.coerceAtLeast(it.publishedAt) }!! }
            .sortedWith(compareByDescending<OracleNews> { it.breaking }.thenByDescending { it.publishedAt }).take(250)

    fun refresh(repository: OracleRepository): OracleModuleData {
        OracleBootstrap.ensure(repository); val current = repository.snapshot(); val normalized = OracleAnalytics.normalize(current.positions); val now = System.currentTimeMillis()
        val recentHistory = current.history.filter { now - it.timestamp < 30L * 24L * 60L * 60L * 1000L }
        val newPoints = normalized.map { OracleHistoryPoint(it.ticker, now, it.currentPrice, it.marketValue, it.pnl) }
        val history = (recentHistory + newPoints).groupBy { "${it.ticker}:${it.timestamp}" }.values.map { it.first() }.sortedBy { it.timestamp }.takeLast(5000)
        val computedActions = OracleAnalytics.actions(normalized, history).associateBy { it.ticker }
        val actions = normalized.mapNotNull { p -> current.actions.firstOrNull { it.ticker.equals(p.ticker, true) } ?: computedActions[p.ticker] }
        val computedTechnical = OracleTechnicalIndicators.all(history).toMutableMap()
        val marketTickers = (normalized.map { it.ticker } + current.growth.map { it.ticker }).distinct()
        for (ticker in marketTickers) OracleTechnicalIndicators.adx14(OracleMarketData.fetchDaily(ticker))?.let { adx -> computedTechnical[ticker]?.let { computedTechnical[ticker] = it.copy(adx = adx) } }
        val technical = normalized.mapNotNull { p -> val existing=current.technical.firstOrNull{it.ticker.equals(p.ticker,true)}; val computed=computedTechnical[p.ticker]; when { existing!=null&&computed?.adx!=null->existing.copy(adx=computed.adx); existing!=null->existing; else->computed } }
        val growthAnchor=currentGrowthAnchor(now); val localDay=Instant.ofEpochMilli(now).atZone(BUCHAREST).dayOfWeek; val weekend=localDay==DayOfWeek.SATURDAY||localDay==DayOfWeek.SUNDAY
        val snapshotIsCurrent=current.growth.isNotEmpty()&&(current.growth.all{it.referenceTimestamp==growthAnchor}||weekend)
        val growth=if(snapshotIsCurrent)current.growth else { val generated=OracleGrowthEngine.run(current.growth); if(generated.isNotEmpty())normalizeGrowthSnapshot(generated,growthAnchor) else current.growth }
        val oldAlerts=current.alerts.filter{it.active}; val generated=actions.filter{it.action=="BUY"||it.action=="SELL"}.map{OracleAlert(it.ticker,if(it.action=="SELL")"HIGH"else"INFO","${it.action} signal","Score ${"%.1f".format(it.score)} — ${it.reason}",now,true)}
        val alertsByTicker=(oldAlerts+generated).groupBy{it.ticker}.mapValues{(_,v)->v.maxByOrNull{it.timestamp}!!}.values.sortedByDescending{it.timestamp}.take(100)
        val journal=OracleActivityJournal.merge(current.journal,actions)
        val fetchedNews=runCatching{OracleNewsFetcher.fetch(150)}.getOrDefault(emptyList()); val news=if(fetchedNews.isNotEmpty())mergeNews(fetchedNews,current.news)else current.news.distinctBy(::newsKey)
        repository.saveNews(news); repository.savePositions(normalized); repository.saveActions(actions); repository.saveTechnical(technical); repository.saveHistory(history); repository.saveAlerts(alertsByTicker); repository.saveJournal(journal); repository.saveGrowth(growth)
        return repository.snapshot().copy(news=news)
    }
}
