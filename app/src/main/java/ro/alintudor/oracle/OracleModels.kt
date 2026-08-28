package ro.alintudor.oracle

import ro.alintudor.oracle.core.OracleAction
import ro.alintudor.oracle.core.OracleAlert
import ro.alintudor.oracle.core.OracleHistoryPoint
import ro.alintudor.oracle.core.OracleKnowledgeItem
import ro.alintudor.oracle.core.OracleModuleData
import ro.alintudor.oracle.core.OracleNews
import ro.alintudor.oracle.core.OraclePosition

/**
 * Compatibility aliases for the original root-package API.
 * The core package is now the single source of truth for Oracle domain models.
 */
typealias OraclePosition = OraclePosition
typealias OracleAlert = OracleAlert
typealias OracleNews = OracleNews
typealias OracleHistoricalPoint = OracleHistoryPoint
typealias OracleAction = OracleAction
typealias OracleKnowledgeItem = OracleKnowledgeItem
typealias OracleModuleState = OracleModuleData
