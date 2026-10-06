# Credentials Security Rule

**Project:** OFFGRID-Compositor  
**Owner:** Royalty  
**Status:** Hard rule. Applies to every agent and every step. Nothing is built yet.  
**Updated:** 2026-10-06 (America/Chicago)

---

## The rule

No credentials anywhere except the phone's secure storage.

- **Never stored in:** the GitHub repo (any folder, any branch, any commit, any file history), the chat, or any document, including logs, research, screenshots, test results, and the Chat History folder.
- **What counts as a credential:** API keys, passwords, tokens, stream keys, login codes, secret keys, and session cookies. Phone numbers and account logins count too.
- **Where they live:** only in Android's secure storage on the phone. That's the Android Keystore, plus encrypted storage that uses it. The app reads them at run time and never writes them to a file, log, screen, or the outgoing video.

## What agents must do

1. Write code that reads credentials from Android secure storage at run time. Never hard-code them.
2. Use a placeholder name in code and docs, such as `XAI_API_KEY`, never the real value.
3. Never print a credential in the terminal, logs, error messages, crash reports, or test results.
4. Never ask Royalty to paste a credential into chat. Royalty types it straight into the app on the phone.
5. Before every commit, check that no credential is in the change.
6. If a credential lands in the repo by mistake: stop, tell Royalty right away in plain words, log it in `07 Logs and Failures` without the value, and treat the key as burned. Royalty replaces it, because deleting a file doesn't erase git history.

## HR review

Agent 04 (the HR Manager) marks any step an **automatic FAIL** if a credential appears in the repo, the chat, or any document. There are no exceptions and no warnings first.

## Related rules

- `Bootstrap.md`, Security rule 1
- `Agent Lanes and HR Review.md`, Instant FAIL list
- Never change the repository owner's email address or account email.
