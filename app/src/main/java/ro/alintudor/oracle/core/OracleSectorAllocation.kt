package ro.alintudor.oracle.core

/**
 * Sector is a correction layer over the existing Growth factors.
 * It is deliberately NOT a 13th factor.
 * All sector correction coefficients are quantized to 0.025.
 */
object OracleSectorAllocation {
    private const val MIN_FACTOR = 0.50
    private const val MAX_FACTOR = 1.25
    private const val STEP = 0.025

    private val neutral = doubleArrayOf(1.000,1.000,1.000,1.000,1.000,1.000,1.000,1.000,1.000,1.000,1.000,1.000)

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

    /**
     * Sector-specific correction of the existing 12 Growth weights.
     * The coefficients are relative biases and are always snapped to 0.025.
     * The corrected weights are then renormalized to exactly 100.
     */
    fun correctedWeights(base: IntArray, sector: String?): IntArray {
        if (base.isEmpty()) return base
        val multipliers = profile(sector)
        val raw = base.indices.map { i -> base[i].toDouble() * multipliers[i] }
        val total = raw.sum().takeIf { it > 0.0 } ?: return base.copyOf()
        val exact = raw.map { it * 100.0 / total }
        val result = exact.map { kotlin.math.floor(it).toInt() }.toIntArray()
        var remainder = 100 - result.sum()
        val order = exact.indices.sortedByDescending { exact[it] - kotlin.math.floor(exact[it]) }
        var p = 0
        while (remainder > 0 && order.isNotEmpty()) {
            result[order[p % order.size]]++
            remainder--
            p++
        }
        return result
    }

    fun apply(baseAllocation: Double, sector: String?): Double {
        val corrected = baseAllocation * factorFor(sector)
        return snapToStep(corrected.coerceIn(0.0, 8.0))
    }

    private fun profile(sector: String?): DoubleArray {
        val s = sector?.trim()?.lowercase() ?: return neutral.copyOf()
        val raw = when {
            s.contains("biotech") || s.contains("biotechnology") -> doubleArrayOf(1.100,1.000,0.950,1.000,0.950,0.975,1.100,0.950,0.950,1.100,1.025,0.975)
            s.contains("semiconductor") || s.contains("semiconductors") -> doubleArrayOf(1.025,1.050,1.050,1.075,1.050,0.950,1.050,0.950,1.000,1.075,0.950,1.025)
            s.contains("fintech") || s.contains("financial technology") -> doubleArrayOf(1.050,1.025,1.025,1.050,1.025,0.975,1.050,0.975,1.000,1.075,1.000,1.000)
            s.contains("ai") || s.contains("artificial intelligence") -> doubleArrayOf(1.050,1.050,1.025,1.100,1.050,0.950,1.000,0.950,1.000,1.100,0.950,1.025)
            s.contains("cyber") || s.contains("cybersecurity") -> doubleArrayOf(1.050,1.050,1.050,1.050,1.025,0.975,1.000,0.975,1.000,1.075,0.975,1.025)
            s.contains("software") -> doubleArrayOf(1.025,1.025,1.025,1.050,1.000,0.975,1.025,0.975,1.000,1.050,0.975,1.025)
            s.contains("technology") || s == "tech" -> doubleArrayOf(1.025,1.025,1.025,1.050,1.000,0.975,1.000,0.975,1.000,1.050,0.975,1.025)
            s.contains("energy") -> doubleArrayOf(1.000,1.025,1.050,1.025,1.075,1.000,1.025,0.975,0.975,1.025,1.025,1.025)
            s.contains("industr") -> doubleArrayOf(0.975,1.025,1.050,1.025,1.050,1.000,1.050,1.000,1.000,1.025,1.025,1.050)
            s.contains("healthcare") || s.contains("health care") -> doubleArrayOf(1.050,1.000,1.000,1.000,0.975,1.000,1.100,1.000,1.000,1.050,1.025,0.975)
            s.contains("consumer staples") -> doubleArrayOf(0.975,0.975,1.000,0.950,0.950,1.050,1.075,1.025,1.000,0.950,1.075,0.950)
            s.contains("utilities") -> doubleArrayOf(0.950,0.950,1.000,0.900,0.925,1.075,1.075,1.050,1.025,0.900,1.100,0.950)
            s.contains("real estate") -> doubleArrayOf(0.975,0.975,1.000,0.950,0.950,1.075,1.050,1.025,1.000,0.950,1.075,0.950)
            s.contains("telecom") || s.contains("communication") -> doubleArrayOf(1.000,1.000,1.025,1.000,1.025,1.000,1.000,1.000,1.000,1.050,1.025,1.000)
            s.contains("financial") || s.contains("bank") -> doubleArrayOf(1.025,1.000,1.025,1.025,1.000,1.025,1.075,0.975,1.000,1.050,1.025,1.000)
            s.contains("materials") -> doubleArrayOf(1.000,1.025,1.025,1.000,1.050,1.000,1.050,1.000,1.000,1.025,1.025,1.025)
            else -> neutral.copyOf()
        }
        return raw.map(::snapToStep).toDoubleArray()
    }

    private fun snapToStep(value: Double): Double =
        kotlin.math.round(value / STEP) * STEP
}
