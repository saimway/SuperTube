# Project: Flow Recommendation Engine Enhancement & Negative Feedback

## Architecture
Flow's recommendation system integrates on-device neural vectors (`FlowNeuroEngine`), cognitive heuristic scoring (`NeuroScoring`), multi-strategy discovery generation (`NeuroDiscovery`), feed assembly and mixing (`HomeFeedAssembly`, `HomeFeedMixer`), and an event-driven invalidation pipeline (`FeedInvalidationBus`).
This project enhances:
1. **Dynamic Session Intent & Craving Engine**: Tracks real-time session topic clusters, modulates candidate scoring with craving boosts, dynamically shifts feed quotas between familiar favorites (subscriptions/related) and fresh discovery on user fatigue (consecutive skips), and prunes rejected topics.
2. **Negative Feedback Storage & Filtering**: Extends in-engine suppression with reversible dismissal APIs (`unsuppressVideo`), adds `DismissedVideoStore` for metadata and undo capabilities, and suppresses rejected topic patterns from query generation.
3. **UI Feed Integration**: Aligns menu options with YouTube ("Not interested" and "Don't recommend channel"), supports undo snackbars, and ensures immediate UI state removal across Home, Related, and Shorts feeds.
4. **CI Build & Delivery**: Verifies all changes via GitHub Actions CI on `main` branch (avoiding git tags to prevent public releases) and delivers the signed test APK to `/storage/emulated/0/supertube.apk`.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Session Craving Detection | Detect real-time session topic clustering and apply craving boost to candidate scoring | M1 | Survey / R2 |
| 2 | Dynamic Familiarity vs Discovery Dial | Adjust `homeFeedQuotas` and `shortsFeedQuotas` dynamically based on session fatigue (consecutive skips) | M1 | Survey / R2 |
| 3 | Session Topic Pruning on Dislike | Prune `sessionTopicHistory` and `recentInteractions` when a topic is marked "Not interested" | M1 | Survey / R2 |
| 4 | Discovery Query Rejection Filtering | Exclude topics matching active `rejectionPatterns` from `NeuroDiscovery` queries | M1 | Survey / R2 |
| 5 | Dismissed Video Metadata Store | Implement `DismissedVideoStore` (typed DataStore) for dismissed video details and history | M2 | Survey / R3 |
| 6 | Reversible Video Unsuppress API | Implement `unsuppressVideo` in `FlowNeuroEngine` and `VideoFeedbackUseCase` for Undo | M2 | Survey / R3 |
| 7 | Persistent Channel Suppression Loop | Ensure channel blocking completely filters candidates across all feed lanes and query generators | M2 | Survey / R3 |
| 8 | YouTube-Style Menu Labels | Update strings in `strings.xml` to "Don't recommend channel" and "Not interested" | M2 | Survey / R3 |
| 9 | Immediate Shorts Feed Dismissal | Wire `FeedInvalidationBus.Event.NotInterested` into `ShortsViewModel` to drop dismissed shorts immediately | M2 | Survey / R3 |
| 10 | "Not interested" Undo UI Action | Connect `QuickActionUndo.NotInterested` to show reversible snackbar on video dismissal | M2 | Survey / R3 |
| 11 | Push to Main CI Trigger (No Tag) | Push commits to `origin main` without any `v*` git tag to trigger CI without public release | M3 | Survey / R4 |
| 12 | Test APK Download & Delivery | Download signed `supertube-release-apk` to `/storage/emulated/0/supertube.apk` and verify integrity | M3 | Survey / R4 |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | M1: Engine Intent & Craving Adaptation | `FlowNeuroEngine`, `NeuroScoring`, `NeuroDiscovery`, `HomeFeedMixer`, `ShortsFeedQuotas` | none | DONE |
| 2 | M2: Negative Feedback Storage, Filtering & UI | `DismissedVideoStore`, `FlowNeuroEngine`, `VideoFeedbackUseCase`, `QuickActionsViewModel`, `ShortsViewModel`, `strings.xml` | M1 | DONE |
| 3 | M3: CI Verification & Private Delivery | Git push to `main`, GitHub Actions workflow monitor, APK download to `/storage/emulated/0/supertube.apk` | M2 | IN_PROGRESS |

## Interface Contracts
### `FlowNeuroEngine` ↔ `HomeFeedMixer` & `ShortsFeedQuotas`
- `FlowNeuroEngine.getSessionIntent(): SessionIntent`
  - Returns `SessionIntent(consecutiveSkips: Int, activeCravingTopic: String?, cravingIntensity: Float, discoveryBias: Float)`
- `HomeFeedMixer.homeFeedQuotas(intent: SessionIntent, ...): FeedQuotas`
- `ShortsFeedQuotas.shortsFeedQuotas(intent: SessionIntent, ...): ShortsFeedQuotas`

### `FlowNeuroEngine` ↔ `VideoFeedbackUseCase`
- `FlowNeuroEngine.markNotInterested(context: Context, video: Video)`: Marks video suppressed, demotes topic vectors, and prunes from `sessionTopicHistory`.
- `FlowNeuroEngine.unsuppressVideo(context: Context, videoId: String)`: Removes video from `suppressedVideoIds` and relaxes rejection penalties.
- `FlowNeuroEngine.blockChannel(context: Context, channelId: String)`: Permanently suppresses channel.
- `FlowNeuroEngine.unblockChannel(context: Context, channelId: String)`: Unblocks channel.

### `DismissedVideoStore` ↔ `VideoFeedbackUseCase`
- `DismissedVideoStore.record(video: Video)`: Stores `DismissedVideoRecord(videoId, title, channelId, channelName, thumbnailUrl, dismissedAt, topics)`.
- `DismissedVideoStore.forget(videoId: String)`: Removes record on undo.
- `DismissedVideoStore.getAll(): Flow<List<DismissedVideoRecord>>`: Emits list for settings management.

### `VideoFeedbackUseCase` ↔ UI ViewModels
- `VideoFeedbackUseCase.markNotInterested(video: Video)`
- `VideoFeedbackUseCase.undoNotInterested(videoId: String)`
- `VideoFeedbackUseCase.blockChannel(video: Video): BlockedChannel`
- `FeedInvalidationBus.Event.NotInterested(videoId, channelId)`
- `FeedInvalidationBus.Event.ChannelBlocked(channelId, videoId)`

## Code Layout
- Engine & Scoring: `app/src/main/java/io/github/aedev/flow/data/recommendation/`
  - `FlowNeuroEngine.kt`
  - `NeuroScoring.kt`
  - `NeuroDiscovery.kt`
  - `FeedExclusions.kt`
  - `DismissedVideoStore.kt`
- Feed Mixers:
  - `app/src/main/java/io/github/aedev/flow/ui/screens/home/HomeFeedMixer.kt`
  - `app/src/main/java/io/github/aedev/flow/data/shorts/feed/ShortsFeedQuotas.kt`
  - `app/src/main/java/io/github/aedev/flow/data/shorts/feed/ShortsFeedMixer.kt`
- Feedback & Bus: `app/src/main/java/io/github/aedev/flow/data/engagement/`
  - `VideoFeedbackUseCase.kt`
  - `FeedInvalidationBus.kt`
- UI Components:
  - `app/src/main/java/io/github/aedev/flow/ui/components/shared/quickactions/VideoQuickActionsSheet.kt`
  - `app/src/main/java/io/github/aedev/flow/ui/components/shared/quickactions/QuickActionsViewModel.kt`
  - `app/src/main/java/io/github/aedev/flow/ui/components/shorts/ShortsSettingsSheet.kt`
  - `app/src/main/java/io/github/aedev/flow/ui/screens/shorts/ShortsViewModel.kt`
- String Resources: `app/src/main/res/values/strings.xml`
- CI/CD Configuration: `.github/workflows/build.yml`
