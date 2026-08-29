package ro.alintudor.oracle.core

/**
 * One-time migration of the latest Oracle state that was available before the
 * standalone Android app became independent from WordPress.
 *
 * The canonical portfolio seed is the supplied 27.08.2026 activity XLSX.
 * After migration the app remains local and does not contact the web for these records.
 */
object OracleBootstrap {
    private const val VERSION = 5

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

        // Growth reference snapshot supplied from the WordPress UI screenshots.
        // These are cached Oracle values; Android does not invent or recalculate Oracle formulas.
        val cachedGrowth = repository.cachedGrowth()
        if (cachedGrowth.isEmpty()) {
            val t0 = 1788008400000L // 29.08.2026 16:00 Europe/Bucharest
            repository.saveGrowth(listOf(
                OracleGrowthRecommendation(
                    horizon="SHORT", ticker="VEEV", company="Veeva Systems Inc.", sector="Technology",
                    score=97, signal="STRONG BUY", risk="RIDICAT", allocationMax=3.0, forecastPct=8.1,
                    momentum5D=12.6, momentum20D=40.0,
                    weights=listOf(22,18,12,16,12,8,3,4,2,2,1,0),
                    newsTitle="Why Veeva Systems (VEEV) Stock Is Trading Up Today - StockStory", newsSource="StockStory", referenceTimestamp=t0
                ),
                OracleGrowthRecommendation(
                    horizon="MEDIUM", ticker="CRM", company="Salesforce, Inc.", sector="Technology",
                    score=91, signal="STRONG BUY", risk="RIDICAT", allocationMax=3.0, forecastPct=18.6,
                    momentum5D=22.7, momentum20D=39.5,
                    weights=listOf(12,12,16,12,9,9,9,5,6,5,4,1),
                    newsTitle="Salesforce stock jumps 18% on AI growth and Anthropic investment gain - CNBC", newsSource="CNBC", referenceTimestamp=t0
                ),
                OracleGrowthRecommendation(
                    horizon="LONG", ticker="CRWD", company="CrowdStrike Holdings, Inc.", sector="Technology",
                    score=82, signal="BUY", risk="RIDICAT", allocationMax=3.0, forecastPct=40.1,
                    momentum5D=19.8, momentum20D=23.1,
                    // V5 correction: LONG Momentum 7 -> 6, bringing the official profile to 100.
                    weights=listOf(6,6,20,6,5,8,18,4,9,7,9,2),
                    newsTitle="CrowdStrike jumps 11% on record second quarter as 'Mythos moment' drives AI cyber wave - CNBC", newsSource="CNBC", referenceTimestamp=t0
                )
            ))
        } else {
            // Migrate only the known pre-V5 LONG seed (sum 101) to the corrected 100-point profile.
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