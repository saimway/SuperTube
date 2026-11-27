# SuperTube v3.0.8

SuperTube v3.0.8 delivers a comprehensive upgrade to the video, Shorts, and Music recommendation engines alongside stability, memory, and responsiveness optimizations.

## Highlights

- **Search Pollution Immunity (`FlowNeuroEngine`)**:
  - Utility and connectivity/display test queries (such as `"4k video"`, `"8k hdr"`, `"wifi speed test"`) no longer hijack your Home or Shorts feed.
  - Normal searches require genuine watch follow-through to promote topics broadly, decaying isolated search-only terms within 48 hours and preserving feed variety across all of your interests.
- **Watch-Depth & Instant-Skip Learning**:
  - Video affinity updates now scale smoothly with completion depth instead of treating shallow clicks like full watches.
  - Immediately skipping videos, Shorts, or music tracks builds decaying negative language avoidance signals (including Bengali, Hindi, Tamil, Telugu, and transliterated regional markers) so unwanted languages drop out quickly across Home, Shorts, Music Radio, Up Next, Quick Picks, and Daily Discover.
- **Real-Time Shorts Adaptation**:
  - Fixed fast swipe-aways during Shorts buffering (<4s with unknown player duration) being misclassified as 100% watched.
  - Sub-second swipe-aways now fire abandon signals and trigger immediate background re-ranking and pruning of the unviewed Shorts tail queue (`ShortsQueueController` / `ShortsFeedPager`) without touching what is currently on screen.
- **Music Radio, Up-Next & Variant Deduplication (`MusicBrain`)**:
  - Radio, Quick Picks, Related/Up Next, Daily Discover, Speed Dial, and playlist suggestions now canonicalize titles to filter out currently playing songs and duplicate/unrequested fan edits (slowed+reverb, sped-up, nightcore, 8D audio, bass-boosted, loop edits).
  - Early music skips (<25% played, including fast skips while buffering) now record song/variant cooldowns and language penalties and immediately prune matching queued automix tracks.
  - Freshness-oriented surfaces (`Radio`, `Similar`, `Discover`, `Daily Discover`) suppress recently played tracks and diversify thin radio mixes using related artist seeds.
- **Efficiency & Platform Cleanups**:
  - Removed global mutex contention on `MusicCache` reads and added expiration cleanup plus bounded capacity limits.
  - Eliminated repeated per-call `Json` serializer allocations in `YouTube` InnerTube calls and replaced hardcoded year queries in `YouTubeMusicService` with dynamic `Year.now().value`.
