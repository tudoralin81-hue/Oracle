package ro.alintudor.oracle.core

/** Canonical local seed and daily Growth snapshot migration. */
object OracleBootstrap {
    private const val VERSION = 15

    /** Deterministic fallback used only when live OHLCV is unavailable. */
    fun fallbackRiskAllocation(item: OracleGrowthRecommendation): Pair<String, Double> {
        val momentum = (kotlin.math.abs(item.momentum5D) * 0.35 + kotlin.math.abs(item.momentum20D) * 0.25).coerceIn(0.0, 30.0)
        val forecast = item.forecastPct.coerceIn(0.0, 50.0) * 0.30
        val convictionRisk = ((item.score.coerceIn(0, 100) - 70).coerceAtLeast(0) * 0.20)
        val riskScore = (momentum + forecast + convictionRisk).coerceIn(0.0, 100.0)
        val risk = when {
            riskScore >= 35.0 -> "RIDICAT"
            riskScore >= 20.0 -> "MEDIU"
            else -> "SCĂZUT"
        }
        val conviction = item.score.coerceIn(0, 100) / 100.0
        val base = 2.0 + conviction * 6.0
        val riskFactor = when (risk) {
            "RIDICAT" -> 0.55
            "MEDIU" -> 0.78
            else -> 1.0
        }
        val momentumPenalty = when {
            kotlin.math.abs(item.momentum5D) >= 20.0 -> 0.75
            kotlin.math.abs(item.momentum5D) >= 12.0 -> 0.35
            else -> 0.0
        }
        val allocation = (base * riskFactor - momentumPenalty).coerceIn(1.0, 8.0)
        return risk to kotlin.math.round(allocation).toInt().toDouble()
    }

    fun ensure(repository: OracleRepository) {
        if (repository.bootstrapVersion() >= VERSION) return
        val positions = repository.cachedPositions().ifEmpty {
            listOf(
                OraclePosition("CRM", "Salesforce", 4.0, 248.69, 252.05, "USD", status = "ACTIVE"),
                OraclePosition("MELI", "MercadoLibre", 1.0, 1937.20, 1930.75, "USD", status = "ACTIVE"),
                OraclePosition("HOOD", "Robinhood Markets", 10.0, 107.315, 109.76, "USD", status = "ACTIVE")
            )
        }
        repository.savePositions(OracleAnalytics.normalize(positions))
        if (repository.cachedJournal().isEmpty()) repository.saveJournal(emptyList())
        if (repository.cachedHistory().isEmpty()) {
            val now = System.currentTimeMillis()
            repository.saveHistory(positions.map { OracleHistoryPoint(it.ticker, now, it.currentPrice, it.marketValue, it.pnl) })
        }

        val t0 = System.currentTimeMillis()
        fun recommendation(ticker: String, fallback: OracleGrowthRecommendation): OracleGrowthRecommendation {
            val a = runCatching { OracleAnalysisEngine.analyze(ticker) }.getOrNull()
            val (risk, allocation) = if (a != null) a.risk to a.allocation else fallbackRiskAllocation(fallback)
            return fallback.copy(risk = risk, allocationMax = allocation, referenceTimestamp = t0, generatedAt = t0)
        }

        val snps = recommendation("SNPS", OracleGrowthRecommendation(
            horizon="SHORT", ticker="SNPS", company="Synopsys, Inc.", sector="Technology",
            score=86, signal="STRONG BUY", risk="NEEVALUAT", allocationMax=0.0,
            forecastPct=6.1, momentum5D=16.8, momentum20D=24.9,
            weights=listOf(21,18,12,16,12,8,3,4,2,2,1,1), referenceTimestamp=t0, generatedAt=t0
        ))
        val veev = recommendation("VEEV", OracleGrowthRecommendation(
            horizon="MEDIUM", ticker="VEEV", company="Veeva Systems Inc.", sector="Technology",
            score=85, signal="STRONG BUY", risk="NEEVALUAT", allocationMax=0.0,
            forecastPct=18.2, momentum5D=12.6, momentum20D=40.0,
            weights=listOf(12,12,16,12,9,9,9,5,6,5,4,1),
            newsTitle="Why Veeva Systems (VEEV) Stock Is Trading Up Today - StockStory", newsSource="StockStory",
            referenceTimestamp=t0, generatedAt=t0
        ))
        val crm = recommendation("CRM", OracleGrowthRecommendation(
            horizon="LONG", ticker="CRM", company="Salesforce, Inc.", sector="Technology",
            score=81, signal="BUY", risk="NEEVALUAT", allocationMax=0.0,
            forecastPct=33.1, momentum5D=22.7, momentum20D=39.5,
            weights=listOf(6,6,20,7,5,8,18,4,9,7,9,2),
            newsTitle="Salesforce stock jumps 18% on AI growth and Anthropic investment gain - CNBC", newsSource="CNBC",
            referenceTimestamp=t0, generatedAt=t0
        ))
        repository.saveGrowth(listOf(snps, veev, crm))
        repository.markBootstrap(VERSION)
    }
}
