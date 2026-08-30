package ro.alintudor.oracle.core

/** Canonical local seed and daily Growth snapshot migration. */
object OracleBootstrap {
    private const val VERSION = 11

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

        val cachedGrowth = repository.cachedGrowth()
        val t0 = System.currentTimeMillis()

        // IMPORTANT: the daily frozen snapshot is produced from the live engine.
        // Risk and allocation are never taken from the old hard-coded 3%/HIGH values.
        fun recommendation(ticker: String, fallback: OracleGrowthRecommendation): OracleGrowthRecommendation {
            val a = OracleAnalysisEngine.analyze(ticker)
            return if (a != null) fallback.copy(
                risk = a.risk,
                allocationMax = a.allocation,
                referenceTimestamp = t0,
                generatedAt = t0
            ) else fallback.copy(
                risk = "NEEVALUAT",
                allocationMax = 0.0,
                referenceTimestamp = t0,
                generatedAt = t0
            )
        }

        val snps = recommendation("SNPS", OracleGrowthRecommendation("SHORT","SNPS","Synopsys, Inc.","Technology",86,"STRONG BUY","NEEVALUAT",0.0,6.1,16.8,24.9,listOf(21,18,12,16,12,8,3,4,2,2,1,1),"","",t0,t0))
        val veev = recommendation("VEEV", OracleGrowthRecommendation("MEDIUM","VEEV","Veeva Systems Inc.","Technology",85,"STRONG BUY","NEEVALUAT",0.0,18.2,12.6,40.0,listOf(12,12,16,12,9,9,9,5,6,5,4,1),"Why Veeva Systems (VEEV) Stock Is Trading Up Today - StockStory","StockStory",t0,t0))
        val crm = recommendation("CRM", OracleGrowthRecommendation("LONG","CRM","Salesforce, Inc.","Technology",81,"BUY","NEEVALUAT",0.0,33.1,22.7,39.5,listOf(6,6,20,7,5,8,18,4,9,7,9,2),"Salesforce stock jumps 18% on AI growth and Anthropic investment gain - CNBC","CNBC",t0,t0))

        val canonical = listOf(snps, veev, crm)
        val oldTickers = cachedGrowth.map { it.ticker.uppercase() }.toSet()
        val oldFrozenValues = cachedGrowth.any { it.allocationMax == 3.0 || it.risk == "RIDICAT" }
        if (cachedGrowth.isEmpty() || oldTickers != setOf("SNPS", "VEEV", "CRM") || oldFrozenValues) {
            repository.saveGrowth(canonical)
        }
        repository.markBootstrap(VERSION)
    }
}
