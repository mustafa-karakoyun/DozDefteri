# DozDefteri

Ad-free, account-free, fully offline medication and supplement reminder for Android.
"No subscription · No account · Unlimited medications."

Built from the implementation plan in [`AGENT_PLAN.md`](AGENT_PLAN.md). Phase reports live in [`reports/`](reports/),
with the summary in [`reports/OZET.md`](reports/OZET.md).

## Requirements

- JDK 17+ (the build targets Java 17)
- Android SDK with platform 36 (set `sdk.dir` in `local.properties` or `ANDROID_HOME`)

## Build commands

```bash
./gradlew assembleDebug            # debug APK
./gradlew testDebugUnitTest        # JVM + Robolectric unit tests
./gradlew lintDebug                # Android lint
./gradlew connectedDebugAndroidTest  # instrumentation tests (emulator/device required)
./gradlew assembleRelease          # unsigned release APK (R8 enabled)
./gradlew bundleRelease            # unsigned release AAB
```

To try Pro features without a purchase, set `DEBUG_FORCE_PRO` to `true` in the `debug` build type in
`app/build.gradle.kts`. Release builds always use `false`.
