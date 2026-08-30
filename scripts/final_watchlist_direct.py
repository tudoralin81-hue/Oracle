from pathlib import Path

p = Path('app/src/main/java/ro/alintudor/oracle/MainActivity.kt')
s = p.read_text()
old_case = '"watchlist"->OracleSimpleModule(host,title,::openWatchlistTicker).render(actions=data.actions,knowledge=data.knowledge,positions=data.positions,history=data.history)'
new_case = '"watchlist"->renderWatchlistDirect()'
if old_case not in s:
    raise SystemExit('Watchlist render case not found')
s = s.replace(old_case, new_case, 1)

marker = '    private fun showModuleError(key:String,error:Throwable){'
if marker not in s:
    raise SystemExit('showModuleError marker not found')

method = '''    private fun renderWatchlistDirect() {
        root.removeAllViews()
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.rgb(1, 3, 8))
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(30))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(6))
        }
        val back = Button(this).apply {
            text = "‹"
            textSize = 28f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 0)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(5, 9, 18))
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), Color.rgb(255, 205, 55))
            }
            setOnClickListener { showHub() }
        }
        header.addView(back, LinearLayout.LayoutParams(dp(70), dp(52)))
        header.addView(TextView(this).apply {
            text = "WATCHLIST"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = .12f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(255, 215, 45))
        }, LinearLayout.LayoutParams(0, dp(52), 1f))
        header.addView(Space(this), LinearLayout.LayoutParams(dp(70), dp(52)))
        page.addView(header)

        page.addView(View(this).apply { setBackgroundColor(Color.rgb(255, 205, 55)) }, LinearLayout.LayoutParams(-1, dp(1)).apply { setMargins(0, 0, 0, dp(28)) })
        page.addView(TextView(this).apply {
            text = "WATCHLIST • TICKERE SALVATE"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = .10f
            setTextColor(Color.rgb(255, 215, 45))
            setPadding(dp(8), 0, 0, dp(14))
        }, LinearLayout.LayoutParams(-1, -2))

        val store = OracleWatchlistStore(this)
        val tickers = store.load().map { it.trim().uppercase(java.util.Locale.US) }.filter { it.isNotBlank() }.distinct()
        if (tickers.isEmpty()) {
            page.addView(TextView(this).apply {
                text = "WATCHLIST GOALĂ"
                textSize = 18f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, dp(30), 0, dp(30))
            })
        } else {
            tickers.forEach { ticker ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(14), dp(8), dp(8), dp(8))
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.rgb(6, 11, 22))
                        cornerRadius = dp(16).toFloat()
                        setStroke(dp(1), Color.rgb(45, 65, 95))
                    }
                }
                val open = Button(this).apply {
                    text = "$ticker   ›"
                    textSize = 20f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER_VERTICAL or Gravity.START
                    setTextColor(Color.WHITE)
                    setPadding(dp(10), 0, dp(8), 0)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.TRANSPARENT)
                        cornerRadius = dp(12).toFloat()
                    }
                    isAllCaps = false
                    contentDescription = "Deschide analiza pentru $ticker"
                    setOnClickListener { openWatchlistTicker(ticker) }
                }
                row.addView(open, LinearLayout.LayoutParams(0, dp(76), 1f))

                val delete = Button(this).apply {
                    text = "ȘTERGE"
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.rgb(255, 105, 105))
                    setPadding(0, 0, 0, 0)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.TRANSPARENT)
                        cornerRadius = dp(10).toFloat()
                    }
                    contentDescription = "Șterge $ticker din Watchlist"
                    setOnClickListener {
                        val current = store.load().toMutableList()
                        current.removeAll { it.equals(ticker, true) }
                        store.save(current)
                        renderWatchlistDirect()
                    }
                }
                row.addView(delete, LinearLayout.LayoutParams(dp(110), dp(76)))
                page.addView(row, LinearLayout.LayoutParams(-1, dp(92)).apply { setMargins(0, 0, 0, dp(12)) })
            }
        }
        scroll.addView(page)
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
    }

'''
s = s.replace(marker, method + marker, 1)
p.write_text(s)
print('FINAL Watchlist direct navigation patch applied')
