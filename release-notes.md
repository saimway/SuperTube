# SuperTube v3.0.4

SuperTube v3.0.4 introduces a hardened anti-pop-ad engine for movies, eliminates Play Protect scan prompts by removing high-risk permissions, removes all external repository links from the app interface, and ensures continuous movie playback stability.

## Highlights

- Play Protect & installation fix: removed high-risk `MANAGE_EXTERNAL_STORAGE` permission and signed with official release certificate across v1, v2, and v3 schemes, eliminating Play Protect scan requirements and install failures.
- Zero pop ads inside movie player: comprehensive multi-layer ad-blocking sandbox preventing popup windows, ad redirects, invisible click-trap overlays, and intrusive banners.
- Clean update interface: eliminated all external repository buttons and links from the update and changelog views.
- Seamless movie playback: smooth seeking without white screens or freezes, plus automatic resume memory per title.
- Reliable in-app updater: one-tap in-app update checks and downloads with matched signatures.

## Movie Playback & Anti-Ad Shield

- Hardened navigation lockdown: strictly confines the player to verified movie streaming providers, instantly dropping external ad redirects before they can load inside the player.
- Defused popup & window APIs: blocked `window.open`, popunders, `alert`, `confirm`, `prompt`, and synthetic click traps commonly abused by streaming embeds.
- Real-time overlay remover: continuously purges full-screen invisible click-jack layers and floating ad banners.
- Expanded ad-blocking filters: expanded blocklist covering 60+ ad networks and tracking scripts.
- Pure black player canvas: eliminates white screen flashes during seek, server switch, and buffering.
- Smart playback resume: remembers exact playback second and working server per title.

## Security & Maintenance

- High-risk permission cleanup: removed `MANAGE_EXTERNAL_STORAGE` to ensure safe, warning-free installations.
- Clean update interface: removed external repository links from Changelog and Update views.
- Release signing updated with v1, v2, and v3 signature schemes for maximum Android Package Manager compatibility.
