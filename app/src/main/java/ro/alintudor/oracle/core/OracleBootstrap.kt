package ro.alintudor.oracle.core

/**
 * One-time migration of the latest Oracle state that was available before the
 * standalone Android app became independent from WordPress.
 *
 * The canonical portfolio seed is the supplied 27.08.2026 activity XLSX.
 * After migration the app remains local and does not contact the web for these records.
 */
object OracleBootstrap {
    private const val VERSION = 3

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

        // Canonical analysis snapshot from the working Oracle analysis screen.
        // Portfolio must consume this snapshot instead of inventing RSI=0 / momentum=0
        // from a one-point local history.
        repository.saveTechnical(listOf(
            OracleTechnicalSnapshot("CRM", 80.6, 178.87, 22.7, 39.5, 0.0, 0.0),
            OracleTechnicalSnapshot("HOOD", 66.1, 101.38, 15.4, 26.7, 83.68, 112.45),
            OracleTechnicalSnapshot("MELI", 59.2, 1815.21, 0.5, 2.4, 1759.21, 2011.20)
        ))

        // Canonical decisions shown by the working analysis screen.
        repository.saveActions(listOf(
            OracleAction("CRM", "HOLD", 82.0, "supraîncălzire RSI · trend și momentum încă acceptabile", System.currentTimeMillis()),
            OracleAction("HOOD", "HOLD", 95.0, "trend și momentum încă acceptabile", System.currentTimeMillis()),
            OracleAction("MELI", "HOLD", 95.0, "trend și momentum încă acceptabile", System.currentTimeMillis())
        ))

        repository.markBootstrap(VERSION)
    }
}
