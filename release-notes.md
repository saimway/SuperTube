# SuperTube v3.0.4

SuperTube v3.0.4 introduces a hardened anti-pop-ad engine for movies, full multi-scheme release signing compliance to eliminate Play Protect scan requirements, and continuous playback stability.

## Highlights

- Play Protect & installation fix: release APK is signed with the official release certificate across v1 (JAR), v2, and v3 schemes, eliminating "Scan required by Play Protect" prompts and install failures.
- Zero pop ads inside movie player: comprehensive 5-layer ad-blocking sandbox preventing popup windows, ad redirects, invisible click-trap overlays, and intrusive banners.
- Seamless movie playback: smooth seeking without white screens or freezes, plus automatic resume memory per title.
- Reliable in-app updater: one-tap in-app update checks and downloads with matched signatures.

## Movie Playback & Anti-Ad Shield

- Hardened main-frame navigation lockdown: strictly confines the player to verified movie streaming providers, instantly dropping external ad redirects before they can load inside the player.
- Defused popup & window APIs: blocked `window.open`, popunders, `alert`, `confirm`, and `prompt` dialogs commonly abused by streaming embeds.
- Real-time overlay remover: continuously purges full-screen invisible click-jack layers (`z-index: 999999`) and floating ad banners.
- Expanded ad-blocking filters: expanded blocklist covering popads, propellerads, exoclick, adsterra, and tracking scripts.
- Pure black player canvas: eliminates white screen flashes during seek, server switch, and buffering.
- Smart playback resume: remembers exact playback second and working server per title.

## Security & Maintenance

- Release signing updated with v1, v2, and v3 signature schemes for maximum Android Package Manager compatibility.
- Streamlined update interface with direct download and version skip controls.
