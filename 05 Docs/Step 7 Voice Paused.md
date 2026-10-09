# Step 7 Voice Paused

**Status:** paused. Nothing in this step is built, on purpose.

Step 7 is the audio pipeline: voice effects, typed-to-speech with the Rex voice preset (feature 8), the read-aloud button (feature 38), and speaking as Rex in streams (feature 39).

It stays paused until Royalty says go. Until then:

- The app has no mic permission. `RECORD_AUDIO` is removed in the manifest with `tools:node="remove"`.
- There is no mic, speech, voice, or text-to-speech code. GitHub's guards fail the build if any appears.
- The `Rex Voice` folder in `06 Prompts and Skills` is not touched.
- The soundboard (feature 6) is built, but its sounds play on the phone's speaker only. They are not added to the video.

When Royalty says go, start from `06 Prompts and Skills/Bootloader.md`, Step 7, and do the research first.
