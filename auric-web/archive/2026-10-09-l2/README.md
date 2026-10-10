# Archived 2026-10-09, Lane 2

## `reel_look_blanket_digit.css`

Moved out of `auric-web/src/styles/reel-look.css`.

The retired rule painted every `.digit-cut`, including the timer readout plaque, with `--reel-number` as soon as the stylesheet loaded. At the default (no custom number color) that plaque is terminal green, and the baseline screenshot budget is 0.5% outside the laser row. The live rule now colors only the selected reel numerals, and the plaque only after a custom number color is committed.
