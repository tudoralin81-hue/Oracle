from pathlib import Path

p = Path('app/src/main/java/ro/alintudor/oracle/MainActivity.kt')
s = p.read_text()
required = ['OracleKnowledgeSync', '"knowledge"', 'private fun renderModule']
missing = [x for x in required if x not in s]
if missing:
    raise SystemExit('Knowledge source is incomplete: ' + ', '.join(missing))

# Ensure the actual MainActivity route renders Knowledge.
start = s.find('            "knowledge"->OracleKnowledgeModule(host).render(')
if start >= 0:
    end_marker = '            )\n        }\n        host.restoreScrollY(preservedScrollY)'
    end = s.find(end_marker, start)
    if end < 0:
        raise SystemExit('Knowledge render branch end not found')
    s = s[:start] + '            "knowledge"->renderKnowledgeDirect(host)\n' + s[end + len('            )\n'):]

# Install the direct web card if not already present.
if 'DESCHIDE KNOWLEDGE' not in s:
    marker = '        host.content.removeAllViews()\n        val webCard = LinearLayout(context).apply {'
    card = '''        host.content.removeAllViews()
        val context = this
        val webCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(host.dp(16), host.dp(15), host.dp(16), host.dp(15))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(8, 14, 27))
                cornerRadius = host.dp(16).toFloat()
                setStroke(host.dp(1), Color.rgb(255, 205, 55))
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { openKnowledgeUrl("https://alintudor.ro/knowledge/") }
        }
        webCard.addView(TextView(context).apply {
            text = "KNOWLEDGE • ALINTUDOR.RO"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(255, 215, 45))
        })
        webCard.addView(TextView(context).apply {
            text = "Deschide biblioteca Knowledge"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(0, host.dp(7), 0, host.dp(11))
        })
        webCard.addView(Button(context).apply {
            text = "DESCHIDE KNOWLEDGE"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(12, 54, 82))
                cornerRadius = host.dp(11).toFloat()
            }
            setOnClickListener { openKnowledgeUrl("https://alintudor.ro/knowledge/") }
        }, LinearLayout.LayoutParams(-1, host.dp(44)))
        host.content.addView(webCard, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(14)) })
        host.addSectionLabel("KNOWLEDGE • ALINTUDOR.RO")'''
    if marker not in s:
        raise SystemExit('Knowledge web card marker not found')
    s = s.replace(marker, card, 1)

# The generated direct renderer must always have a local Android Context.
fn = '    private fun renderKnowledgeDirect(host: OracleNativeModule) {\n'
if fn in s and '    private fun renderKnowledgeDirect(host: OracleNativeModule) {\n        val context = this\n' not in s:
    s = s.replace(fn, fn + '        val context = this\n', 1)

if 'DESCHIDE KNOWLEDGE' not in s or 'https://alintudor.ro/knowledge/' not in s:
    raise SystemExit('Knowledge web card was not installed')

p.write_text(s)
print('Knowledge direct web card installed')
