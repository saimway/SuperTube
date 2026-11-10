# SuperTube v3.0.5

SuperTube v3.0.5 introduces full TV shows streaming directly inside the Movies section with direct embed controls, season and episode switching, recently watched shelves with history deletion, Shorts bottom navigation tap-to-refresh with right-edge thumb ergonomics, and extensive recommendation algorithm tuning to eliminate regional song dumps and repetition on refresh.

## Highlights

- **TV Shows in Movies section**: Integrated TV show streaming catalog with season chips and episode lists with preview thumbnails.
- **Direct Embed Controls**: TV show player loads in an unblocked 16:9 portrait embed at the top of the screen (or horizontal fullscreen), allowing direct touch interaction with quality switchers, subtitle selections, audio tracks, and scrubbing.
- **Recently Watched Shelves & History Deletion**: Replaced top continue watching banner with a clean 1st shelf row for both Movies and TV Shows. Users can remove individual titles (`✕`) or clear all watch history with a confirmation dialog.
- **Tuned Recommendations & No Repetition**:
  - Removed bare generic music queries that triggered unwanted regional pop/movie song dumps.
  - Expanded starter queries into 16 diverse high-quality topics that rotate on every generation.
  - Prioritizes fresh unseen videos at the top of the Home feed on refresh.
  - Shorts refresh records all displayed shorts as seen and rotates discovery queries to guarantee new content.
- **Shorts Navigation Tap-to-Refresh**: Tapping the Shorts navigation icon while scrolled down returns to the top short; tapping while at the top triggers a feed refresh with silent 3-stage exponential-backoff retries.
- **Shorts Right-Edge Thumb Controls**: Action buttons (like, comments, save, share, more) sit flush against the right bezel with semi-transparent buttons for thumb comfort and maximum video visibility.
- **Sleeping Lifecycle Discipline**: TV and movie network operations remain completely dormant until the section is actively opened, aborting instantly on exit.
