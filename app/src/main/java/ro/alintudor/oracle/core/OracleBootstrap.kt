package ro.alintudor.oracle.core

/**
 * One-time migration of the latest Oracle state that was available before the
 * standalone Android app became independent from WordPress.
 *
 * The canonical portfolio seed is the supplied 27.08.2026 activity XLSX.
 * After migration the app remains local and does not contact the web for these records.
 */
object OracleBootstrap {
    private const val VERSION = 9

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

        if (repository.cachedJournal().isEmpty()) {
            repository.saveJournal(listOf(
                OracleJournalEntry(1787841364000L, "CRM", "BUY / OPEN", 0.080581688009822, "Deschidere poziție", "ACTIVE", 4.0, 248.69, 0.0, 0.0, 994.76, 0.0, 0.0, "p58_6a904b54d3ce84.44500668"),
                OracleJournalEntry(1787664732000L, "MELI", "BUY / OPEN", 0.16307360057563, "Deschidere poziție", "ACTIVE", 1.0, 1937.20, 0.0, 0.0, 1937.20, 0.0, 0.0, "p58_6a8d995cb69045.35194800"),
                OracleJournalEntry(1787594825000L, "HOOD", "BUY / OPEN", 0.23464565052348, "Deschidere poziție", "ACTIVE", 10.0, 107.315, 0.0, 0.0, 1073.15, 0.0, 0.0, "p58_6a8c884900da03.57898950")
            ))
        }

        if (repository.cachedHistory().isEmpty()) {
            val now = System.currentTimeMillis()
            repository.saveHistory(positions.map { OracleHistoryPoint(it.ticker, now, it.currentPrice, it.marketValue, it.pnl) })
        }

        repository.saveTechnical(listOf(
            OracleTechnicalSnapshot("CRM", 80.6, 178.87, 22.7, 39.5, 0.0, 0.0),
            OracleTechnicalSnapshot("HOOD", 66.1, 101.38, 15.4, 26.7, 83.68, 112.45),
            OracleTechnicalSnapshot("MELI", 59.2, 1815.21, 0.5, 2.4, 1759.21, 2011.20)
        ))

        repository.saveActions(listOf(
            OracleAction("CRM", "HOLD", 82.0, "supraîncălzire RSI · trend și momentum încă acceptabile", System.currentTimeMillis()),
            OracleAction("HOOD", "HOLD", 95.0, "trend și momentum încă acceptabile", System.currentTimeMillis()),
            OracleAction("MELI", "HOLD", 95.0, "trend și momentum încă acceptabile", System.currentTimeMillis())
        ))

        // Canonical Growth snapshot for the Friday 28.08.2026 16:00
        // Europe/Bucharest trading-day anchor. Saturday 29.08.2026 is not a
        // trading day, so the active weekend snapshot must remain this Friday T0.
        val cachedGrowth = repository.cachedGrowth()
        val t0 = 1787922000000L // 28.08.2026 16:00 Europe/Bucharest

        // Allocation is deliberately NOT taken from the historical snapshot.
        // Calculate it independently for each ticker using the same single-ticker
        // Oracle Analysis allocation formula. This prevents one hard-coded 3.0%
        // value from being propagated to every Growth recommendation.
        fun calculatedAllocation(ticker: String, fallback: Double): Double =
            OracleAnalysisEngine.analyze(ticker)?.allocation ?: fallback

        val snpsAllocation = calculatedAllocation("SNPS", 3.0)
        val veevAllocation = calculatedAllocation("VEEV", 3.0)
        val crmAllocation = calculatedAllocation("CRM", 3.0)

        val canonicalGrowthFallback = listOf(
            OracleGrowthRecommendation(
                horizon="SHORT", ticker="SNPS", company="Synopsys, Inc.", sector="Technology",
                score=86, signal="STRONG BUY", risk="RIDICAT", allocationMax=snpsAllocation, forecastPct=6.1,
                momentum5D=16.8, momentum20D=24.9,
                weights=listOf(21,18,12,16,12,8,3,4,2,2,1,1),
                newsTitle="", newsSource="", referenceTimestamp=t0, generatedAt=t0
            ),
            OracleGrowthRecommendation(
                horizon="MEDIUM", ticker="VEEV", company="Veeva Systems Inc.", sector="Technology",
                score=85, signal="STRONG BUY", risk="RIDICAT", allocationMax=veevAllocation, forecastPct=18.2,
                momentum5D=12.6, momentum20D=40.0,
                weights=listOf(12,12,16,12,9,9,9,5,6,5,4,1),
                newsTitle="Why Veeva Systems (VEEV) Stock Is Trading Up Today - StockStory", newsSource="StockStory", referenceTimestamp=t0, generatedAt=t0
            ),
            OracleGrowthRecommendation(
                horizon="LONG", ticker="CRM", company="Salesforce, Inc.", sector="Technology",
                score=81, signal="BUY", risk="RIDICAT", allocationMax=crmAllocation, forecastPct=33.1,
                momentum5D=22.7, momentum20D=39.5,
                weights=listOf(6,6,20,7,5,8,18,4,9,7,9,2),
                newsTitle="Salesforce stock jumps 18% on AI growth and Anthropic investment gain - CNBC", newsSource="CNBC", referenceTimestamp=t0, generatedAt=t0
            )
        )

        // V5 cached Growth contained the previous VEEV/CRM/CRWD recommendation set.
        // Replace known legacy data and, for the canonical 28.08.2026 16:00 anchor,
        // restore the approved snapshot while calculating allocation per ticker.
        val legacyV5 = cachedGrowth.map { it.ticker.uppercase() }.toSet() == setOf("VEEV", "CRM", "CRWD")
        val sameSession = cachedGrowth.any { it.referenceTimestamp == t0 }
        if (cachedGrowth.isEmpty() || legacyV5 || sameSession) {
            repository.saveGrowth(canonicalGrowthFallback)
        } else {
            // Preserve newer snapshots, but migrate the known legacy 101-point LONG profile.
            val migrated = cachedGrowth.map { item ->
                if (item.horizon.equals("LONG", true) && item.weights.size >= 12 && item.weights.sum() == 101 && item.weights[3] == 7) {
                    item.copy(weights = item.weights.toMutableList().also { it[3] = 6 })
                } else item
            }
            if (migrated != cachedGrowth) repository.saveGrowth(migrated)
        }

        repository.markBootstrap(VERSION)
    }
}