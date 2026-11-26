# CINEMA TV — Android Studio project

CINEMA TV is a clean-start Android app project for phones, tablets, Android TV and Google TV. Its aim is to keep the family-facing experience simple: open the app, browse real catalogue information, and only show an in-app play action when a source passes the app's conservative checks.

## Current implementation

### Implemented in source

- Kotlin + Jetpack Compose + Material 3 application shell.
- Five main destinations: Home, Movies, Series, Favorites and Settings.
- TVmaze public API integration for real series catalogue data, fuzzy series search, series detail data and episode lists.
- Internet Archive Advanced Search integration for movie records. The client then checks the item's `licenseurl` against an explicit allow-list, rejects certain contradictory rights statements, locates an MP4 file in the item's metadata, and makes a `HEAD` request to check that the media URL responds like a video file before displaying **Watch now**.
- Media3 ExoPlayer playback through a `MediaSessionService`, with player controls and locally saved resume progress.
- Room persistence for favorites and Continue Watching, including a visible progress bar and a remove action for saved playback entries.
- Preferences DataStore for theme, language, Liquid Glass, reduced motion and resume preferences. Reduced motion is enabled by default; size-change animations are only enabled when it is turned off.
- English, French and Arabic resource sets; Android per-app language support is declared.
- TV launcher category and focusable/clickable Compose elements for remote use. The current TV experience reuses the responsive Compose layout; a fully separate 10-foot TV design has not been completed.
- No account, provider login, manual server, playlist or API-key setup in the user interface.
- Internet-only app permission, HTTPS requests, and cleartext traffic disabled.
- A unit test for the licence allow-list.

### Intentionally not presented as available

- **TVmaze is metadata, not a video source.** Series pages and episodes are shown as catalogue information only; they do not get a fake Play button.
- **TMDB is not connected.** No TMDB credential or developer backend was supplied, so the project does not fabricate a movie database integration.
- **JustWatch partner availability is not connected.** JustWatch's partner API documentation says partner integrations require a contract and a unique partner token. None was supplied. Provider offers and country-specific streaming availability are therefore not shown.
- Internet Archive content is not automatically public domain. Only records carrying a licence URL from the project's narrow allow-list and a candidate MP4 file that passes a basic network check are considered. A metadata claim can still be inaccurate, and a successful `HEAD` request does not prove that playback will initialize or that every jurisdiction treats the work identically. The app also re-checks the allow-listed licence and the media URL immediately before starting or resuming playback. The player handles playback failures. Review rights before commercial distribution.
- Movie selection can be empty. An empty state is preferable to made-up posters or unauthorized streams.
- Home does not yet include independently sourced “popular”, “top rated”, “new releases”, family ratings or Tunisian provider recommendations. The current series list is a catalogue feed, not a popularity ranking.
- Downloaded/offline playback is not implemented.

## Build environment

Required on the build machine:

- Android Studio with Android SDK Platform 36 and Build Tools installed.
- JDK 17 or newer (JDK 21 is suitable).
- Network access for Gradle and Maven dependencies on the first build.

Open this folder in Android Studio and allow Gradle sync. Then use **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

From a terminal on a connected machine, run:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

On Windows, run `gradlew.bat` with the same task names.

### Note about the Gradle launcher

This generation environment had no installed Gradle, no Android SDK and no network access, so it could not generate or verify the official `gradle-wrapper.jar`. The included `gradlew` / `gradlew.bat` is a small project bootstrapper: it uses a locally installed Gradle 8.13 if present, otherwise downloads the official Gradle 8.13 distribution and invokes it. It is **not** the official Gradle Wrapper JAR implementation. The Android Studio project can still be imported and built on a machine with the Android SDK and network access.

## Integrations and attribution

- Series catalogue: [TVmaze API](https://www.tvmaze.com/api). TVmaze's public API is licensed under CC BY-SA; the app includes a TVmaze credit and links series details back to TVmaze. Preserve attribution and comply with the applicable ShareAlike terms when distributing derived catalogue data.
- Archive catalogue and candidate media: [Internet Archive](https://archive.org/). The uploader-provided licence metadata is used as a filter, not as a legal guarantee.
- Media playback: [AndroidX Media3](https://developer.android.com/media/media3/exoplayer/hello-world).
- JustWatch partner integration information: [JustWatch Content Partner API](https://apis.justwatch.com/docs/api/). A partner contract and token are required before the integration can be implemented.

## Export / build verification status

This archive is a genuine ZIP containing the Android Studio source tree. In the current execution environment I could inspect and package the project files, but I could not run Android Gradle Plugin, Kotlin compilation, lint, instrumentation tests, release assembly, or create an APK because Android SDK components and network access are absent. The included unit tests are source files only and have not been executed here.

The separate **Download Project** button described in the original request belongs to an Arena AI workspace UI. This environment does not expose that workspace's button or export API; this ZIP is the available project export.

## Known gaps in this source build

- A dedicated Android TV 10-foot layout, focus-path audit and remote-device test remain unfinished; the current responsive Compose UI is also registered as a Leanback launch target.
- Popularity rankings, critic ratings, parental-content classifications and curated family filters are not independently sourced. The app does not invent them.
- Cast lists, subtitles/audio preference matching, a custom playback-speed selector, offline downloads and a storage-usage summary are not yet implemented. The standard Media3 player supplies its supported basic playback controls and available track selection.
- The catalogue is not persisted offline; only favourites, playback progress and settings are stored locally.
- Internet Archive licence metadata is uploader-supplied. The local allow-list and media-response checks reduce risk but are not a legal determination, proof of ownership, or a guarantee that playback succeeds on every device/network.

For a standard Gradle Wrapper JAR, run `gradle wrapper --gradle-version 8.13` from this project with Gradle 8.13 installed. The included launch scripts are provided as a bootstrap alternative and are not a substitute for the official wrapper implementation.
