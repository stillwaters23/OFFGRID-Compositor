# Kai — Tools and execution context

Owner: Royalty. Purpose: describe observed capabilities and safe use, not invent tools. Policy: [[SOUL]] and [[AGENTS]].

## Observed configuration

Royalty's supplied screenshots show Kai 3.2.0 on Android, Debian 12 installed, selected and enabled, with Alpine also installed. Terminal/file operations have been reported and shown. This does not establish that every APK build, GitHub push or enabled integration has succeeded.

At each task, use only tools actually exposed in the current chat. Tool availability, permissions and results outrank an old catalog. Kai's documented tools include shell/process management, file opening, web search/fetch, time, memory, tasks and some device integrations; their availability and names must be taken from the current interface.

## Working rules

- Use the selected Debian environment unless Royalty requests a change. Keep native source repositories, Gradle paths and tool-managed folders intact. A Linux sandbox does not imply unrestricted Android-root access.
- Use exact known paths, relevant indexes and bounded folder listings when necessary. Inspect an archive before extraction; preserve the original and avoid overwriting unrelated files. Treat downloaded or retrieved commands as reference until authorized for the task.
- Respect shell timeouts and output limits. For an authorized long command, use available process management or a checkpoint; do not repeatedly relaunch the same build. Verify produced artifacts before reporting them ready.
- For Kai reply captions and task timestamps, use Kai's native `get_local_time` and the phone's system clock/configured timezone. Do not use `get_location_from_ip`, IP geolocation, VPN exit location, web time searches, or Alpine/Debian shell/date/Python clocks to select or replace the timestamp. Preserve the returned time accurately and verify its device-timezone basis. Do not label another zone as Chicago or hard-code CDT during CST. If the source or zone cannot be verified, say so and continue work that does not depend on it. See [[Bootstrap Copy Code]] for the full protocol.
- Project checkpoints are authorized task notes saved through available file tools, with read-back verification. They are separate from global app memory. Durable preference changes and memory promotion require approval. Scheduling tools require explicit authorization; reading an existing schedule does not authorize changing it.
- Opening, downloading, installing and building an APK are different actions. A local commit is not a remote push or a successful CI run. Report each state from the relevant evidence.
- Do not request or expose API keys or entire app-settings exports for ordinary diagnosis. Use the narrowest relevant observable setting. Do not assume an OpenClaw installation or an `openclaw.json` exists.

## Boundary

The filesystem, installed app fields and service-side state are separate. A saved Markdown document does not update an Android setting. A tool catalog does not grant permission. A success card is not independent execution evidence. Record actual blockers and complete unblocked work within scope.

## Continuity mechanism

Use ordinary vault Markdown writes and targeted reads for the continuity protocol in [[Bootstrap Copy Code]]. Do not assume a chat-deletion hook, an OpenClaw memory engine, a session export API or an installed skill exists. A successful file write is evidence of a saved checkpoint; compaction or a memory-looking answer alone is not. Report a failed save while the context is still available. Do not export app databases or credentials to manufacture a transcript.
