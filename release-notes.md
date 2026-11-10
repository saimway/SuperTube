# SuperTube v3.0.5

SuperTube v3.0.5 introduces full TV shows integration into the Movie section with season and episode switching, automatic resume memory, Shorts bottom navigation tap-to-refresh with silent exponential-backoff retries, and flush right-edge Shorts action buttons for enhanced thumb ergonomics and video visibility.

## Highlights

- TV shows in Movies section: integrated complete TV show catalog from rabbitmeow with season and episode browsing, episode switching drawer, and seamless resume memory.
- Shorts navigation tap-to-refresh: tapping the Shorts navigation icon while scrolled down returns to the top short; tapping while at the top triggers a feed refresh.
- Silent Shorts refresh & retry: automatic 3-stage retry loop with exponential backoff on feed refresh, completely suppressing error dialogs and preserving the active queue.
- Shorts flush right-edge layout: action buttons (like, comments, save, share, more) sit flush against the right bezel with clean semi-transparent buttons for thumb comfort and maximum video visibility.
- Sleeping lifecycle efficiency: TV shows and movie network operations remain completely dormant until the section is actively opened, aborting instantly on exit.

## Shorts Experience

- Bottom navigation reselect: seamless parity with Home tab navigation — single tap scrolls to top, second tap fetches fresh recommendations.
- Resilient background retry: handles transient network hiccups without clearing the loaded queue or popping error dialogs.
- Right-edge button alignment: reduced edge padding to 0dp (with 2dp curved screen buffer), freeing the central 80%+ video area.
- Ergonomic semi-transparent controls: Material 3 CircleShape pill buttons styled with `PlayerScrimAffordance` for effortless one-handed thumb interaction.

## TV Shows & Movie Streaming

- Complete TV catalog: browse trending, popular, and top-rated TV shows.
- Season & episode switching: in-player episode switcher drawer and detailed season lists with thumbnail previews.
- Smart resume memory: tracks exact playback position per TV show episode and movie title.
- 7-layer anti-redirect shield: full popup ad blocker and sandboxed playback across all TV and movie streaming mirrors.
