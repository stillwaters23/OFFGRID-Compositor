# Terminal Plain Language Rule

**Project:** OFFGRID-Compositor  
**Status:** Rule for every coding agent. Nothing is built yet.  
**Updated:** 2026-10-06 (America/Chicago)

---

## The rule

Everything the terminal says to Royalty is written as if it's talking to a 13-year-old who is new to the cyber world and doesn't know technical words. That includes typewriter guidance lines, IP leak alerts, status messages, error messages, and anything the terminal speaks out loud.

## How to write it

1. Use simple, everyday words.
2. Keep sentences short, about 12 words or fewer.
3. Say what's happening, then what to do next.
4. Spell out every acronym the first time, and give it a one-line meaning. For example: "VPN (virtual private network) hides where you are."
5. Don't use an advanced term without a one-line definition. Better yet, skip the term.
6. Be calm and clear, not scary, even in a red alert.
7. Keep the meaning true. Plain words never hide a real risk.

## Examples

| Don't write | Write |
|---|---|
| WebRTC leak detected. | Your real internet address is showing. Turn on Stealth mode now. |
| DNS leak detected. | Your phone is asking for websites the unsafe way. Turn on Stealth mode now. |
| IPv6 leak detected. | A second internet address is showing. Turn on Stealth mode now. |
| Geo mismatch: exit node not PH. | You look like you're in the wrong country. Pick a Philippines server. |
| IP masked. | You're hidden. Your real internet address is safe. |
| RTMP ingest failed. | Your stream didn't connect. Check your stream key and try again. |
| Platform not supported in native client. | This doesn't work in the app. Use the website instead. |

## Where it applies

- The folder guidance panel (feature 19)
- The IP leak check and its alerts (feature 25)
- The platform test toggles and their results (feature 15)
- Every status line, label, and spoken message in the terminal panel (feature 13)

## HR Manager check

Any terminal text with unexplained jargon or a bare acronym is a **FAIL**, and the fix list must include a rewrite.
