from pathlib import Path
import re

src = Path('app/src/main/java/ro/alintudor/oracle/nativeui/OracleAnalysisModules.kt')
s = src.read_text(encoding='utf-8')
start = s.find('    private fun addMetricGrid(')
end = s.find('    private fun metricValueColor', start)
if start < 0 or end < 0:
    raise SystemExit('B511: addMetricGrid anchors not found')
grid = '''    private fun addMetricGrid(container: LinearLayout, items: List<Pair<String, String>>) {
        var row: LinearLayout? = null
        items.forEachIndexed { index, item ->
            if (index % 2 == 0) {
                row = LinearLayout(host.root.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.FILL_VERTICAL
                }
                container.addView(row, LinearLayout.LayoutParams(-1, -2).apply {
                    setMargins(0, 0, 0, host.dp(6))
                })
            }
            val card = LinearLayout(host.root.context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(host.dp(11), host.dp(8), host.dp(11), host.dp(8))
                background = GradientDrawable().apply {
                    setColor(Color.rgb(6, 12, 24))
                    cornerRadius = host.dp(12).toFloat()
                    setStroke(host.dp(1), Color.rgb(35, 65, 98))
                }
            }
            card.addView(TextView(host.root.context).apply {
                text = item.first.uppercase(Locale.US)
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = .07f
                setTextColor(Color.rgb(85, 190, 235))
                includeFontPadding = true
            })
            card.addView(TextView(host.root.context).apply {
                text = item.second
                textSize = 12.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(metricValueColor(item.first, item.second))
                setPadding(0, host.dp(2), 0, 0)
                includeFontPadding = true
                setHorizontallyScrolling(false)
                maxLines = Int.MAX_VALUE
                ellipsize = null
            })
            row?.addView(card, LinearLayout.LayoutParams(0, -2, 1f).apply {
                if (index % 2 == 1) setMargins(host.dp(4), 0, 0, 0)
                else setMargins(0, 0, host.dp(4), 0)
            })
        }
        container.post {
            for (i in 0 until container.childCount) {
                val rv = container.getChildAt(i) as? LinearLayout ?: continue
                var maxHeight = 0
                for (j in 0 until rv.childCount) maxHeight = maxOf(maxHeight, rv.getChildAt(j).measuredHeight)
                if (maxHeight > 0) for (j in 0 until rv.childCount) {
                    val child = rv.getChildAt(j)
                    val lp = child.layoutParams
                    lp.height = maxHeight
                    child.layoutParams = lp
                }
                rv.requestLayout()
            }
        }
    }

'''
s = s[:start] + grid + s[end:]
pattern = re.compile(r'(?s)        top\.addView\(TextView\(host\.root\.context\)\.apply \{\n            text = "\$\{companyName\(r\.ticker\)\}.*?\n        \}\)')
replacement = '''        top.addView(TextView(host.root.context).apply {
            text = companyName(r.ticker)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(205, 213, 228))
            setPadding(0, host.dp(4), 0, 0)
        })
        top.addView(TextView(host.root.context).apply {
            text = "Sector: ${r.sector ?: "Sector indisponibil"}"
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(145, 158, 180))
            setPadding(0, host.dp(2), 0, 0)
        })'''
s2 = pattern.sub(replacement, s, count=1)
if s2 == s:
    raise SystemExit('B511: company header anchor not found')
s = s2
s = s.replace('Color.rgb(228, 178, 28)', 'Color.rgb(205, 165, 38)')
if '"APLD" -> "Applied Digital Corporation"' not in s:
    anchor = '"AAOI" -> "Applied Optoelectronics, Inc."'
    if anchor not in s:
        raise SystemExit('B511: APLD map anchor not found')
    s = s.replace(anchor, anchor + '\n        "APLD" -> "Applied Digital Corporation"', 1)
s = re.sub(r'V6g-FINAL-B\d+', 'V6g-FINAL-B511', s)
src.write_text(s, encoding='utf-8')

gradle = Path('app/build.gradle')
g = gradle.read_text(encoding='utf-8')
g = re.sub(r'versionCode\s+\d+', 'versionCode 26', g, count=1)
g = re.sub(r"versionName\s+'[^']+'", "versionName 'V6g-FINAL-B511'", g, count=1)
gradle.write_text(g, encoding='utf-8')
