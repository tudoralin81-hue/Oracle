from pathlib import Path
import hashlib

# B540: the Claude ZIP is the source of truth. This script reproduces the
# exact Claude GROWTH snapshot at build time. START and frozen non-GROWTH
# files are never modified.

M = Path('app/src/main/java/ro/alintudor/oracle/nativeui/OracleGrowthModule.kt')
s = M.read_text(encoding='utf-8')

old_progress = '''// Requirement #6: the visible counter steps in increments of 50;
                // the engine tracks the exact count internally.
                val shown = if (loaded >= total) total else (loaded / 50) * 50
                progressBar.max = total
                progressBar.progress = shown
                progressLabel.text = "DATE ÎNCĂRCATE: $shown / $total"'''
new_progress = '''// B540 update: only a percentage is ever shown — the raw counts
                // (and so the size of the monitored universe) stay internal to
                // the engine and are never rendered in the UI.
                val pct = ((loaded * 100.0) / total).toInt().coerceIn(0, 100)
                progressBar.progress = pct
                progressLabel.text = "DATE ÎNCĂRCATE: $pct%"'''
s = s.replace(old_progress, new_progress)
M.write_text(s, encoding='utf-8')

E = Path('app/src/main/java/ro/alintudor/oracle/core/OracleGrowthEngine.kt')
es = E.read_text(encoding='utf-8')
es = es.replace(
    'private const val TOTAL_BUDGET_NANOS = 19_000_000_000L // 1s buffer under the 20s target',
    'private const val TOTAL_BUDGET_NANOS = 44_000_000_000L // 1s buffer under the 45s target'
)
es = es.replace(
    'private const val SCAN_BUDGET_NANOS = 13_000_000_000L',
    'private const val SCAN_BUDGET_NANOS = 30_000_000_000L // scaled with TOTAL_BUDGET_NANOS so OHLCV fetch keeps its ~68% share of the run'
)
E.write_text(es, encoding='utf-8')

# Exact Git blob SHA-1 values calculated from Oracle-main-20-growth-loader-update.zip.
expected = {
    str(E): '683eea654306692a0d207cae5109dc4c51897b83',
    str(M): 'd087f7e508daff3f54b3e29eb7cb96892dc08e82',
}
for path, want in expected.items():
    data = Path(path).read_bytes()
    got = hashlib.sha1(f'blob {len(data)}\0'.encode() + data).hexdigest()
    if got != want:
        raise SystemExit(f'Claude snapshot mismatch: {path}: {got} != {want}')

print('B540: Claude GROWTH files reproduced byte-for-byte')
