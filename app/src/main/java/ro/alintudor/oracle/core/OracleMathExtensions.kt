package ro.alintudor.oracle.core

import kotlin.math.round

/** Shared numeric rounding helpers for the Oracle engines. */
internal fun Double.roundToHalf(): Double = round(this * 2.0) / 2.0
