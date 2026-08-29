package ro.alintudor.oracle.core

/** Canonical V6e post-processing for Growth raw scores. */
internal object OracleGrowthScoreV6e {
    /**
     * Applies the approved non-overlapping V6e rule:
     * 0..95 unchanged, 96 -> -2, 97..100 -> -3.
     */
    fun finalScore(rawScore: Int): Int {
        require(rawScore in 0..100) { "Growth raw score must be 0..100" }
        return when {
            rawScore <= 95 -> rawScore
            rawScore == 96 -> rawScore - 2
            else -> rawScore - 3
        }
    }
}
