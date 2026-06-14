# TeamCity setup — Undercurrent

CI for Undercurrent runs as **versioned settings** (config-as-code) committed
under [`.teamcity/`](../.teamcity). TeamCity reads the Kotlin DSL straight from
the repo — no build configs are hand-clicked in the UI.

## The one non-obvious thing: the composite build

`settings.gradle.kts` does `includeBuild("../weft")`, so **the build only works
if the `weft` SDK (repo `android-harness`) is checked out as a sibling**:

```
<agent checkout dir>/
├── undercurrent/   ← this repo        (Gradle runs here)
└── weft/           ← android-harness  (the substrate SDK)
```

Every compiling build type uses `sharedComposeCheckout()`
([`.teamcity/Common.kt`](../.teamcity/Common.kt)), which attaches both VCS roots
with checkout rules (`+:. => undercurrent`, `+:. => weft`) and sets the Gradle
working directory to `undercurrent`. Don't remove the second VCS root — the
build fails to resolve `dev.weft:*` without it.

**VCS roots are referenced, not defined.** They already exist on the server, so
the DSL doesn't redeclare them. Set their external IDs once in
[`Common.kt`](../.teamcity/Common.kt):

```kotlin
const val UNDERCURRENT_VCS_ID = "ReplaceWithUndercurrentVcsRootId"  // undercurrent
const val WEFT_VCS_ID         = "ReplaceWithWeftVcsRootId"          // android-harness
```

Find each ID under *VCS Root → identifier* in TeamCity. If a root for the weft /
android-harness repo doesn't exist yet, add it on the server first — the
composite build needs it.

## Pipeline

| Build config | Gradle | Agent | Trigger |
|---|---|---|---|
| **Android · compile + unit tests + coverage** | `:androidApp:compileDebugKotlin test koverXmlReport` | Linux/mac + Android SDK | via PR gate |
| **iOS · compile** | `:composeApp:compileKotlinIosSimulatorArm64` | **macOS** | via PR gate |
| **PR validation** (composite) | — | — | pull requests → GitHub status |
| **Android · assemble debug APK** | `:androidApp:assembleDebug` | Linux/mac + Android SDK | manual |
| **UAT · assemble** | `%uat.gradle.tasks%` (debug for now) | Linux/mac + Android SDK | push to `main` |
| **Publish · Google Play** (next phase) | `:androidApp:bundleRelease` (+publish) | Android SDK | manual, **paused** |
| **Publish · App Store** (next phase) | fastlane/xcodebuild | **macOS** | manual, **paused** |

`PR validation` is a composite that stays red until both Android and iOS pass,
then publishes a single commit status to the PR.

## One-time server wiring

0. **Set the VCS root IDs** in [`Common.kt`](../.teamcity/Common.kt)
   (`UNDERCURRENT_VCS_ID`, `WEFT_VCS_ID`) to the IDs of the roots already on
   your server. The DSL references these; it does not create roots.

1. **Connect the project to this repo**
   *Project → … → Versioned Settings* → enable, point at the existing
   `undercurrent` VCS root, path `.teamcity`, format **Kotlin**. TeamCity
   imports the DSL and adds the seven build configs alongside your existing
   project setup.

2. **Match the DSL version to your server**
   `version = "2024.12"` in [`settings.kts`](../.teamcity/settings.kts) and
   `<version>2024.12</version>` in [`pom.xml`](../.teamcity/pom.xml) must equal
   your TeamCity version. The server rewrites both on first import — commit the
   result.

3. **Parameters** (Project → Parameters) — only two, and only if they don't
   already exist at a higher scope:
   - `github.token` — **password** param: a GitHub PAT with `commit status`
     scope, used by the PR status publisher. VCS auth itself comes from your
     existing roots, so the PR feature reuses those credentials. Committed value
     is a `REPLACE_WITH_TOKEN` placeholder.
   - `jdk.home` — defaults to `%env.JDK_17_0%`. Set to whatever your agents
     expose for **JDK 17** (`env.JDK_17`, a custom property, or an absolute
     path). KMP here targets JVM 17.

4. **Agent requirements**
   - Android/JVM agents: `env.ANDROID_HOME` set, JDK 17, Gradle wrapper allowed
     (Gradle 9.3.1 downloads via the wrapper). License accepted for the SDK
     packages the build needs.
   - iOS agent: **macOS** with Xcode + the Kotlin/Native toolchain (the wrapper
     provisions Konan on first run).

## Next phase — publishing

Both publish configs are paused scaffolds guarded by `publish.enabled=false`;
they exit early until configured. To turn on **Google Play**
([`PublishPlayConsole.kt`](../.teamcity/PublishPlayConsole.kt)):

1. Add the Gradle Play Publisher plugin to `:androidApp`.
2. Add the release signing config (keystore as a TeamCity secure file).
3. Upload the Play service-account JSON as a secure file / secret param.
4. Set `publish.enabled=true`, repoint `play.gradle.tasks` to the publish task.

For **App Store / TestFlight**
([`PublishAppStore.kt`](../.teamcity/PublishAppStore.kt)): stand up the
`iosApp` archive/export (fastlane or `xcodebuild -exportArchive`), provision
signing certs + an App Store Connect API key on the mac agent, then flip
`publish.enabled=true`.

When the real UAT variant exists, repoint `uat.gradle.tasks` in
[`UatRelease.kt`](../.teamcity/UatRelease.kt) from `assembleDebug` to the signed
`uatRelease` variant.

## Editing the DSL locally

```bash
cd .teamcity
mvn -q teamcity-configs:generate   # validates the DSL compiles + generates XML
```

Output lands in `.teamcity/target/` (gitignored). The authoritative check is the
server re-generating on import, but this catches compile errors before you push.
