# Deployment — Firebase, Play Console, App Store

Three TeamCity configs deploy the app, each building + uploading via **fastlane**:

| Config | Target | Tool | Agent |
|---|---|---|---|
| `Deploy · Firebase App Distribution` | Android UAT (`.uat`) | **Gradle** `appDistributionUploadUat` | Android SDK |
| `Deploy · Google Play Console` | Android store | fastlane `android play` | Android SDK |
| `Deploy · App Store Connect` | iOS store/TestFlight | fastlane `ios appstore` | **macOS + Xcode** |

Firebase uses the **Firebase App Distribution Gradle plugin** (configured in
[`androidApp/build.gradle.kts`](../androidApp/build.gradle.kts)) — no fastlane.
Play + App Store use fastlane lanes in [`fastlane/Fastfile`](../fastlane/Fastfile).
All read config from env vars wired by the TeamCity configs.

The `com.google.gms.google-services` plugin **is** applied (for future Firebase
SDK use), but a `google-services.json` is provided only for the **UAT** variant
(`androidApp/src/uat/`). Its processing is disabled for `debug`/`release` (they
have no base-package file) so those builds don't fail. To use Firebase SDK in
debug/release, add a `google-services.json` for `dev.weft.undercurrent`
(register that app in Firebase) and remove the disable block in
`androidApp/build.gradle.kts`.

## Agent prerequisites (one-time)

On the build agent(s):

```bash
# Ruby + fastlane (the configs run `bundle install` then `bundle exec fastlane`)
gem install bundler
# Firebase plugin is declared in fastlane/Pluginfile and installed by bundle.

# Android agents: env.ANDROID_HOME + JDK 17 (already required by the build).
# iOS agent: full Xcode + command line tools, signed in to an Apple Developer team.
```

## Credentials to provide (TeamCity → Project → Parameters)

Define these as **Password** params (secrets) or plain params (ids/paths). The
file-based secrets (keystore, service-account JSONs, `.p8`) are best uploaded via
*Administration → Project → Secure Files*, then reference the resolved path.

### Android release signing (Play only)
Firebase distributes the **debug** build (debug signing key — no keystore needed).
Only Play Console needs the release keystore:

| Param | What |
|---|---|
| `release.keystore.path` | Path to the upload keystore (`.jks`) on the agent / secure file |
| `release.keystore.password` | Keystore password (**Password**) |
| `release.key.alias` | Key alias |
| `release.key.password` | Key password (**Password**) |

The signing config in [`androidApp/build.gradle.kts`](../androidApp/build.gradle.kts)
activates **only** when `RELEASE_KEYSTORE` is set, so normal builds are unaffected.

### Firebase App Distribution
| Param | What |
|---|---|
| `firebase.app.id` | Firebase Android App ID (`1:123…:android:abc…`) — register the Firebase app for package **`dev.weft.undercurrent.uat`** (the UAT build's id), not the base package |
| `firebase.service.credentials.path` | Path to the Firebase service-account JSON (secure file) |
| (optional) `FIREBASE_GROUPS` | Tester groups, comma-separated (default `testers`) |

### Google Play Console
| Param | What |
|---|---|
| `play.service.account.json.path` | Path to the Play service-account JSON (secure file) with "Release manager" access |
| `PLAY_TRACK` | `internal` (default) / `alpha` / `beta` / `production` |

First upload to Play must be done manually (the app must already exist in the
console). Uploads land as a **draft**.

### App Store Connect (iOS)
| Param | What |
|---|---|
| `asc.key.id` | App Store Connect API key id |
| `asc.issuer.id` | API key issuer id |
| `asc.key.path` | Path to the `AuthKey_XXXX.p8` (secure file) |

The API key handles auth + signing. If you don't use automatic signing, add
`match`/manual provisioning to the `ios appstore` lane.

## Running a deploy

*Open the config → Run… → (optionally set `PLAY_TRACK` / `FIREBASE_GROUPS`) → Run.*
The lane builds the signed artifact and uploads it.

### Versioning (collision-safe, automatic)

The deploy configs pass the **TeamCity build counter** (`%build.counter%`, monotonic
per config) into the build, so stores never reject a duplicate:

- **Android** — `VERSION_CODE` env → `versionCode` in
  [`androidApp/build.gradle.kts`](../androidApp/build.gradle.kts) (defaults to `1`
  locally).
- **iOS** — `BUILD_NUMBER` env → `CURRENT_PROJECT_VERSION` via fastlane `xcargs`.
  Requires the iOS target's `CFBundleVersion` to be `$(CURRENT_PROJECT_VERSION)`
  (default for modern Xcode projects); if it's a literal, switch the lane to
  `increment_build_number`.

The user-facing `versionName` / marketing version stays manual (override with
`-PversionName=…` / `VERSION_NAME`, e.g. from a release tag) — only the
build *code/number* needs to be unique.

## Security

- Never commit keystores, `.p8`, or service-account JSON. `.gitignore` already
  excludes `*.keystore`, `*.jks`, `**/*.p8`.
- Store all secrets as TeamCity Password params / Secure Files, not in the repo.
