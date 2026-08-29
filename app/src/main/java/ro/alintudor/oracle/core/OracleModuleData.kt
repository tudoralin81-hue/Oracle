package ro.alintudor.oracle.core

/** Single snapshot shared by all native modules. */
data class OracleModuleData(
    val positions: List<OraclePosition> = emptyList(),
    val alerts: List<OracleAlert> = emptyList(),
    val news: List<OracleNews> = emptyList(),
    val history: List<OracleHistoryPoint> = emptyList(),
    val actions: List<OracleAction> = emptyList(),
    val knowledge: List<OracleKnowledgeItem> = emptyList(),
    val journal: List<OracleJournalEntry> = emptyList()
)

fun OracleRepository.snapshot(): OracleModuleData {
    val actions = cachedActions()
    val persistedJournal = cachedJournal()
    return OracleModuleData(
        positions = cachedPositions(),
        alerts = cachedAlerts(),
        news = cachedNews(),
        history = cachedHistory(),
        actions = actions,
        knowledge = cachedKnowledge(),
        journal = if (persistedJournal.isNotEmpty()) persistedJournal else OracleActivityJournal.fromActions(actions)
    )
}
