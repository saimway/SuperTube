# SuperTube v3.0.7

SuperTube v3.0.7 brings a major overhaul to the recommendation algorithms for Music, Video, and Shorts, complete speed dial and suggestion deduplication, unified search across Movies and TV Shows, TV embed landscape orientation controls, a hardened in-app update checker resolving network errors, and complete home feed refresh and launch reliability.

## Highlights

- **Recommendation Engine Overhaul (FlowNeuroEngine & MusicBrain)**:
  - Upgraded candidate scoring and ranking algorithms to adapt dynamically to user watch history and taste signals.
  - Added Shorts topic-stopping and dwell-time tracking: the algorithm recognizes when users stop or linger on Shorts topics and adapts video and Shorts feeds accordingly.
  - Eliminated repetitive recommendations, providing high-affinity content aligned with actual user engagement.
- **Speed Dial & Music Suggestion Deduplication**:
  - Completely resolved duplicate entries appearing in Speed Dial and Music recommendation carousels when songs are played repeatedly.
  - Enforced strict video ID and normalized `title|artist` signature deduplication throughout the music candidate, normalization, and UI pipelines.
- **Unified Movie & TV Shows Search**:
  - Unified search results across Movies and TV Shows sections: searching from either section now queries both catalogs simultaneously and displays categorized results with Material 3 filter chips ("All", "Movies", "TV Shows").
  - Tapping a TV show search result navigates directly to the TV Shows viewer and player.
- **TV Shows Embed Horizontal Orientation**:
  - Added an embed horizontal / landscape toggle button directly on the TV show player surface, with full support for HTML5 web player full-screen viewports and seamless orientation changes into sensor landscape or portrait mode.
- **Robust In-App Update Checker**:
  - Fixed the network error ("couldn't search for update") when tapping the Check for Updates button in Settings.
  - Added multi-tier release verification with direct GitHub API queries and rate-limit immune fallback redirect checks, ensuring update checks succeed and show release details or confirm that SuperTube is up to date.
- **Feed Reliability & Blank State Fixes**:
  - Fixed feed pull-to-refresh to reload all feed videos rather than only the top card.
  - Resolved cold-start empty states where the feed would occasionally fail to load and remain blank on application launch, with explicit error/empty recovery states.
