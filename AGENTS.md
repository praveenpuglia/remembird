# CLAUDE.md - Remembird Project Guide

## Project Overview

Remembird reads the dates saved on the user's contacts (birthdays, anniversaries, custom dates) and shows what's coming up, sorted into buckets. Sibling of [Clean SMS](https://github.com/praveenpuglia/clean-sms): same principles (clear, private, non-invasive, simple), a little more fun.

**Min SDK**: 33 (Android 13) | **Target SDK**: 36 | **Compile SDK**: 37

## Tech Stack

- **Kotlin Multiplatform** 2.4.21, Gradle 9.8.1 (Kotlin DSL), AGP 9.4.1, daemon on JDK 21 (`gradle/gradle-daemon-jvm.properties`)
- **`:shared`** (`com.android.kotlin.multiplatform.library`): platform-free logic in `commonMain`, targets Android + iOS (`iosArm64`, `iosSimulatorArm64`). Dates via kotlinx-datetime.
- **`:app`**: the Android app. Jetpack Compose with Material 3, styled as a scrapbook (see Look & feel).
- **iOS app**: not started. When it is, the UI moves into `:shared` with Compose Multiplatform (same `androidx.compose.*` packages) and contacts/notifications get `expect`/`actual` implementations.

## Project Structure

```
shared/src/commonMain/.../Occasion.kt   # Occasion model, buckets, next-date / years maths
app/src/main/java/com/praveenpuglia/remembird/
├── MainActivity.kt                     # Permission flow, loads contacts, owns screen state
├── ContactOccasions.kt                 # ContactsContract Event query + START_DATE parsing
└── ui/                                 # Compose screens and theme
```

## Commands

```bash
./gradlew installDebug && adb shell am start -n com.praveenpuglia.remembird/.MainActivity
./gradlew testDebugUnitTest :shared:testAndroidHostTest   # JVM tests (pre-commit runs these)
./gradlew :shared:iosSimulatorArm64Test                   # shared tests on iOS (macOS + Xcode)
scripts/signoff.sh                                        # local CI signoff, see below
```

## Project Principles

### Privacy & Permissions
- Permissions: `READ_CONTACTS` only. Add `POST_NOTIFICATIONS` when reminders land. **No `INTERNET` permission**; adding one needs a written reason in the PR.
- Read contacts on demand; don't copy contact data into app storage unless a feature needs it. `allowBackup` stays off while the app stores nothing.
- Logging: no PII (no names, dates or contact IDs in logs).
- Every permission must degrade gracefully: denied → explain, permanently denied → offer settings.

### Logic placement
- Anything that doesn't touch a platform API (date maths, bucketing, sorting, formatting rules) goes in `:shared` `commonMain` with a `commonTest`. Keep `commonMain` free of `java.*`.
- Platform code stays thin: query, map to `Occasion`, hand over.
- Dates without a year are first-class (`Occasion.year == null`). Feb 29 is marked on Feb 28 in common years.

### Look & feel: the scrapbook
Unlike Clean SMS, Remembird is meant to be fun, so it has its own visual identity instead of dynamic color:
- Kraft paper background, ink text, pink highlighter, red pen. Always light, like paper. Colours live in `Paper` (`ui/theme/RemembirdTheme.kt`).
- Caveat (handwriting) for the title, names, months and dates; Nunito for small text. Both bundled in `res/font`.
- Months are strips of washi tape in their ritu's colour; the timeline is a dashed pen line in that colour; avatars are tilted polaroids; today's date is circled in red pen.
- Material 3 components underneath, restyled through the theme where possible.

### UX
- Composables are stateless: the Activity owns permissions and loading, screens render state and emit callbacks.
- Accessibility: 48dp touch targets, content descriptions on icon-only buttons, large font scaling.
- Strings in `strings.xml` with placeholders and plurals; never concatenate user data with static text.

### Code Style
- Idiomatic Kotlin, catch specific exceptions, close cursors with `use { }`.
- [Conventional Commits](https://www.conventionalcommits.org/) (the release workflow derives the version bump from them).

## Local CI signoff
GitHub Actions never run automatically. Run checks locally and post them as `signoff/unit` and `signoff/emulator` (needs `gh extension install basecamp/gh-signoff`, a running emulator, a clean tree and a pushed HEAD):
```bash
scripts/signoff.sh            # both
scripts/signoff.sh unit       # unit tests (JVM + iOS simulator) + lint
```

## Release Process
Manual: **Actions → Release → Run workflow**, pick a bump (`auto` reads Conventional Commits). It bumps `app/build.gradle.kts`, builds the signed APK + AAB, tags `vX.Y.Z` on `main`, publishes a GitHub Release and optionally uploads to Play. Needs the same secrets as Clean SMS: `RELEASE_TOKEN`, `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_PASSWORD`, `KEY_ALIAS`, `SERVICE_ACCOUNT_JSON`. Create a signing key with `./generate-keystore.sh`.
