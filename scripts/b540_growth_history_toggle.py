from pathlib import Path

# B540 final patch is intentionally idempotent: the Claude snapshot may already
# contain the history implementation. Only enforce the requested loader/budget
# changes here, without rewriting GROWTH UI that is already correct.
p = Path('app/src/main/java/ro/alintudor/oracle/nativeui/OracleGrowthModule.kt')
s = p.read_text(encoding='utf-8')

s = s.replace('text("DATE ÎNCĂRCATE: 0 / ${initial.total}", 12f, Typeface.DEFAULT_BOLD, cyan, 0, 10)',
              'text("PROGRES: 0%", 12f, Typeface.DEFAULT_BOLD, cyan, 0, 10)')
s = s.replace('max = initial.total.coerceAtLeast(1); progress = 0; isIndeterminate = false',
              'max = 100; progress = 0; isIndeterminate = false')

old_progress = '''// Requirement #6: the visible counter steps in increments of 50;
                // the engine tracks the exact count internally.
                val shown = if (loaded >= total) total else (loaded / 50) * 50
                progressBar.max = total
                progressBar.progress = shown
                progressLabel.text = "DATE ÎNCĂRCATE: $shown / $total"'''
new_progress = '''// B540 final: expose only a percentage; the monitored universe size remains private.
                val shownPct = ((loaded.toDouble() / total.toDouble()) * 100.0).toInt().coerceIn(0, 100)
                progressBar.max = 100
                progressBar.progress = shownPct
                progressLabel.text = "PROGRES: $shownPct%"'''
s = s.replace(old_progress, new_progress)
s = s.replace('"Maxim țintă: 20 secunde"', '"Maxim țintă: 45 secunde"')
s = s.replace('(${progress.loaded} / ${progress.total} simboluri primite).', 'Datele OHLCV necesare nu au fost primite.')
s = s.replace('02.09.2026 16:00', '01.09.2026 16:00')
p.write_text(s, encoding='utf-8')

e = Path('app/src/main/java/ro/alintudor/oracle/core/OracleGrowthEngine.kt')
es = e.read_text(encoding='utf-8')
es = es.replace('private const val TOTAL_BUDGET_NANOS = 19_000_000_000L // 1s buffer under the 20s target',
               'private const val TOTAL_BUDGET_NANOS = 45_000_000_000L // hard 45s overall target')
es = es.replace('private const val TOTAL_BUDGET_NANOS = 44_000_000_000L',
                'private const val TOTAL_BUDGET_NANOS = 45_000_000_000L')
e.write_text(es, encoding='utf-8')

# Fail loudly if the requested final state was not produced.
ms = p.read_text(encoding='utf-8')
if 'PROGRES: $shownPct%' not in ms or 'Maxim țintă: 45 secunde' not in ms:
    raise SystemExit('B540 loader patch was not applied')
if '45_000_000_000L' not in es:
    raise SystemExit('B540 45s budget was not applied')
print('B540 final loader patch: 45s hard budget + percentage-only progress + private universe size')
