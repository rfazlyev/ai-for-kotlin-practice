# environment_notes.md

- OS + version:            macOS 26.2 (build 25C56)
- Host CPU architecture:   arm64 (Apple silicon)
- JDK:                     openjdk version "17.0.20.1" 2026-08-18 LTS
                           OpenJDK Runtime Environment Corretto-17.0.20.10.1 (build 17.0.20.1+10-LTS)
                           OpenJDK 64-Bit Server VM Corretto-17.0.20.10.1 (build 17.0.20.1+10-LTS, mixed mode, sharing)
- Android platform:        android-36
- Build-tools:             36.0.0
- System image:            android-36 / arm64-v8a (system-images/android-36/google_apis/arm64-v8a)
- AVD name:                Pixel_6
- Node.js:                 v26.3.0
- Appium:                  2.16.2
- uiautomator2 driver:     3.9.8
- APK build variant:       stableDebug
- Test run command:        ./scripts/run-suite.sh
- Allure CLI:              2.43.0
- Date of setup:           2026-09-01 (notes recorded 2026-09-02)

## Environment notes that affect result interpretation

- `./scripts/bootstrap.sh` reports 14 passed, 0 failed.
- JDK selection is per-directory. A `chpwd` hook in `~/.zshrc` sets `JAVA_HOME`
  to Corretto 17 inside this repository and back to Corretto 24 outside it,
  because Corretto 24 is the intentional global default for another project.
  A terminal opened before that hook existed still reports JDK 24.
- Three different JDKs take part in a build: the shell JDK is 17 (checked by
  the doctor, used by the `gradlew` launcher), the Gradle daemon JVM is 25
  (from the local, git-ignored `gradle/gradle-daemon-jvm.properties`), and
  compilation is pinned to 17 by `jvmToolchain(17)`. Compilation output is
  therefore 17 regardless of the daemon JVM.
- The AVD was cold-booted with `-no-snapshot-load`. A quick-boot snapshot
  restore left the device stuck at `offline`.
- `~/.emulator_console_auth_token` must not be empty. A 0-byte token disables
  emulator console auth, which makes `adb emu avd name` return an empty string
  and the doctor report a booted emulator as not booted.
- Emulator animations are disabled (`window_animation_scale`,
  `transition_animation_scale`, `animator_duration_scale` set to 0).
- The suite runner defaults to `FLAVOR=stable`; `FLAVOR=redesign
  ./scripts/run-suite.sh` selects the redesign APK.

## Not yet verified

- `Test run command` records the runner this environment is set up for. The
  Appium suite has not been executed yet, so no pass/fail count is recorded
  here. Confirm the command and its result on the first real suite run before
  filling `baseline_report.md`.
