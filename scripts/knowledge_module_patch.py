from pathlib import Path

# Knowledge is already wired into the current MainActivity/source tree.
# This build step is intentionally idempotent: it must never fail merely because
# an earlier successful patch changed the exact source formatting.
p = Path('app/src/main/java/ro/alintudor/oracle/MainActivity.kt')
s = p.read_text()
required = [
    'OracleKnowledgeSync',
    'OracleKnowledgeModule',
    '"knowledge"->OracleKnowledgeModule',
    'OracleKnowledgeSync.scheduleDaily(this)',
]
missing = [x for x in required if x not in s]
if missing:
    raise SystemExit('Knowledge source is incomplete: ' + ', '.join(missing))

# The dedicated sync engine, UI and boot receiver are committed source files.
for path in [
    'app/src/main/java/ro/alintudor/oracle/core/OracleKnowledgeSync.kt',
    'app/src/main/java/ro/alintudor/oracle/nativeui/OracleKnowledgeModule.kt',
]:
    if not Path(path).exists():
        raise SystemExit('Missing Knowledge source file: ' + path)

print('Knowledge module patch verified (idempotent)')
