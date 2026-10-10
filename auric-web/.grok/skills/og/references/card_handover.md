# Share card hand-over

Write the finished 1200×630 JPEG to a temp under `/workspace/.grok/`, then move
it onto `public/og.jpg` in one rename. The temp must stay outside `public/`.

```text
node scripts/write-atomic.mjs /workspace/.grok/og.jpg.tmp public/og.jpg
```
