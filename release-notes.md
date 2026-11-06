# SuperTube v3.0.5

SuperTube v3.0.5 resolves Play Protect installation warnings by removing high-risk permissions, eliminates all external repository links from the update and changelog screens, and features a hardened anti-pop-ad engine for completely ad-free movie playback.

## Highlights

- Play Protect installation fix: removed high-risk `MANAGE_EXTERNAL_STORAGE` permission, eliminating Play Protect scan requirements and installation blocks.
- Zero pop ads inside movie player: enhanced multi-layer anti-ad shield blocking all popup windows, redirect clicks, transparent trap overlays, and in-player banners.
- Clean update interface: removed all external repository buttons, links, and mentions from the update and changelog views.
- Seamless movie playback: smooth seeking without white screens or freezes, plus automatic resume memory per title.

## Movie Playback & Anti-Ad Shield

- Hardened navigation lockdown: strictly confines the player to verified movie streaming providers, dropping external ad redirects instantly.
- Defused popup & window APIs: blocked `window.open`, popunders, `alert`, `confirm`, `prompt`, and synthetic click hijacking.
- Real-time overlay & banner remover: continuous DOM observer instantly purging click-trap layers (`z-index >= 1000`) and floating ad banners.
- Comprehensive ad-server blocklist: expanded filters covering 60+ ad networks and tracker scripts.
- Pure black player canvas: eliminates white screen flashes during seek, server switch, and buffering.
- Smart playback resume: remembers exact playback second and working server per title.

## Security & Maintenance

- High-risk permission cleanup: removed `MANAGE_EXTERNAL_STORAGE` to ensure safe, warning-free installations.
- Clean update interface: removed external repository links from Changelog and Update views.
- Multi-scheme release signing with genuine certificate for Android Package Manager compatibility.
