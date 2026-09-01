from pathlib import Path
import re

ENGINE = Path("app/src/main/java/ro/alintudor/oracle/core/OracleGrowthEngine.kt")
SECTOR = Path("app/src/main/java/ro/alintudor/oracle/core/OracleSectorAllocation.kt")

engine = ENGINE.read_text(encoding="utf-8")
weights_line = '    private val weights=mapOf("SHORT" to intArrayOf(21,18,18,12,16,12,3,4,4,2,2,1),"MEDIUM" to intArrayOf(12,12,12,16,12,9,9,5,5,6,5,4),"LONG" to intArrayOf(6,6,6,19,7,9,18,4,4,9,7,2))'
engine, n = re.subn(r'^\s*private val weights=.*$', weights_line, engine, count=1, flags=re.M)
if n != 1:
    raise SystemExit("Growth weights declaration not found")
if 'val correctedAllocation=OracleSectorAllocation.apply(pick.allocation,sector)' not in engine:
    raise SystemExit("Allocation-only sector correction anchor not found")
if 'correctedWeights=weights[h]!!.copyOf()' not in engine:
    raise SystemExit("Displayed Growth weights anchor not found")
if 'private fun horizonScore' not in engine or 'val w=weights[h]!!' not in engine:
    raise SystemExit("12-factor Growth scoring anchor not found")

# CRITICAL: allocation must remain continuous until the final sector snap.
# The old engine rounded the base allocation to 0.5% first, which forced the
# UI to show values such as 3.0 / 3.5 / 4.0 / 4.5 instead of real tenths.
# Remove that premature half-percent quantization. OracleSectorAllocation.apply()
# is the only final quantizer and snaps to the authoritative 0.025% grid.
engine, n = re.subn(r'\.roundToHalf\(\)', '', engine, count=1)
if n != 1:
    raise SystemExit("Premature 0.5% allocation rounding anchor not found")
if '.roundToHalf()' in engine:
    raise SystemExit("Growth allocation still contains premature 0.5% rounding")
ENGINE.write_text(engine, encoding="utf-8")

SECTOR.write_text('''package ro.alintudor.oracle.core

/** V5.9.7 sector correction: allocation only, never a 13th score factor. */
object OracleSectorAllocation {
    private const val MIN_FACTOR = 0.50
    private const val MAX_FACTOR = 1.25
    private const val STEP = 0.025

    fun factorFor(sector: String?): Double {
        val s = sector?.trim()?.lowercase() ?: return 1.0
        val raw = when {
            s.contains("biotech") || s.contains("biotechnology") -> 0.750
            s.contains("semiconductor") || s.contains("eda") -> 0.900
            s.contains("fintech") || s.contains("financial technology") -> 0.900
            s.contains("cybersecurity") || s.contains("cyber") -> 0.900
            s.contains("artificial intelligence") || s == "ai" || s.contains("ai /") || s.contains("/ ai") -> 0.850
            s.contains("healthcare defensive") || s.contains("defensive healthcare") -> 1.050
            s.contains("healthcare") || s.contains("health care") -> 1.050
            s.contains("industr") -> 1.000
            s.contains("utilities") -> 1.100
            else -> 1.000
        }
        return snapToStep(raw.coerceIn(MIN_FACTOR, MAX_FACTOR))
    }

    fun apply(baseAllocation: Double, sector: String?): Double =
        snapToStep((baseAllocation * factorFor(sector)).coerceIn(0.0, 8.0))

    fun correctedWeights(base: IntArray, sector: String?): IntArray = base.copyOf()

    private fun snapToStep(value: Double): Double = kotlin.math.round(value / STEP) * STEP
}
''', encoding="utf-8")

expected = [
    "21,18,18,12,16,12,3,4,4,2,2,1",
    "12,12,12,16,12,9,9,5,5,6,5,4",
    "6,6,6,19,7,9,18,4,4,9,7,2",
]
for value in expected:
    if value not in engine:
        raise SystemExit(f"Missing authoritative Growth weights: {value}")
print("V5.9.7 Growth reference rules validated; allocation remains continuous until 0.025% final snap")
