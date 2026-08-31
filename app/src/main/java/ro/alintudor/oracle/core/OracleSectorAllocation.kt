package ro.alintudor.oracle.core

/**
 * V5.9.7 sector correction layer.
 * Sector is NOT a 13th scoring component. It changes allocation only.
 * Confirmed factors are the values recorded in the Oracle Growth reference document.
 */
object OracleSectorAllocation {
    private const val MIN_FACTOR = 0.50
    private const val MAX_FACTOR = 1.25
    private const val STEP = 0.025

    /** Allocation correction is separate from the 12-component Growth score. */
    fun factorFor(sector: String?): Double {
        val s = sector?.trim()?.lowercase() ?: return 1.0
        val raw = when {
            s.contains("biotech") || s.contains("biotechnology") -> 0.750
            s.contains("semiconductor") || s.contains("eda") -> 0.900
            s.contains("fintech") || s.contains("financial technology") -> 0.900
            s.contains("cybersecurity") || s.contains("cyber") -> 0.900
            s.contains("artificial intelligence") || s == "ai" || s.contains("ai /") || s.contains("/ ai") -> 0.850
            s.contains("healthcare defensive") || s.contains("defensive healthcare") -> 1.050
            s == "healthcare" || s == "health care" -> 1.050
            s.contains("industr") -> 1.000
            s.contains("utilities") -> 1.100
            else -> 1.000
        }
        return snapToStep(raw.coerceIn(MIN_FACTOR, MAX_FACTOR))
    }

    /** Allocation final = base allocation * sector factor, rounded to 0.025%. */
    fun apply(baseAllocation: Double, sector: String?): Double =
        snapToStep((baseAllocation * factorFor(sector)).coerceIn(0.0, 8.0))

    /** Retained for API compatibility: sector does not modify score weights in V5.9.7. */
    fun correctedWeights(base: IntArray, sector: String?): IntArray = base.copyOf()

    private fun snapToStep(value: Double): Double = kotlin.math.round(value / STEP) * STEP
}
