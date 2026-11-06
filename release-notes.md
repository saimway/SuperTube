# SuperTube v3.0.3

SuperTube v3.0.3 brings major streaming stability improvements, seamless movie playback resuming without white screen interruptions, verified update signing for zero-friction in-app updates, and interface refinements.

## Highlights

- Seamless in-app package updates: matching release certificates ensure one-tap updates over existing installs without suspicious package warnings or signature mismatch errors.
- Movie playback resume: movies now remember your exact progress and last working server, offering instant "Resume" or "Play from beginning" options.
- Zero white screens during seek: fixed video buffer abortions and subresource loading so movie scrubbing and seeking play continuously without going white or freezing.
- Clean update screen: simplified the update interface and updated changelogs.

## Movie Playback & Streaming

- Fixed seek cancellations (`ERR_ABORTED`) being erroneously treated as server failures, eliminating white screen stalls when scrubbing movies.
- Enforced hardware black canvas background on the movie player so seeking and buffering transitions remain completely seamless.
- Intelligent playback persistence: stores playback timestamp and confirmed working server per movie.
- Single persistent player instance: entering and exiting fullscreen landscape mode preserves the active stream without reloading or stuttering.
- Multi-server fallback engine: automatically cycles through all available streaming servers with instant failover if any mirror is unavailable.
- Ad-blocking sandbox: blocks video-overlay ads, redirect popunders, and trackers while permitting all genuine video segments and subtitle streams.

## In-App Updates & Maintenance

- Release signing certificate aligned with the installed app fingerprint to guarantee standard Android package manager upgrade compatibility.
- Streamlined update menu to focus on version skipping and updates.
- Refreshed in-app update notes parser for clearer release summaries.
