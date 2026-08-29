package ro.alintudor.oracle.core

/**
 * One-time migration of the latest Oracle state that was available before the
 * standalone Android app became independent from WordPress.
 *
 * The canonical portfolio seed is the supplied 27.08.2026 activity XLSX.
 * After migration the app remains local and does not contact the web for these records.
 */
object OracleBootstrap {
    private const val VERSION = 2

    fun ensure(repository: OracleRepository) {
        if (repository.bootstrapVersion() >= VERSION) return

        // V2: replace the obsolete demo/mismatched portfolio with the exact
        // three active positions from the supplied Excel export.
        val positions = listOf(
            OraclePosition("CRM", "Salesforce", 4.0, 248.69, 252.05, "USD", status = "ACTIVE"),
            OraclePosition("MELI", "MercadoLibre", 1.0, 1937.20, 1930.75, "USD", status = "ACTIVE"),
            OraclePosition("HOOD", "Robinhood Markets", 10.0, 107.315, 109.76, "USD", status = "ACTIVE")
        )
        repository.savePositions(OracleAnalytics.normalize(positions))

        // The journal rows are copied from the supplied Excel model, including
        // ticker, share count, entry price, Oracle forecast and position ID.
        repository.saveJournal(listOf(
            OracleJournalEntry(1787848564000L, "CRM", "BUY / OPEN", 0.080581688009822, "Deschidere poziție", "ACTIVE", 4.0, 248.69, 0.0, 0.0, 994.76, 0.0, 0.0, "p58_6a904b54d3ce84.44500668"),
            OracleJournalEntry(1787675532000L, "MELI", "BUY / OPEN", 0.16307360057563, "Deschidere poziție", "ACTIVE", 1.0, 1937.20, 0.0, 0.0, 1937.20, 0.0, 0.0, "p58_6a8d995cb69045.35194800"),
            OracleJournalEntry(1787591224000L, "HOOD", "BUY / OPEN", 0.23464565052348, "Deschidere poziție", "ACTIVE", 10.0, 107.315, 0.0, 0.0, 1073.15, 0.0, 0.0, "p58_6a8c884900da03.57898950")
        ))

        repository.saveHistory(listOf(
            OracleHistoryPoint("CRM", 1787848564000L, 252.05, 1008.20, 13.44),
            OracleHistoryPoint("MELI", 1787675532000L, 1930.75, 1930.75, -6.45),
            OracleHistoryPoint("HOOD", 1787591224000L, 109.76, 1097.60, 24.45)
        ))

        repository.markBootstrap(VERSION)
    }
}
