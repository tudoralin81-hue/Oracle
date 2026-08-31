from pathlib import Path

MARKER = "// ANALYSIS_VALUES_REPAIR_498"

files = {
    "app/src/main/java/ro/alintudor/oracle/nativeui/OracleNativeModule.kt": None,
    "app/src/main/java/ro/alintudor/oracle/core/OracleRealData.kt": None,
    "app/src/main/java/ro/alintudor/oracle/nativeui/OracleAnalysisModules.kt": None,
    "app/build.gradle": None,
}

native = Path("app/src/main/java/ro/alintudor/oracle/nativeui/OracleNativeModule.kt")
s = native.read_text(encoding="utf-8")
if MARKER not in s:
    old = 'center.addView(TextView(context).apply { text="BUILD 494";textSize=8f;typeface=Typeface.DEFAULT_BOLD;letterSpacing=.10f;setTextColor(Color.rgb(115,150,190));gravity=Gravity.CENTER;includeFontPadding=true })'
    new = 'center.addView(TextView(context).apply { text="BUILD 498";textSize=10f;typeface=Typeface.DEFAULT_BOLD;letterSpacing=.10f;setTextColor(Color.rgb(25,205,255));gravity=Gravity.CENTER;includeFontPadding=true })\n        // ANALYSIS_VALUES_REPAIR_498'
    if old not in s:
        raise SystemExit("BUILD 494 header anchor not found")
    native.write_text(s.replace(old, new, 1), encoding="utf-8")

real = Path("app/src/main/java/ro/alintudor/oracle/core/OracleRealData.kt")
s = real.read_text(encoding="utf-8")
old_pe = 'val pe=summary?.trailingPe ?: quote?.trailingPe ?: ts?.trailingPe'
new_pe = 'val pe=(summary?.trailingPe ?: quote?.trailingPe ?: ts?.trailingPe)?.takeIf { it.isFinite() && it > 0.0 }'
old_fpe = 'val fpe=summary?.forwardPe ?: quote?.forwardPe ?: ts?.forwardPe'
new_fpe = 'val fpe=(summary?.forwardPe ?: quote?.forwardPe ?: ts?.forwardPe)?.takeIf { it.isFinite() && it > 0.0 }'
if old_pe not in s or old_fpe not in s:
    raise SystemExit("P/E fundamentals anchors not found")
s = s.replace(old_pe, new_pe, 1).replace(old_fpe, new_fpe, 1)
# Use the sector naming used by the current public reference data; this is only a fallback.
s = s.replace('->"Information Technology"', '->"Technology"', 1)
real.write_text(s, encoding="utf-8")

analysis = Path("app/src/main/java/ro/alintudor/oracle/nativeui/OracleAnalysisModules.kt")
s = analysis.read_text(encoding="utf-8")
old = 'l == "REVENUE GROWTH"'
new = 'l.startsWith("REVENUE GROWTH")'
if old in s:
    s = s.replace(old, new, 1)
analysis.write_text(s, encoding="utf-8")

gradle = Path("app/build.gradle")
s = gradle.read_text(encoding="utf-8")
s = s.replace("versionCode 16", "versionCode 17", 1)
s = s.replace("versionName 'V6g-KNOWLEDGE-B494'", "versionName 'V6g-KNOWLEDGE-B498'", 1)
gradle.write_text(s, encoding="utf-8")

print("Analysis repair 498 applied: visible build label, positive-only P/E display, Technology fallback, and build metadata.")
