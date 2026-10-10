# Site identity hand-over

`src/lib/og/site.json` names the card and, for a game, sets `"type": "x:game"`.
Write the document to a temp first so a build never reads a partial JSON file.

```text
node scripts/write-atomic.mjs /workspace/.grok/site.json.tmp src/lib/og/site.json
```
