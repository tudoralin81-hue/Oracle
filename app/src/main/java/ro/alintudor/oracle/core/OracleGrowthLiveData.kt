package ro.alintudor.oracle.core

/**
 * Growth is a historical 16:00 snapshot.
 *
 * The bootstrap layer can temporarily contain an invalidated seed while the first
 * local refresh is running. Never expose that seed to the UI: build 439 showed
 * SNPS/VEEV/CRM first and then replaced them a few minutes later when refresh
 * completed. The UI must have one authoritative state per Growth anchor.
 *
 * Live market data therefore never mutates Growth T0 while the module is open.
 */
object OracleGrowthLiveData {
    private fun sectorFor(ticker: String, existing: String): String {
        val t = ticker.trim().uppercase()
        if (existing.isNotBlank() && !existing.equals("US", true)) return existing
        return when (t) {
            "SNPS" -> "Semiconductors / EDA"
            "VEEV" -> "Healthcare / Life Sciences Software"
            "CRM", "NOW", "ORCL" -> "AI / Enterprise Software"
            "ADBE", "SNOW", "DDOG", "NET", "APP", "ROKU", "SPOT", "U", "HUBS", "TEAM", "PAYC", "DOCU", "TWLO" -> "Software"
            "SLB", "HAL", "XOM", "CVX", "COP", "EOG", "OXY", "MPC", "PSX", "VLO", "WMB", "KMI", "OKE", "LNG", "DVN", "FANG", "APA" -> "Energy"
            "NVDA", "AMD", "AVGO", "MU", "QCOM", "INTC", "MRVL", "SMCI", "LRCX", "AMAT", "KLAC", "ARM", "TSM", "ASML", "CDNS" -> "Semiconductors / EDA"
            "PLTR" -> "AI / Enterprise Software"
            "CRWD", "PANW", "FTNT", "ZS", "OKTA" -> "Cybersecurity"
            "MELI", "SHOP", "COIN", "HOOD", "UBER", "ABNB", "EXPE" -> "Fintech / Digital Commerce"
            "LLY", "JNJ", "ABBV", "MRK", "PFE", "BMY", "AMGN", "GILD", "REGN", "ISRG", "ABT", "TMO", "DHR", "BSX", "MDT" -> "Healthcare / Life Sciences"
            else -> existing.ifBlank { "US" }
        }
    }

    fun refresh(items: List<OracleGrowthRecommendation>): List<OracleGrowthRecommendation> {
        if (items.isEmpty()) return emptyList()
        // Invalidated bootstrap snapshots use referenceTimestamp=0.
        if (items.any { it.referenceTimestamp <= 0L }) return emptyList()
        return items.map { item ->
            item.copy(sector = sectorFor(item.ticker, item.sector))
        }
    }
}
