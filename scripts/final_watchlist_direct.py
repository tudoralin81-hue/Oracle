from pathlib import Path

p = Path('app/src/main/java/ro/alintudor/oracle/MainActivity.kt')
s = p.read_text()
needle = '                    setOnClickListener { openWatchlistTicker(ticker) }'
replacement = '''                    setOnClickListener { openWatchlistTicker(ticker) }
                    setOnTouchListener { v, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                scroll.requestDisallowInterceptTouchEvent(true)
                                false
                            }
                            MotionEvent.ACTION_UP -> {
                                scroll.requestDisallowInterceptTouchEvent(false)
                                v.performClick()
                                true
                            }
                            MotionEvent.ACTION_CANCEL -> {
                                scroll.requestDisallowInterceptTouchEvent(false)
                                false
                            }
                            else -> false
                        }
                    }'''
if needle not in s:
    raise SystemExit('Watchlist open button handler not found')
s = s.replace(needle, replacement, 1)

needle2 = '                    setOnClickListener { openWatchlistTicker(ticker) }\n                    setOnTouchListener'
# Make the complete row a second navigation target, while preserving the DELETE child.
row_anchor = '''                val delete = Button(this).apply {'''
row_patch = '''                row.setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            scroll.requestDisallowInterceptTouchEvent(true)
                            false
                        }
                        MotionEvent.ACTION_UP -> {
                            scroll.requestDisallowInterceptTouchEvent(false)
                            openWatchlistTicker(ticker)
                            true
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            scroll.requestDisallowInterceptTouchEvent(false)
                            false
                        }
                        else -> false
                    }
                }

                val delete = Button(this).apply {'''
if row_anchor not in s:
    raise SystemExit('Watchlist row anchor not found')
s = s.replace(row_anchor, row_patch, 1)

p.write_text(s)
print('FINAL Watchlist touch navigation patch applied')
