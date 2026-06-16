# Requirements Document

## Introduction

ConveneMusic currently routes all UI logic through a single `MusicViewModel` (600+ lines) and a singleton `NavigationController` backed by a raw `mutableStateListOf` history stack. The goal of this refactor is to split `MusicViewModel` into three focused ViewModels (`SearchViewModel`, `PlaybackViewModel`, `LibraryViewModel`) and replace the fragile singleton navigation system with a structured, testable `AppNavigator` that supports back-stack integrity and future deep-link readiness — all without introducing a DI framework and without switching to Jetpack Navigation.

## Glossary

- **SearchViewModel**: The new ViewModel responsible for all search, artist browsing, genre browsing, playlist search, and artist thumbnail prefetching state and logic.
- **PlaybackViewModel**: The new ViewModel responsible for playback control, queue management, autoplay, and stream-URL resolution.
- **LibraryViewModel**: The new ViewModel responsible for download management, playback history, local audio scanning, and local video scanning.
- **AppNavigator**: The replacement for `NavigationController`; a class (not a singleton `object`) that owns the navigation back-stack and exposes it as Compose `State`.
- **Destination**: The sealed class that enumerates all navigable screens: `Home`, `SearchList`, `PlaylistDetail`, `Player`, `Library`, `VideoPlayer`.
- **Player**: The full-screen music player overlay, rendered via `AnimatedVisibility` (not pushed as a routed destination in the traditional sense, but still tracked in the back-stack).
- **VideoPlayer**: The full-screen video player overlay, same rendering model as Player.
- **Back-stack**: The ordered list of `Destination` entries tracked by `AppNavigator`.
- **Autoplay**: Automatic generation of recommended songs appended to the queue when the queue nears its end.
- **InnerTubeClient**: The network client that calls YouTube InnerTube APIs for search, playlist, and stream-URL resolution.
- **DownloadRepository**: Repository interface managing offline download persistence and file I/O.
- **HistoryRepository**: Repository interface managing play-history persistence.
- **LocalMediaRepository**: Repository interface for scanning device audio and video files via MediaStore.
- **PlaybackManager**: The class that wraps `MediaController` / ExoPlayer and exposes reactive playback state flows.

---

## Requirements

### Requirement 1: Split MusicViewModel into SearchViewModel

**User Story:** As a developer, I want search, artist browsing, genre browsing, and playlist search logic isolated in a dedicated `SearchViewModel`, so that each file has a single, clear responsibility and is independently testable.

#### Acceptance Criteria

1. THE `SearchViewModel` SHALL expose `searchResults`, `playlistResults`, `playlistTracks`, `artistTracks`, `isArtistLoading`, `artistThumbnails`, `genreTracks`, `isGenreLoading`, `isLoading`, `isPlaylistLoading`, `isLoadMoreLoading` as `StateFlow` properties.
2. WHEN `search(query)` is called with a non-blank query, THE `SearchViewModel` SHALL immediately populate `searchResults` with locally-matched downloaded and scanned tracks, then after a 300 ms debounce fetch remote results from `InnerTubeClient` and merge them, deduplicating by song ID.
3. WHEN `search(query)` is called with a blank query, THE `SearchViewModel` SHALL clear `searchResults`, cancel any in-flight search coroutine, and set `isLoading` to `false`.
4. WHEN `loadMoreSongs()` is called while a `songContinuationToken` is available and `isLoadMoreLoading` is `false`, THE `SearchViewModel` SHALL append the next page of results to `searchResults` and update the continuation token.
5. WHEN `loadArtistTracks(artistName)` is called with a non-blank name, THE `SearchViewModel` SHALL set `isArtistLoading` to `true`, fetch combined local and remote tracks filtered by artist match, set `artistTracks`, then set `isArtistLoading` to `false`.
6. WHEN `clearArtistTracks()` is called, THE `SearchViewModel` SHALL set `artistTracks` to an empty list and clear the `artistContinuationToken`.
7. WHEN `loadGenreTracks(genre)` is called with a non-blank genre, THE `SearchViewModel` SHALL set `isGenreLoading` to `true`, fetch and sort remote tracks by view count, set `genreTracks`, then set `isGenreLoading` to `false`.
8. WHEN `clearGenreTracks()` is called, THE `SearchViewModel` SHALL set `genreTracks` to an empty list and clear `genreContinuationToken`.
9. WHEN `loadPlaylistTracks(playlistId)` is called, THE `SearchViewModel` SHALL set `isPlaylistLoading` to `true`, fetch tracks via `InnerTubeClient`, set `playlistTracks`, then set `isPlaylistLoading` to `false`.
10. WHEN `clearPlaylistTracks()` is called, THE `SearchViewModel` SHALL set `playlistTracks` to an empty list.
11. THE `SearchViewModel` SHALL prefetch thumbnails for famous artists on initialisation by launching individual coroutines per artist, populating `artistThumbnails` without blocking the main thread.
12. IF `InnerTubeClient` throws an exception during any search or load operation, THEN THE `SearchViewModel` SHALL log the error and leave the previous state unchanged, without crashing.
13. THE `SearchViewModel` SHALL accept `downloadedSongs: StateFlow<List<Song>>` and `localTracks: StateFlow<List<Song>>` as constructor parameters (or equivalent observable inputs) so that local-match results always reflect the latest library state without coupling to `LibraryViewModel` internals.

---

### Requirement 2: Split MusicViewModel into PlaybackViewModel

**User Story:** As a developer, I want all playback, queue, and autoplay logic isolated in a dedicated `PlaybackViewModel`, so that playback behaviour can be reasoned about and tested independently of search and library features.

#### Acceptance Criteria

1. THE `PlaybackViewModel` SHALL expose `currentSong`, `isPlaying`, `currentPosition`, `duration`, `queue`, `currentQueueIndex`, `isQueueEndless`, `isQueueLoadingMore`, `isStreamLoading` as `StateFlow` properties delegated from or derived from `PlaybackManager`.
2. WHEN `playSong(song, initialQueue, isPremadePlaylist)` is called, THE `PlaybackViewModel` SHALL update the queue and current index, call `playSongInternal`, and — if the queue is endless — trigger `generateAutoplaySongs`.
3. WHEN `playSongInternal(song)` is called, THE `PlaybackViewModel` SHALL resolve the stream URL (local file path if downloaded, else remote URL from `InnerTubeClient`), fetch artwork bytes concurrently, then call `PlaybackManager.play()`.
4. IF `InnerTubeClient.getStreamUrl()` returns `null`, THEN THE `PlaybackViewModel` SHALL log the failure and leave playback in its current state without crashing.
5. WHEN `playNextSong()` is called and the next index is within the current queue, THE `PlaybackViewModel` SHALL advance `currentQueueIndex` and call `playSongInternal`.
6. WHEN `playNextSong()` is called and the queue is exhausted and `isQueueEndless` is `true`, THE `PlaybackViewModel` SHALL call `generateAutoplaySongs()`, await its completion, then advance to the newly appended track.
7. WHEN `playPreviousSong()` is called and `currentPosition` is at or above 5 000 ms (inclusive), THE `PlaybackViewModel` SHALL seek to position 0 without changing the queue index.
8. WHEN `playPreviousSong()` is called and `currentPosition` is below 5 000 ms and a previous index exists, THE `PlaybackViewModel` SHALL decrement `currentQueueIndex` and call `playSongInternal`.
9. WHEN `generateAutoplaySongs()` is called and `isQueueEndless` is `true` and `isQueueLoadingMore` is `false`, THE `PlaybackViewModel` SHALL append up to 15 non-duplicate recommended tracks to the queue using artist, title, or generic-query fallback searches.
10. WHEN `togglePlayPause()` is called and the player is idle with a known `currentSong`, THE `PlaybackViewModel` SHALL restart playback from `playSongInternal`; otherwise it SHALL toggle pause/resume on `PlaybackManager`.
11. WHEN `seekTo(positionMs)` is called, THE `PlaybackViewModel` SHALL forward the seek to `PlaybackManager`.
12. WHEN `playQueueSong(index)` is called with an index within bounds, THE `PlaybackViewModel` SHALL set `currentQueueIndex` to that index and call `playSongInternal`.
13. WHEN `playPlaylist(playlist)` is called, THE `PlaybackViewModel` SHALL fetch playlist tracks via `InnerTubeClient`, then call `playSong` with the first track, the full track list, and `isPremadePlaylist = true`.
14. WHEN the `PlaybackViewModel` is cleared, THE `PlaybackViewModel` SHALL call `PlaybackManager.release()` to free media resources.
15. THE `PlaybackViewModel` SHALL register `PlaybackServiceConnector.onNext` and `PlaybackServiceConnector.onPrevious` callbacks in `init` to route media-notification button presses to `playNextSong()` and `playPreviousSong()`.

---

### Requirement 3: Split MusicViewModel into LibraryViewModel

**User Story:** As a developer, I want downloads, history, local audio scanning, and local video scanning isolated in a dedicated `LibraryViewModel`, so that offline and device-media concerns are decoupled from search and playback.

#### Acceptance Criteria

1. THE `LibraryViewModel` SHALL expose `historyList`, `downloadedList`, `downloadingSongs`, `downloadingQueue`, `downloadProgress`, `localTracks`, `isScanning`, `localVideos`, `isVideoScanning` as `StateFlow` properties.
2. WHEN the `LibraryViewModel` is initialised, THE `LibraryViewModel` SHALL load history and downloads from their respective repositories and populate the corresponding `StateFlow` properties.
3. WHEN `addToHistory(song)` is called, THE `LibraryViewModel` SHALL prepend the song to the history list, deduplicate by song ID, truncate to 50 entries, persist the result via `HistoryRepository`, and update `historyList`.
4. WHEN `downloadSong(song)` is called for a song that is neither downloaded nor currently downloading, THE `LibraryViewModel` SHALL add the song to `downloadingQueue` and `downloadingSongs`, track per-song progress in `downloadProgress` via `DownloadRepository.download()`, post a progress notification, and on success reload `downloadedList` and post a completion notification.
5. IF `DownloadRepository.download()` returns `false`, THEN THE `LibraryViewModel` SHALL post a failure notification and remove the song from `downloadingSongs`, `downloadingQueue`, and `downloadProgress`.
6. WHEN `downloadSong(song)` is called for a song that is already downloaded or already downloading, THE `LibraryViewModel` SHALL take no action.
7. WHEN `scanLocalFiles()` is called while `isScanning` is `false`, THE `LibraryViewModel` SHALL set `isScanning` to `true`, clear `localTracks`, invoke `LocalMediaRepository.scanAudio()`, populate `localTracks` with the result, and set `isScanning` to `false`.
8. IF `LocalMediaRepository.scanAudio()` throws an exception, THEN THE `LibraryViewModel` SHALL log the error, set `isScanning` to `false`, and leave `localTracks` as an empty list.
9. WHEN `scanLocalVideos()` is called while `isVideoScanning` is `false`, THE `LibraryViewModel` SHALL set `isVideoScanning` to `true`, clear `localVideos`, invoke `LocalMediaRepository.scanVideo()`, populate `localVideos`, and set `isVideoScanning` to `false`.
10. WHEN `clearLocalTracks()` is called, THE `LibraryViewModel` SHALL set `localTracks` to an empty list.
11. WHEN `clearLocalVideos()` is called, THE `LibraryViewModel` SHALL set `localVideos` to an empty list.
12. THE `LibraryViewModel` SHALL create the downloads notification channel on initialisation for Android API 26 and above.

---

### Requirement 4: Replace NavigationController with AppNavigator

**User Story:** As a developer, I want a structured, non-singleton `AppNavigator` class to own the navigation back-stack, so that navigation state is scoped to the Activity lifecycle, is testable, and is not shared as a global mutable object.

#### Acceptance Criteria

1. THE `AppNavigator` SHALL be a class (not an `object`) that exposes `backStack` as `SnapshotStateList<Destination>`, `currentTab` as `MutableState<MusicTab>`, and `activePlaylist` as `MutableState<Playlist?>`.
2. THE `AppNavigator` SHALL be instantiated once in `MainActivity` or at the Compose root and passed down via `CompositionLocal` or constructor parameter, eliminating the global singleton.
3. WHEN `navigateTo(dest)` is called with a `Home`, `SearchList`, or `Library` destination that already exists in the back-stack, THE `AppNavigator` SHALL pop all entries above that destination rather than duplicating it; for all other destination types the back-stack size SHALL NOT be reduced.
4. WHEN `navigateTo(dest)` is called with a `Player`, `PlaylistDetail`, or `VideoPlayer` destination, THE `AppNavigator` SHALL append the destination to the back-stack only if it is not already the top entry; tab destinations (`Home`, `SearchList`, `Library`) are excluded from this append-only rule and are always handled by the pop-to rule in criterion 3.
5. WHEN `goBack()` is called and the back-stack contains more than one entry, THE `AppNavigator` SHALL remove the last entry, sync `currentTab` from the new top entry, and return `true`.
6. WHEN `goBack()` is called and the back-stack contains exactly one entry, THE `AppNavigator` SHALL take no action and return `false`.
7. THE `AppNavigator` SHALL keep `currentTab` and `activePlaylist` in sync after every `navigateTo` and `goBack` call, applying the same sync rules as the current `syncTab` logic.
8. THE `AppNavigator` SHALL initialise the back-stack with a single `Destination.Home` entry and `currentTab` set to `MusicTab.Home`.
9. WHEN `isPlayerOpen` is read, THE `AppNavigator` SHALL return `true` if and only if the last entry in the back-stack is `Destination.Player`.
10. WHEN `isVideoPlayerOpen` is read, THE `AppNavigator` SHALL return `true` if and only if the last entry in the back-stack is `Destination.VideoPlayer`.

---

### Requirement 5: Maintain ViewModel Inter-Communication Without DI

**User Story:** As a developer, I want the three focused ViewModels to share necessary state (e.g., downloaded songs visible to search local-match logic) without introducing Hilt or Dagger, so that the no-DI constraint of the project is preserved while still allowing cross-ViewModel data flow.

#### Acceptance Criteria

1. THE `SearchViewModel` SHALL accept the `downloadedList` and `localTracks` state flows from `LibraryViewModel` as constructor arguments so that local search matches always reflect current library state.
2. THE `PlaybackViewModel` SHALL accept a reference to `LibraryViewModel` (or its relevant repository) so that it can check `isDownloaded(songId)` and resolve local file paths during stream URL resolution.
3. THE `PlaybackViewModel` SHALL call `LibraryViewModel.addToHistory(song)` each time a new song begins playing via `playSongInternal`, tracking all playback in history regardless of source or context (streaming, local file, or playlist).
4. THE `MainUI` composable (or its ViewModel factory) SHALL be responsible for constructing and wiring the three ViewModels together, passing shared dependencies at creation time.
5. WHEN the `MainUI` composable is first composed, THE `MainUI` SHALL create `LibraryViewModel` first, then pass its relevant state flows to `SearchViewModel` and `PlaybackViewModel` constructors, ensuring no circular dependency.

---

### Requirement 6: Preserve All Existing UI Behavior During Refactor

**User Story:** As a user, I want all screens and interactions to behave identically after the refactor, so that the internal restructuring is invisible to me.

#### Acceptance Criteria

1. THE `MainUI` composable SHALL continue to render `SearchScreen`, `HomeScreen`, `LibraryScreen`, `PlayerScreen`, and `VideoPlayerScreen` based on the current `AppNavigator` back-stack state, using the same `AnimatedVisibility` overlay pattern for `Player` and `VideoPlayer`.
2. WHEN the user taps a bottom-bar tab, THE `BottomBarTabs` component SHALL call `AppNavigator.navigateTo()` with the corresponding `Destination`, preserving the existing tab-sync behaviour.
3. WHEN the user presses the system back button, THE system back handler SHALL call `AppNavigator.goBack()` and, if the back-stack is at root, fall back to clearing the search query if one is active.
4. THE mini-player bar SHALL remain visible on non-Home screens whenever `currentSong` is non-null and neither `Player` nor `VideoPlayer` is open, as determined by `AppNavigator.isPlayerOpen` and `AppNavigator.isVideoPlayerOpen`; the mini-player SHALL be hidden when `currentSong` is `null` regardless of player state, and SHALL be hidden whenever the video player is open even if audio continues playing in the background.
5. THE `PredictiveBackHandler` logic in `SearchScreen` for dismissing artist views, genre views, and "view all" panels SHALL remain self-contained within `SearchScreen` and SHALL NOT be moved to `AppNavigator`.
6. WHEN the app is resumed after being killed by the system, THE `PlaybackViewModel` SHALL restore the last-played song from `SharedPreferences` via `PlaybackManager`, matching the existing restore behaviour.

---

### Requirement 7: ViewModel and Repository Construction Without DI Framework

**User Story:** As a developer, I want ViewModels to construct their own repositories or accept them as constructor parameters via a custom `ViewModelProvider.Factory`, so that dependencies are explicit and the project remains free of Hilt/Dagger while still being testable.

#### Acceptance Criteria

1. THE `SearchViewModel` SHALL be constructable via a `ViewModelProvider.Factory` that accepts `Application` and the required `StateFlow` dependencies, or via manual construction with `remember { }` as a fallback if the factory pattern cannot be applied.
2. THE `PlaybackViewModel` SHALL be constructable via a `ViewModelProvider.Factory` that accepts `Application` and a reference to `LibraryViewModel` (or equivalent repositories).
3. THE `LibraryViewModel` SHALL be constructable via a standard `AndroidViewModelFactory` or a custom factory accepting `Application`.
4. THE `MainUI` composable or `MainActivity` SHALL use `viewModel(factory = …)` calls to obtain each ViewModel where a factory is available, or manual construction with `remember { }` where factories are not applicable, passing the wired dependencies in either case so that no ViewModel constructs another ViewModel directly.
5. IF a `ViewModelProvider.Factory` cannot be created for any of the three ViewModels within the existing `AndroidViewModel` pattern, THEN THE implementation SHALL use a manual constructor pattern with `remember { }` in the Compose tree as a fallback, documented with a code comment explaining the deviation; when a factory is successfully used, no such documentation is required.
