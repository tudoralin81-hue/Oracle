package ro.alintudor.oracle.core

/**
 * One-time migration of the latest Oracle state that was available before the
 * standalone Android app became independent from WordPress.
 *
 * This is deliberately local: after migration the app never contacts the web
 * to obtain these records. The values below come from the 27.08.2026 Oracle
 * activity export (V5.8.17).
 */
object OracleBootstrap {
    private const val VERSION = 1

    fun ensure(repository: OracleRepository) {
        if (repository.bootstrapVersion() >= VERSION) return
        if (repository.cachedPositions().isNotEmpty()) {
            repository.markBootstrap(VERSION)
            return
        }

        val positions = listOf(
            OraclePosition("HOOD", "Robinhood Markets", 5.0, 107.32, 112.45, "USD", status = "ACTIVE"),
            OraclePosition("NVDA", "NVIDIA", 3.0, 126.50, 131.75, "USD", status = "ACTIVE"),
            OraclePosition("CRM", "Salesforce", 4.0, 248.69, 248.69, "USD", status = "ACTIVE")
        )
        repository.savePositions(OracleAnalytics.normalize(positions))

        // Two price observations where the activity export provides them.
        repository.saveHistory(listOf(
            OracleHistoryPoint("HOOD", 1787594825000L, 107.32, 1073.20, 0.0),
            OracleHistoryPoint("HOOD", 1787830401000L, 112.45, 1124.50, 51.30),
            OracleHistoryPoint("NVDA", 1787753433000L, 126.50, 632.50, 0.0),
            OracleHistoryPoint("NVDA", 1787838009000L, 131.75, 658.75, 26.25),
            OracleHistoryPoint("CRM", 1787815365000L, 248.69, 994.76, 0.0)
        ))

        repository.saveJournal(listOf(
            OracleJournalEntry(1787594825000L, "HOOD", "BUY / OPEN", 23.5, "Deschidere poziție", "ACTIVE", 10.0, 107.32, 0.0, 0.0, 1073.20, 0.0, 0.0, "p58_6a8c884900da"),
            OracleJournalEntry(1787664732000L, "MELI", "BUY / OPEN", 16.3, "Deschidere poziție", "CLOSED", 1.0, 1937.20, 0.0, 0.0, 1937.20, 0.0, 0.0, "p58_6a8d995cb690"),
            OracleJournalEntry(1787753433000L, "NVDA", "BUY / OPEN", 18.7, "Deschidere poziție", "ACTIVE", 5.0, 126.50, 0.0, 0.0, 632.50, 0.0, 0.0, "p58_6a8f1c1e3ab2"),
            OracleJournalEntry(1787815365000L, "CRM", "BUY / OPEN", 8.1, "Deschidere poziție", "ACTIVE", 4.0, 248.69, 0.0, 0.0, 994.76, 0.0, 0.0, "p58_6a904b54d3ce"),
            OracleJournalEntry(1787830401000L, "HOOD", "SELL (PARTIAL)", 23.5, "Vânzare parțială", "ACTIVE", 5.0, 107.32, 112.45, 50.0, 536.60, 562.25, 25.65, "p58_6a8c884900da"),
            OracleJournalEntry(1787832318000L, "MELI", "SELL (FULL)", 16.3, "Închidere poziție", "CLOSED", 1.0, 1937.20, 2005.80, 100.0, 1937.20, 2005.80, 68.60, "p58_6a8d995cb690"),
            OracleJournalEntry(1787838009000L, "NVDA", "SELL (PARTIAL)", 18.7, "Vânzare parțială", "ACTIVE", 2.0, 126.50, 131.75, 40.0, 253.00, 263.50, 10.50, "p58_6a8f1c1e3ab2")
        ))

        repository.markBootstrap(VERSION)
    }
}
