from pathlib import Path

p = Path('app/src/main/java/ro/alintudor/oracle/MainActivity.kt')
s = p.read_text()
required = ['OracleKnowledgeSync', '"knowledge"', 'private fun renderModule']
missing = [x for x in required if x not in s]
if missing:
    raise SystemExit('Knowledge source is incomplete: ' + ', '.join(missing))

start = s.find('            "knowledge"->OracleKnowledgeModule(host).render(')
if start < 0:
    raise SystemExit('Knowledge render branch not found')
end_marker = '            )\n        }\n        host.restoreScrollY(preservedScrollY)'
end = s.find(end_marker, start)
if end < 0:
    raise SystemExit('Knowledge render branch end not found')
s = s[:start] + '            "knowledge"->renderKnowledgeDirect(host)\n' + s[end + len('            )\n'):]

method_marker = '    private fun renderWatchlistDirect() {'
if 'private fun renderKnowledgeDirect(host: OracleNativeModule)' not in s:
    method = '''    private fun renderKnowledgeDirect(host: OracleNativeModule) {
        host.content.removeAllViews()
        host.addSectionLabel("KNOWLEDGE • ALINTUDOR.RO")
        val context = this
        val last = OracleKnowledgeSync.lastSuccess(context)
        val error = OracleKnowledgeSync.lastError(context)
        val status = if (last == 0L) "NESINCRONIZAT" else "ULTIMUL REFRESH: " + java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(last))
        host.addCard("KNOWLEDGE", "Biblioteca Oracle — conținut preluat direct din alintudor.ro/knowledge/\\n$status")
        val refresh = Button(context).apply {
            text = "REFRESH KNOWLEDGE"; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.rgb(12,54,82)); cornerRadius = host.dp(11).toFloat(); setStroke(host.dp(1), Color.rgb(55,105,145)) }
            setOnClickListener {
                Toast.makeText(context, "Se actualizează Knowledge…", Toast.LENGTH_SHORT).show()
                OracleKnowledgeSync.refreshAsync(context) { ok, err ->
                    if (currentModule != "knowledge" || isFinishing) return@refreshAsync
                    if (ok) renderModule("knowledge", false) else Toast.makeText(context, "Knowledge refresh eșuat: ${err ?: "eroare necunoscută"}", Toast.LENGTH_LONG).show()
                }
            }
        }
        host.content.addView(refresh, LinearLayout.LayoutParams(-1, host.dp(46)).apply { setMargins(0,0,0,host.dp(12)) })
        if (error.isNotBlank()) host.addCard("ULTIMA EROARE", error)
        val items = OracleKnowledgeSync.load(context)
        if (items.isEmpty()) {
            host.addCard("SINCRONIZARE", "Nu există articole în cache. Se încearcă preluarea automată…")
            OracleKnowledgeSync.refreshAsync(context) { ok, err ->
                if (currentModule != "knowledge" || isFinishing) return@refreshAsync
                if (ok) renderModule("knowledge", false) else Toast.makeText(context, "Nu s-au putut prelua articolele: ${err ?: "eroare necunoscută"}", Toast.LENGTH_LONG).show()
            }
            return
        }
        host.addSectionLabel("ARTICOLE • ${items.size}")
        val dateFmt = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
        items.forEach { article ->
            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL; setPadding(host.dp(15),host.dp(13),host.dp(15),host.dp(13))
                background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.rgb(7,12,23)); cornerRadius = host.dp(15).toFloat(); setStroke(host.dp(1), Color.rgb(38,55,80)) }
            }
            card.addView(TextView(context).apply { text=article.title; textSize=18f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
            if (article.publishedAt > 0L) card.addView(TextView(context).apply { text=dateFmt.format(java.util.Date(article.publishedAt)); textSize=11f; setTextColor(host.accent); setPadding(0,host.dp(5),0,0) })
            card.addView(TextView(context).apply { text=article.excerpt; textSize=13f; setTextColor(Color.rgb(190,198,213)); setPadding(0,host.dp(8),0,host.dp(8)) })
            card.addView(Button(context).apply {
                text="DESCHIDE ARTICOLUL"; textSize=12f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
                background=android.graphics.drawable.GradientDrawable().apply { setColor(Color.rgb(12,54,82)); cornerRadius=host.dp(11).toFloat() }
                setOnClickListener { openKnowledgeUrl(article.url) }
            }, LinearLayout.LayoutParams(-1,host.dp(44)))
            host.content.addView(card,LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,0,0,host.dp(10)) })
        }
    }

'''
    s = s.replace(method_marker, method + method_marker, 1)

p.write_text(s)
print('Knowledge direct MainActivity renderer installed')
