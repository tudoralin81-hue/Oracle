package ro.alintudor.oracle.nativeui

import android.graphics.Color

/** Small compatibility helpers used by the native Analysis chart renderer. */
fun Int.red(): Int = Color.red(this)
fun Int.green(): Int = Color.green(this)
fun Int.blue(): Int = Color.blue(this)

/** Keeps the existing chart source independent of an extra android.graphics import. */
object Typeface {
    val DEFAULT_BOLD: android.graphics.Typeface = android.graphics.Typeface.DEFAULT_BOLD
}
