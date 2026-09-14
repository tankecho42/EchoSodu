# Architecture

`MainActivity` contains Home / Game / Statistics / Settings and guided game setup. `Store` owns local progress, backup validation and statistics persistence. `Game` is the Android-independent rules/state engine; `HintEngine` works from board candidates, never from the solution, to explain logical deductions. `GameCodec` validates and serializes current progress.

`BoardView` is the production 81-cell ViewGroup, shared by play and learning. `EffectTimeline`, `RegionMotion` and `VictoryMotion` retain independently timed events. `VictoryOverlay` renders celebration ribbons. `MedalView`, `MedalStyle` and `MedalRadiance` compose individual medal faces, ranks and solar finishes. Effects follow visibility, application preferences and Android animator settings.

## Learning Studio

Both gameplay explanation and Settings gameplay open the non-exported `TutorialActivity`. Its deterministic `TutorialLesson` boards use the same Game, BoardView and HintEngine as play. It never accesses Store. A teaching game exists only in memory or its own saved Activity Bundle. The previous activity remains in the back stack and pauses its timer while covered.

Six chapters cover rules, cell-first input, quick input, candidates/notes, explanations and celebrations. A replay resets only the current teaching board. Touching the board interrupts a demonstration; exiting, backgrounding or losing window focus cancels queued steps. Returning to foreground does not unexpectedly resume an old script. Small screens scroll while the previous/next actions remain fixed.

## Data and privacy

The app needs no network permission, account or analytics SDK. GitHub buttons hand an HTTPS URL to a browser; gameplay and learning work offline. User records stay local and only leave through an explicit file export. Personal backups, signing keys, delivery receipts, original reference images and machine-specific configuration are excluded from this repository.
