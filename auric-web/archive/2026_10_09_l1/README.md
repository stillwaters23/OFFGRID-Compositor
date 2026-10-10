# Archive 2026_10_09_l1

These files moved out of `auric-web/src/styles/` on 2026-10-10. The directory was renamed from `archive/2026-10-09-l1/` to `archive/2026_10_09_l1/` so the path has no hyphens.

| Archived file | Previous path | Why |
|---|---|---|
| `styles/reel_look.css` | `src/styles/reel-look.css` | One-line ownership comment for Lane 2. HR review forbids a comment-only file. Lane 1 does not own reel-look styles. The archived name uses an underscore so the new path is not hyphenated. |
| `styles/folders.css` | `src/styles/folders.css` | One-line ownership comment for Lane 3. Same reason. |
| `styles/clicker.css` | `src/styles/clicker.css` | One-line ownership comment for Lane 4. Same reason. |

The three `@import` lines were removed from `src/styles.css` so the build does not import files that are no longer there. Lane 1 laser rules stay in `src/styles.css` (`.laser-strip`). Lanes 2, 3, and 4 can add their own stylesheets when those lanes land.
