package ro.alintudor.oracle.core

/** Sector is an allocation correction, not a scoring factor. */
object OracleSectorAllocation {
    private const val MIN_FACTOR = 0.50
    private const val MAX_FACTOR = 1.25
    private const val STEP = 0.025

    fun factorFor(sector: String?): Double {
        val s = sector?.trim()?.lowercase() ?: return 1.0
        val raw = when {
            s.contains("biotech") || s.contains("biotechnology") -> 0.750
            s.contains("semiconductor") || s.contains("semiconductors") -> 0.900
            s.contains("fintech") || s.contains("financial technology") -> 0.900
            s.contains("ai") || s.contains("artificial intelligence") -> 0.850
            s.contains("cyber") || s.contains("cybersecurity") -> 0.900
            s.contains("software") -> 0.950
            s.contains("technology") || s == "tech" -> 0.950
            s.contains("energy") -> 0.975
            s.contains("industr") -> 1.000
            s.contains("healthcare") || s.contains("health care") -> 1.025
            s.contains("consumer staples") -> 1.075
            s.contains("utilities") -> 1.100
            s.contains("real estate") -> 1.075
            s.contains("telecom") || s.contains("communication") -> 1.025
            s.contains("financial") || s.contains("bank") -> 1.000
            s.contains("materials") -> 1.000
            else -> 1.000
        }
        return snapToStep(raw.coerceIn(MIN_FACTOR, MAX_FACTOR))
    }

    fun apply(baseAllocation: Double, sector: String?): Double {
        val corrected = baseAllocation * factorFor(sector)
        return snapToStep(corrected.coerceIn(0.0, 8.0))
    }

    private fun snapToStep(value: Double): Double =
        kotlin.math.round(value / STEP) * STEP
}
