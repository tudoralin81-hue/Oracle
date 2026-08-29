package ro.alintudor.oracle.nativeui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import ro.alintudor.oracle.core.*
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Functional Portfolio module: local positions, journal and model-matched file exports. */
class OraclePortfolioModule(private val host: OracleNativeModule) {
    private val context: Context get() = host.root.context
    private val repo by lazy { OracleRepository(context) }
    private val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun render(positions: List<OraclePosition>) {
        host.content.removeAllViews()
        val data = repo.snapshot()
        val items = OracleAnalytics.normalize(positions)
        host.addCard("PORTFOLIO", "Poziții, valoare, acțiuni, prognoză Oracle, randament real și indicatori")
        if (items.isEmpty()) {
            host.addCard("FĂRĂ POZIȚII", "Nu există poziții active în memoria locală.")
            addManagementRow()
            return
        }
        val value = items.sumOf { it.marketValue }
        val invested = items.sumOf { it.shares * it.avgCost }
        val pnl = items.sumOf { it.pnl }
        addHero(value, pnl, if (invested == 0.0) 0.0 else pnl / invested * 100.0, items.size)
        addMetrics(items)
        addPositionSummary(items)
        addManagementRow()
        val actions = OracleAnalytics.actions(items, data.history).associateBy { it.ticker }
        val tech = OracleTechnicalIndicators.all(data.history)
        items.sortedByDescending { it.marketValue }.forEachIndexed { i, p ->
            card(i + 1, p, actions[p.ticker], tech[p.ticker], data.journal)
        }
        addBottomActions(items, data.journal)
    }

    private fun addHero(value: Double, pnl: Double, pct: Double, count: Int) {
        val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(host.dp(18), host.dp(15), host.dp(18), host.dp(15)); background = OracleNativeModule.rounded(Color.rgb(7, 11, 22), host.dp(16), Color.rgb(92, 72, 28), host.dp(1)) }
        val row = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(context).apply { text = "◔"; textSize = 32f; setTextColor(Color.rgb(255, 210, 55)); gravity = Gravity.CENTER }, LinearLayout.LayoutParams(host.dp(45), host.dp(45)))
        row.addView(LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(host.dp(10), 0, 0, 0); addView(TextView(context).apply { text = "TOTAL PORTOFOLIU • $count POZIȚII"; textSize = 11f; setTextColor(Color.rgb(155, 166, 188)) }); addView(TextView(context).apply { text = money(value); textSize = 23f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(0, host.dp(3), 0, 0) }) }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(TextView(context).apply { text = signedPct(pct); textSize = 18f; typeface = Typeface.DEFAULT_BOLD; setTextColor(if (pnl >= 0) Color.rgb(145, 245, 35) else Color.rgb(255, 80, 65)) })
        box.addView(row); box.addView(TextView(context).apply { text = "P/L  ${money(pnl)}"; textSize = 13f; setTextColor(Color.rgb(175, 183, 201)); setPadding(host.dp(55), host.dp(5), 0, 0) })
        host.content.addView(box, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(9)) })
    }

    private fun addMetrics(items: List<OraclePosition>) { val row1 = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }; metric(row1, "CÂȘTIGĂTOARE", items.count { it.pnl > 0 }.toString()); metric(row1, "PIERZĂTOARE", items.count { it.pnl < 0 }.toString()); host.content.addView(row1); val row2 = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }; metric(row2, "CONCENTRARE MAX.", pct(items.maxOf { it.weight })); metric(row2, "RISC", when { items.maxOf { it.weight } >= 50 -> "HIGH"; items.maxOf { it.weight } >= 35 -> "MEDIUM"; else -> "CONTROLAT" }); host.content.addView(row2) }

    private fun addPositionSummary(items: List<OraclePosition>) { val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(host.dp(13), host.dp(11), host.dp(13), host.dp(11)); background = OracleNativeModule.rounded(Color.rgb(7, 11, 22), host.dp(12), Color.rgb(42, 52, 76), host.dp(1)) }; box.addView(TextView(context).apply { text = "POZIȚII ACTIVE • TICKER / ACȚIUNI / VALOARE"; textSize = 10f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(110, 220, 255)) }); items.sortedBy { it.ticker }.forEach { p -> val row = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, host.dp(7), 0, 0) }; row.addView(TextView(context).apply { text = p.ticker; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(host.dp(62), -2)); row.addView(TextView(context).apply { text = "${shares(p.shares)} acțiuni"; textSize = 12f; setTextColor(Color.rgb(175, 183, 201)) }, LinearLayout.LayoutParams(0, -2, 1f)); row.addView(TextView(context).apply { text = money(p.marketValue) + " ${p.currency}"; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(255, 210, 55)) }); box.addView(row) }; host.content.addView(box, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, host.dp(8), 0, host.dp(8)) }) }

    /** Management row intentionally contains only Add Position. Journal lives at the bottom. */
    private fun addManagementRow() {
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(host.dp(2), 0, host.dp(2), 0) }
        row.addView(btn("+ ADAUGĂ POZIȚIE", Color.rgb(145, 245, 35)) { addPositionDialog() }, LinearLayout.LayoutParams(-1, host.dp(46)))
        host.content.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(8)) })
    }

    private fun card(rank: Int, p: OraclePosition, a: OracleAction?, t: OracleTechnicalSnapshot?, journal: List<OracleJournalEntry>) {
        val forecast = journal.filter { it.ticker.equals(p.ticker, true) && it.action.contains("BUY / OPEN", true) }.minByOrNull { it.timestamp }?.score ?: a?.score ?: 0.0
        val action = decision(a?.action ?: "HOLD", t)
        val accent = when (action) { "BUY" -> Color.rgb(145, 245, 35); "SELL" -> Color.rgb(255, 80, 95); else -> Color.rgb(50, 220, 190) }
        val reason = when { t == null -> "Date tehnice insuficiente; monitorizare locală"; t.rsi >= 70 -> "supraîncălzire RSI · trend și momentum încă acceptabile"; t.rsi <= 30 -> "RSI slab · presiune de vânzare"; action == "BUY" -> "trend și momentum favorabile"; action == "SELL" -> "semnal negativ · risc în creștere"; else -> "trend și momentum încă acceptabile" }
        val c = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(host.dp(15), host.dp(13), host.dp(12), host.dp(13)); background = OracleNativeModule.rounded(Color.rgb(6, 10, 20), host.dp(15), Color.rgb(42, 52, 76), host.dp(1)) }
        val top = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(TextView(context).apply { text = "%02d".format(rank); textSize = 11f; typeface = Typeface.DEFAULT_BOLD; setTextColor(accent) }, LinearLayout.LayoutParams(host.dp(34), host.dp(30)))
        top.addView(LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; addView(TextView(context).apply { text = p.ticker; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) }); addView(TextView(context).apply { text = "${p.company} • ${shares(p.shares)} acțiuni • intrare ${money(p.avgCost)}"; textSize = 10f; setTextColor(Color.rgb(155, 166, 188)); setPadding(0, host.dp(2), 0, 0) }) }, LinearLayout.LayoutParams(0, -2, 1f))
        val topAction = TextView(context).apply { text = action; textSize = 12f; typeface = Typeface.DEFAULT_BOLD; setTextColor(accent); gravity = Gravity.CENTER }
        top.addView(topAction, LinearLayout.LayoutParams(host.dp(62), host.dp(30)))
        pulseSignal(topAction, action)
        c.addView(top)
        c.addView(TextView(context).apply { text = "${money(p.marketValue)} ${p.currency}   •   ${pct(p.weight)} PONDERE   •   ${shares(p.shares)} ACȚIUNI"; textSize = 13f; setTextColor(Color.rgb(175, 183, 201)); setPadding(host.dp(34), host.dp(5), 0, 0) })
        val forecasts = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(host.dp(34), host.dp(10), 0, 0) }
        forecasts.addView(valueBox("PROGNOZAT ORACLE", signedPct(forecast), Color.rgb(55, 215, 255)), LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(0, 0, host.dp(4), 0) })
        forecasts.addView(valueBox("REAL ACUM", signedPct(p.pnlPercent), if (p.pnlPercent >= 0) Color.rgb(65, 225, 135) else Color.rgb(255, 85, 105)), LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(host.dp(4), 0, 0, 0) })
        c.addView(forecasts)
        val decision = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(host.dp(15), host.dp(9), host.dp(15), host.dp(9)); background = OracleNativeModule.rounded(Color.rgb(8, 16, 25), host.dp(11), accent, host.dp(1)) }
        val decisionSignal = TextView(context).apply { text = action; textSize = 18f; typeface = Typeface.DEFAULT_BOLD; setTextColor(accent) }
        decision.addView(decisionSignal)
        pulseSignal(decisionSignal, action)
        decision.addView(TextView(context).apply { text = reason; textSize = 12f; setTextColor(Color.rgb(190, 198, 215)); setPadding(0, host.dp(4), 0, 0) })
        c.addView(decision, LinearLayout.LayoutParams(-1, -2).apply { setMargins(host.dp(34), host.dp(8), 0, 0) })
        val grid = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(host.dp(34), host.dp(8), 0, 0) }
        two(grid, "P/L", "${money(p.pnl)} (${signedPct(p.pnlPercent)})", "Score", a?.score?.let { String.format(Locale.US, "%.0f/100", it) } ?: "N/A")
        two(grid, "RSI", t?.rsi?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A", "SMA50", money(t?.sma50 ?: p.currentPrice))
        two(grid, "Momentum 5D", signedPct(t?.momentum5D ?: 0.0), "Momentum 20D", signedPct(t?.momentum20D ?: 0.0))
        two(grid, "Suport 20D", money(t?.support20D ?: p.currentPrice), "Rezistență 20D", money(t?.resistance20D ?: p.currentPrice))
        c.addView(grid)
        val buttons = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(host.dp(34), host.dp(9), 0, 0) }
        buttons.addView(btn("SELL ACȚIUNI", Color.rgb(255, 205, 65)) { partialSell(p, forecast) }, LinearLayout.LayoutParams(0, host.dp(43), 1f).apply { setMargins(0, 0, host.dp(4), 0) })
        buttons.addView(btn("FULL SELL", Color.rgb(255, 80, 105)) { fullSell(p, forecast) }, LinearLayout.LayoutParams(0, host.dp(43), 1f).apply { setMargins(host.dp(4), 0, 0, 0) })
        c.addView(buttons)
        c.addView(TextView(context).apply { text = "Actualizat local • ${date.format(Date())}"; textSize = 9f; setTextColor(Color.rgb(105, 120, 145)); setPadding(host.dp(34), host.dp(7), 0, 0) })
        host.content.addView(c, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(9)) })
    }

    private fun pulseSignal(view: TextView, action: String) {
        if (action != "SELL" && action != "HOLD") return
        ObjectAnimator.ofFloat(view, "alpha", 1f, 0.38f, 1f).apply {
            duration = 1150L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            start()
        }
    }

    private fun valueBox(label: String, value: String, color: Int) = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(host.dp(9), host.dp(8), host.dp(9), host.dp(8)); background = OracleNativeModule.rounded(Color.rgb(8, 13, 27), host.dp(10), color, host.dp(1)); addView(TextView(context).apply { text = label; textSize = 9f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(155, 166, 188)) }); addView(TextView(context).apply { text = value; textSize = 19f; typeface = Typeface.DEFAULT_BOLD; setTextColor(color); setPadding(0, host.dp(2), 0, 0) }) }
    private fun two(g: LinearLayout, a: String, av: String, b: String, bv: String) { val r = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }; metric(r, a, av); metric(r, b, bv); g.addView(r) }
    private fun metric(row: LinearLayout, label: String, value: String) { val b = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(host.dp(13), host.dp(10), host.dp(13), host.dp(10)); background = OracleNativeModule.rounded(Color.rgb(7, 11, 22), host.dp(11), Color.rgb(35, 44, 66), host.dp(1)) }; b.addView(TextView(context).apply { text = label; textSize = 9f; setTextColor(Color.rgb(145, 155, 176)) }); b.addView(TextView(context).apply { text = value; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(0, host.dp(3), 0, 0) }); row.addView(b, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(host.dp(2), host.dp(4), host.dp(2), host.dp(5)) }) }
    private fun btn(label: String, color: Int, click: () -> Unit) = TextView(context).apply { text = label; textSize = 10f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setTextColor(color); background = OracleNativeModule.rounded(Color.rgb(8, 12, 25), host.dp(10), color, host.dp(1)); isClickable = true; isFocusable = true; setOnClickListener { click() } }
    private fun addPositionDialog() { val panel = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(host.dp(4), 0, host.dp(4), 0) }; val ticker = field("TICKER", "CRM"); val company = field("COMPANIE", "Salesforce"); val shares = field("NUMĂR ACȚIUNI", "1"); val entry = field("PREȚ INTRARE", "100"); val current = field("PREȚ CURENT", "100"); listOf(ticker, company, shares, entry, current).forEach { panel.addView(it.first); panel.addView(it.second) }; AlertDialog.Builder(context).setTitle("ADĂUGĂ POZIȚIE").setMessage("Poziția este salvată local în Oracle.").setView(panel).setNegativeButton("ANULEAZĂ", null).setPositiveButton("ADAUGĂ") { _, _ -> val t = ticker.second.text.toString().trim().uppercase(Locale.US); val c = company.second.text.toString().trim().ifEmpty { t }; val q = shares.second.text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0; val e = entry.second.text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0; val cp = current.second.text.toString().replace(',', '.').toDoubleOrNull() ?: e; if (t.isEmpty() || q <= 0.0 || e <= 0.0 || cp <= 0.0) { toast("Date invalide pentru poziție"); return@setPositiveButton }; val existing = repo.cachedPositions().filterNot { it.ticker.equals(t, true) }.toMutableList(); existing += OracleCalculations.position(t, c, q, e, cp); repo.savePositions(OracleCalculations.withWeights(existing)); val now = System.currentTimeMillis(); repo.saveJournal(repo.cachedJournal() + OracleJournalEntry(now, t, "BUY / OPEN", 0.0, "Poziție adăugată local", "ACTIVE", q, e, 0.0, 0.0, q * e, 0.0, 0.0, "manual_$now")); toast("$t adăugat în portofoliu"); render(repo.cachedPositions()) }.show() }
    private fun field(label: String, value: String): Pair<TextView, EditText> { val labelView = TextView(context).apply { text = label; textSize = 9f; setTextColor(Color.rgb(145, 155, 176)); setPadding(0, host.dp(5), 0, host.dp(2)) }; val edit = EditText(context).apply { setText(value); setTextColor(Color.WHITE); setSingleLine(true); textSize = 15f; setSelectAllOnFocus(true) }; return labelView to edit }
    private fun partialSell(p: OraclePosition, forecast: Double) { val input = EditText(context).apply { inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; setText(shares(p.shares / 2)) }; AlertDialog.Builder(context).setTitle("SELL ACȚIUNI • ${p.ticker}").setMessage("Acțiunea este locală în Oracle; nu execută tranzacții la broker.\n\nCantitate:").setView(input).setNegativeButton("ANULEAZĂ", null).setPositiveButton("CONFIRMĂ") { _, _ -> val q = input.text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0; if (q <= 0 || q > p.shares) { toast("Cantitate invalidă"); return@setPositiveButton }; sell(p, q, false, forecast) }.show() }
    private fun fullSell(p: OraclePosition, forecast: Double) { AlertDialog.Builder(context).setTitle("FULL SELL • ${p.ticker}").setMessage("Închide poziția locală la ${money(p.currentPrice)}. Nu se transmite brokerului.").setNegativeButton("ANULEAZĂ", null).setPositiveButton("FULL SELL") { _, _ -> sell(p, p.shares, true, forecast) }.show() }
    private fun sell(p: OraclePosition, q: Double, full: Boolean, forecast: Double) { val now = System.currentTimeMillis(); val old = repo.cachedPositions().filterNot { it.ticker.equals(p.ticker, true) }.toMutableList(); val remain = p.shares - q; if (!full && remain > 0) old += p.copy(shares = remain); repo.savePositions(OracleCalculations.withWeights(old)); val j = repo.cachedJournal().toMutableList(); j += OracleJournalEntry(now, p.ticker, if (full) "SELL (FULL)" else "SELL (PARTIAL)", forecast, if (full) "Închidere poziție locală" else "Vânzare parțială locală", if (full) "CLOSED" else "ACTIVE", q, p.avgCost, p.currentPrice, if (p.shares == 0.0) 100.0 else q / p.shares * 100.0, q * p.avgCost, q * p.currentPrice, q * (p.currentPrice - p.avgCost)); repo.saveJournal(j); toast(if (full) "${p.ticker}: poziție închisă local" else "${p.ticker}: vânzare înregistrată"); render(repo.cachedPositions()) }

    private fun addBottomActions(p: List<OraclePosition>, j: List<OracleJournalEntry>) { val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(host.dp(2), host.dp(3), host.dp(2), 0) }; row.addView(btn("JURNAL ACTIVITATE", Color.rgb(55, 215, 255)) { saveJournalAndShow(j) }, LinearLayout.LayoutParams(0, host.dp(46), 1f).apply { setMargins(0, 0, host.dp(3), 0) }); row.addView(btn("DESCARCĂ EXCEL", Color.rgb(65, 225, 135)) { saveExcel(p) }, LinearLayout.LayoutParams(0, host.dp(46), 1f).apply { setMargins(host.dp(3), 0, host.dp(3), 0) }); row.addView(btn("DESCARCĂ PDF", Color.rgb(255, 205, 65)) { savePdf(p) }, LinearLayout.LayoutParams(0, host.dp(46), 1f).apply { setMargins(host.dp(3), 0, 0, 0) }); host.content.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, host.dp(5), 0, host.dp(10)) }) }
    private fun saveJournalAndShow(j: List<OracleJournalEntry>) { val text = "AI STOCK ORACLE — JURNAL ACTIVITATE\nGenerated ${date.format(Date())}\n\n" + j.sortedByDescending { it.timestamp }.take(250).joinToString("\n") { e -> "${date.format(Date(e.timestamp))} | ${e.ticker} | ${e.action} | ${e.status} | ${shares(e.shares)} sh | score=${String.format(Locale.US, "%.1f", e.score)} | ${e.reason}" }; saveDownload("oracle_jurnal_${stamp()}.txt", "text/plain") { it.write(text.toByteArray(Charsets.UTF_8)) }; AlertDialog.Builder(context).setTitle("JURNAL ACTIVITATE").setMessage(text.take(12000)).setPositiveButton("OK", null).show() }
    private fun saveExcel(p: List<OraclePosition>) { val csv = buildString { append("Ticker,Company,Shares,Entry,Current,Value,P/L,P/L%,Weight\n"); p.sortedBy { it.ticker }.forEach { append("${it.ticker},\"${it.company.replace("\"", "\"\"")}\",${it.shares},${it.avgCost},${it.currentPrice},${it.marketValue},${it.pnl},${it.pnlPercent},${it.weight}\n") } }; saveDownload("oracle_portfolio_${stamp()}.csv", "text/csv") { it.write(csv.toByteArray(Charsets.UTF_8)) } }
    private fun savePdf(p: List<OraclePosition>) { saveDownload("oracle_portfolio_${stamp()}.pdf", "application/pdf") { out -> val doc = PdfDocument(); val pageW = 595f; val pageH = 842f; val margin = 28f; val widths = floatArrayOf(62f, 112f, 55f, 65f, 65f, 70f, 65f, 62f, 62f); val rows = mutableListOf<List<String>>(); rows += listOf("TICKER","COMPANY","SHARES","ENTRY","CURRENT","VALUE","P/L","P/L %","WEIGHT"); p.sortedBy { it.ticker }.forEach { rows += listOf(it.ticker,it.company,shares(it.shares),money(it.avgCost),money(it.currentPrice),money(it.marketValue),money(it.pnl),signedPct(it.pnlPercent),pct(it.weight)) }; val rp=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(15,23,42);textSize=8f;typeface=Typeface.DEFAULT}; var pageNo=1; var page=doc.startPage(PdfDocument.PageInfo.Builder(pageW.toInt(),pageH.toInt(),pageNo).create()); var canvas=page.canvas; fun drawHeader(){ rp.typeface=Typeface.DEFAULT_BOLD;rp.textSize=16f;canvas.drawText("AI STOCK ORACLE — PORTFOLIO",margin,30f,rp);rp.typeface=Typeface.DEFAULT;rp.textSize=8f;canvas.drawText("Generated ${date.format(Date())}",margin,43f,rp);rp.typeface=Typeface.DEFAULT_BOLD;rp.textSize=7f }; drawHeader(); var y=60f; rows.forEachIndexed { index, row -> if (y > pageH-46f) { doc.finishPage(page); pageNo++; y=60f; page=doc.startPage(PdfDocument.PageInfo.Builder(pageW.toInt(),pageH.toInt(),pageNo).create()); canvas=page.canvas; drawHeader() }; val bg=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=if(index%2==0) Color.rgb(234,242,250) else Color.WHITE;style=Paint.Style.FILL}; var x=margin; for (c in row.indices) { val v = row[c]; canvas.drawRect(x,y,x+widths[c],y+20f,bg); canvas.drawText(v.take(28),x+2f,y+12f,rp); x+=widths[c] }; y += 20f }; val footer=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(15,23,42);textSize=7f;typeface=Typeface.DEFAULT_BOLD}; canvas.drawText("RATA DE SUCCES GLOBALĂ: ${successRate(rows)}",margin,pageH-18f,footer); doc.finishPage(page); doc.writeTo(out); doc.close() } }
    private fun saveDownload(fileName: String, mime: String, writer: (OutputStream) -> Unit) { runCatching { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { val values=ContentValues().apply{put(MediaStore.Downloads.DISPLAY_NAME,fileName);put(MediaStore.Downloads.MIME_TYPE,mime);put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/Oracle");put(MediaStore.Downloads.IS_PENDING,1)}; val uri=context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values)?:error("Nu pot crea fișierul în Downloads"); try{context.contentResolver.openOutputStream(uri)?.use(writer)?:error("Nu pot scrie fișierul");context.contentResolver.update(uri,ContentValues().apply{put(MediaStore.Downloads.IS_PENDING,0)},null,null)}catch(e:Exception){context.contentResolver.delete(uri,null,null);throw e} } else { val dir=context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?:context.filesDir;dir.mkdirs();File(dir,fileName).outputStream().use(writer) };toast("Salvat: $fileName") }.onFailure{toast("Export eșuat: ${it.message?:it.javaClass.simpleName}")} }
    private fun decision(action:String,t:OracleTechnicalSnapshot?)=when{(t?.rsi?:50.0)>=70.0->"HOLD";action=="BUY"->"BUY";action=="SELL"->"SELL";else->"HOLD"}
    private fun stamp()=SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(Date())
    private fun money(v:Double)=String.format(Locale.US,"%,.2f",v)
    private fun pct(v:Double)=String.format(Locale.US,"%.2f%%",v)
    private fun signedPct(v:Double)=String.format(Locale.US,"%+.1f%%",v)
    private fun shares(v:Double)=if(v%1.0==0.0)v.toInt().toString() else String.format(Locale.US,"%.2f",v)
    private fun toast(s:String)=Toast.makeText(context,s,Toast.LENGTH_LONG).show()
}
