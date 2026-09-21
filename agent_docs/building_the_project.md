# Building the project

<!-- Agent drafting scope:
Read AGENTS.md first. Then inspect package.json, the root Gradle build files,
module READMEs and the OS-specific scripts under scripts/.

Fill only this file. Keep its existing headings. Cite the repository source for
every command, mark anything you cannot confirm as UNVERIFIED, and do not invent
execution results. Do not modify other files.
-->

This document starts as an AI draft. Verify every claim against the repository
before relying on it.

Every command below names the repository file it was taken from. Windows uses
`gradlew.bat`, `npm.cmd` and the `.ps1` scripts; macOS and Linux use `gradlew`,
`npm` and the `.sh` scripts (source: `README.md`, "Quick start").

## Prerequisites

### Toolchain versions the repository pins or checks

| Requirement | Pinned or checked value | Source |
| --- | --- | --- |
| JDK | 17 | `AGENTS.md` ("Build and run"); `README.md`; `jvmToolchain(17)` in `appium-tests/build.gradle.kts`, `api-tests/build.gradle.kts`, `fake-api/build.gradle.kts`; `JavaVersion.VERSION_17` in `app/build.gradle.kts`; `scripts/bootstrap.sh` / `scripts/bootstrap.ps1` "JDK 17" check; `.github/workflows/ci.yml` (temurin 17) |
| Gradle | 9.5.1, supplied by the wrapper | `gradle/wrapper/gradle-wrapper.properties`; `README.md` ("Gradle is supplied by the wrapper") |
| Android Gradle Plugin | 9.2.0 | `build.gradle.kts` |
| Kotlin | 2.3.20 | `build.gradle.kts` |
| ktlint | Gradle plugin 14.2.0, engine 1.8.0 | `build.gradle.kts` |
| Node.js | 20 or newer | `README.md`; `scripts/bootstrap.sh` / `scripts/bootstrap.ps1` "Node.js 20+" check |
| Appium | 2.16.2 | `package.json`; version check in `scripts/run-suite.sh`, `scripts/run-suite.ps1` and both bootstrap scripts |
| Appium UiAutomator2 driver | 3.9.8 | `package.json`; both bootstrap scripts |
| Allure CLI | 2.43.0 | `package.json`; both bootstrap scripts |
| Android SDK platform | 36 (`platforms/android-36/android.jar`) | `README.md`; both bootstrap scripts; `compileSdk = 36` and `targetSdk = 36` in `app/build.gradle.kts` |
| Android build-tools | 36.0.0 | `README.md`; both bootstrap scripts |
| Android platform-tools (adb) and Emulator | present under the SDK root, with hardware acceleration | both bootstrap scripts |
| Emulator AVD | API 36, `google_apis` image, default name `Pixel_6`, RAM 1536 to 4096 MB (default 2048) | `scripts/setup-emulator.sh`, `scripts/setup-emulator.ps1`; both bootstrap scripts |
| Python | required to run `scripts/sync_agent_skills.py`; no version is pinned | `README.md` ("Agent skills"); `.github/workflows/ci.yml`. Required Python version: UNVERIFIED |

The Node toolchain is installed with `npm ci` and must not be upgraded
independently (source: `README.md`, "Prerequisites"; `package.json`
description). `package.json` and `package-lock.json` are protected paths
(source: `scripts/protected-paths.txt`).

```bash
npm ci
```

```powershell
npm.cmd ci
```

Source: `README.md` ("Quick start"), `AGENTS.md` ("Build and run").

### Android SDK location

The scripts resolve the SDK root from `ANDROID_HOME`, then `ANDROID_SDK_ROOT`,
then an OS default: `~/Library/Android/sdk` on macOS, `~/Android/Sdk` on Linux
and `%LOCALAPPDATA%\Android\Sdk` on Windows (source: `scripts/bootstrap.sh`,
`scripts/bootstrap.ps1`, `scripts/setup-emulator.sh`, `scripts/setup-emulator.ps1`,
`scripts/run-suite.ps1`).

OS-specific caveats confirmed from the scripts:

- `scripts/run-suite.sh` and `scripts/bootstrap.sh` call `adb` by name, so on
  macOS and Linux `platform-tools` must be on `PATH`. `scripts/run-suite.ps1`
  instead resolves `platform-tools\adb.exe` from the SDK root and does not
  need `PATH`.
- `scripts/setup-emulator.sh` and `scripts/setup-emulator.ps1` need the
  Android SDK Command-line Tools (`sdkmanager`, `avdmanager`) under
  `cmdline-tools/latest/bin` or a versioned `cmdline-tools/*/bin`.
- The system image ABI is `arm64-v8a` on arm64 hosts and `x86_64` otherwise
  (source: `scripts/setup-emulator.sh`, `scripts/bootstrap.ps1`).
- `scripts/run-suite.sh` avoids bash 4 features because macOS ships bash 3.2
  (source: comment in `scripts/run-suite.sh`).
- Whether a `local.properties` file with `sdk.dir` is required for the Gradle
  Android build is UNVERIFIED; the file is gitignored (source: `.gitignore`)
  and no repository document mentions it.

### Provision and check the environment

```bash
./scripts/setup-emulator.sh
./scripts/bootstrap.sh
```

```powershell
.\scripts\setup-emulator.ps1
.\scripts\bootstrap.ps1
```

Source: `README.md` ("Quick start"); `.agents/skills/run-appium-suite/SKILL.md`.
`setup-emulator` creates and boots the `Pixel_6` AVD (bash accepts the
`AVD_NAME`, `RAM_MB`, `CHECK_ONLY` and `NO_START` environment variables;
PowerShell accepts `-AvdName`, `-RamMb`, `-CheckOnly` and `-NoStart`).
`bootstrap` is a read-only doctor that prints `[OK]` / `[FAIL]` per check and
exits non-zero when anything fails (source: `scripts/bootstrap.sh`,
`scripts/bootstrap.ps1`).

### Services that must be running before tests

- The Ktor backend `fake-api` on port 8080. The emulator reaches it at
  `http://10.0.2.2:8080`, which is baked into the app as
  `BuildConfig.API_BASE_URL` (source: `AGENTS.md` "Product and backend";
  `app/build.gradle.kts`; `fake-api/README.md`).
- The repository-pinned Appium server on `http://127.0.0.1:4723` (source:
  `scripts/start-appium.sh`, `scripts/start-appium.ps1`, `scripts/run-suite.sh`,
  `scripts/run-suite.ps1`).
- Exactly one booted emulator or device, unless a serial is given explicitly
  (source: `scripts/run-suite.sh` `DEVICE` variable, `scripts/run-suite.ps1`
  `-Device` parameter).

## Build commands

### Android app

The app has one flavor dimension, `ui`, with two flavors: `stable` (marked
`isDefault = true`) and `redesign`, which renames auth-screen test tags
(source: `app/build.gradle.kts`). Application ID is `com.sandbox.qa`.

```bash
./gradlew :app:assembleStableDebug
./gradlew :app:assembleRedesignDebug
```

Source: `AGENTS.md` ("Build and run"). On Windows use `.\gradlew.bat` with the
same task names (source: `README.md`).

Artifacts land at `app/build/outputs/apk/<flavor>/debug/app-<flavor>-debug.apk`:

| Task | APK | Source |
| --- | --- | --- |
| `:app:assembleStableDebug` | `app/build/outputs/apk/stable/debug/app-stable-debug.apk` | default `app.apk` in `appium-tests/build.gradle.kts`; `scripts/run-suite.sh` |
| `:app:assembleRedesignDebug` | `app/build/outputs/apk/redesign/debug/app-redesign-debug.apk` | APK path pattern in `scripts/run-suite.sh` and `scripts/run-suite.ps1` |

CI builds both flavors and runs Android lint on the stable debug variant with
one command (source: `.github/workflows/ci.yml`):

```bash
./gradlew :app:assembleDebug :app:lintStableDebug
```

Android lint is configured with `abortOnError = true` (source:
`app/build.gradle.kts`). Release variants exist as Gradle build types but no
repository document describes building them: UNVERIFIED.

The suite runners build the selected flavor themselves before running tests,
so a separate `assemble` step is not required before `run-suite` (source:
`scripts/run-suite.sh`, `scripts/run-suite.ps1`).

### Backend

```bash
./gradlew :fake-api:run
```

Source: `AGENTS.md` ("Product and backend"), `fake-api/README.md`. The server
listens on port 8080; `FAKE_API_PORT` overrides it (source:
`fake-api/README.md`; `fake-api/src/main/kotlin/com/sandbox/qa/fakeapi/Main.kt`).
Note that the suite runners and the API tests still probe port 8080 by
default, so overriding the port also requires `-Dapi.url` for API tests and is
not supported by the UI runners (source: `scripts/run-suite.sh`,
`scripts/run-suite.ps1`, `api-tests/build.gradle.kts`). Swagger UI is served at
`http://localhost:8080/swagger` (source: `AGENTS.md`). Module self tests:

```bash
./gradlew :fake-api:test
```

Source: `fake-api/README.md`.

### Test modules and formatting

Compile the Appium module without running it, as CI does (source:
`.github/workflows/ci.yml`):

```bash
./gradlew :appium-tests:compileTestKotlin
```

Check Kotlin formatting before committing Kotlin changes (source: `AGENTS.md`
"Build and run"; `.github/workflows/ci.yml`):

```bash
./gradlew ktlintCheck
```

Keep the generated skills mirror in sync after editing `.agents/skills`
(source: `AGENTS.md` "Safety boundaries", `README.md` "Agent skills"; CI runs it
with `--check`):

```bash
python scripts/sync_agent_skills.py
```

## Run the suite

### API tests

Start the backend in one terminal, then run the tests in another (source:
`AGENTS.md` "Build and run"; `api-tests/README.md`):

```bash
./gradlew :fake-api:run
./gradlew :api-tests:test --rerun
```

```powershell
.\gradlew.bat :fake-api:run
.\gradlew.bat :api-tests:test --rerun
```

The target defaults to `http://localhost:8080` and can be overridden with
`-Dapi.url=http://host:port` (source: `api-tests/README.md`;
`api-tests/build.gradle.kts`). The task deletes `api-tests/build/allure-results`
before each run (source: `doFirst` block in `api-tests/build.gradle.kts`).
Every stateful test uses an isolated sandbox session and resets it (source:
`AGENTS.md` "API test architecture").

### Appium UI suite

Start Appium and run the suite through the OS-specific runner. `fake-api` must
already be running (source: `AGENTS.md` "Build and run"; `README.md`;
`appium-tests/README.md`).

macOS or Linux:

```bash
./scripts/start-appium.sh
./scripts/run-suite.sh
FLAVOR=redesign ./scripts/run-suite.sh
```

Windows PowerShell:

```powershell
.\scripts\start-appium.ps1
.\scripts\run-suite.ps1
.\scripts\run-suite.ps1 -Flavor redesign
```

Source: `appium-tests/README.md` ("Run"); `.agents/skills/run-appium-suite/SKILL.md`.

`start-appium` refuses to run without the local `node_modules/.bin/appium`,
unsets `APPIUM_HOME` so Appium discovers the driver pinned by this npm project,
and starts `npx --no-install appium --address 127.0.0.1 --port 4723` (bash
reads `PORT`, PowerShell takes `-Port`). Source: `scripts/start-appium.sh`,
`scripts/start-appium.ps1`. The runners and the test default `appium.url` are
fixed to port 4723, so a non-default port is not supported end to end
(source: `scripts/run-suite.sh`, `scripts/run-suite.ps1`,
`appium-tests/build.gradle.kts`, `appium-tests/src/test/kotlin/rule/DriverFactory.kt`).

Runner options (source: `scripts/run-suite.sh`, `scripts/run-suite.ps1`;
`.agents/skills/run-appium-suite/SKILL.md`):

- Flavor: `FLAVOR=stable|redesign` (bash, default `stable`) or
  `-Flavor stable|redesign` (PowerShell).
- Device: `DEVICE=<serial>` (bash) or `-Device <serial>` (PowerShell). Required
  when more than one device is connected.
- Filter: `-TestFilter <filter>` (PowerShell). The bash runner forwards any
  extra arguments to Gradle, so `./scripts/run-suite.sh --tests <filter>` works.

What each runner does, in order (source: `scripts/run-suite.sh`,
`scripts/run-suite.ps1`):

1. Verifies the pinned local Appium and Allure binaries exist.
2. Probes `http://127.0.0.1:8080/swagger` for `fake-api` and
   `http://127.0.0.1:4723/status` for Appium, and rejects any Appium server
   whose reported version is not 2.16.2.
3. Builds the selected flavor with `:app:assemble<Flavor>Debug`.
4. Selects exactly one connected device and checks `sys.boot_completed`.
5. Sets the three global animation scales to 0.
6. Broadcasts the sandbox reset to `com.sandbox.qa/.condition.ConditionReceiver`.
7. Deletes stale `appium-tests/build/allure-results` and
   `appium-tests/build/test-results/test` (PowerShell also deletes the old
   Allure report).
8. Runs `:appium-tests:test --rerun` with `-Dapp.apk`, `-Dui.variant` and
   `-Dappium.devices` set for the chosen flavor and device.
9. Generates the static Allure report from the raw results, also on a red run.

The runner is the canonical entry point rather than a direct
`./gradlew :appium-tests:test` because the repository states that the runners
"select the APK, disable animations and force a real test run" (source:
`AGENTS.md` "Build and run") and that a bare cached `:appium-tests:test` result
must not be used as evidence (source: `appium-tests/README.md` "Run"). A direct
Gradle invocation does none of steps 1 to 7 and 9 above: it can return a cached
`UP-TO-DATE` result without executing anything, it defaults `app.apk` to the
stable APK regardless of the flavor under test (source:
`appium-tests/build.gradle.kts`), it leaves animations on, which breaks
UiAutomator2 element lookup under overlay animations (source: comment in
`scripts/run-suite.sh`), and it lets Allure results accumulate across runs so
the report misdescribes the run (source: comment in `scripts/run-suite.sh`).

Sandbox states must be enabled after the Appium session starts because session
creation relaunches the app process (source: `AGENTS.md` "Sandbox states").

## Verify it worked

### Distinguish executed tests from a green command

A green Gradle exit is not proof by itself. The repository names three ways a
run can look green while proving nothing:

- A cached `UP-TO-DATE` `:appium-tests:test` result is not a test execution;
  the runners pass `--rerun` for this reason (source: `appium-tests/README.md`;
  `.agents/skills/run-appium-suite/SKILL.md`).
- A zero-test run. `scripts/run-suite.ps1` fails with "No tests executed. The
  run is not valid proof." when the JUnit XML totals sum to zero
  (source: `scripts/run-suite.ps1`).
- A green CI result. CI compiles the Appium module but never executes Appium
  tests (source: `AGENTS.md` "CI"; `.github/workflows/ci.yml`).

The expected inventory is six Appium `@Test` methods in `OnboardingSmokeTest`,
`PasskeyPromoTest` and `RideAndHistoryE2ETest`, and four API `@Test` methods in
`ApiSmokeTest` and `RideLifecycleApiTest` (source: `AGENTS.md` "Starter
tests"; confirmed by counting `@Test` under `appium-tests/src/test/kotlin/tests/`
and `api-tests/src/test/kotlin/tests/`). Compare the executed count against the
current `@Test` inventory, not against this number, once tests are added
(source: `.agents/skills/run-appium-suite/SKILL.md` "Verify").

### Appium suite evidence

Both runners tee the whole run into `suite-run.log` at the repository root
(source: `scripts/run-suite.sh`, `scripts/run-suite.ps1`). The file is
gitignored (source: `.gitignore`, `/*.log`).

The two runners differ in what they write, and this matters for verification:

- `scripts/run-suite.ps1` parses `TEST-*.xml` under
  `appium-tests/build/test-results/test`, appends a line of the form
  `Appium result: N passed, N failed, N errors, N skipped (N total) on <device>`
  to `suite-run.log`, and exits 1 when the total is zero (source:
  `scripts/run-suite.ps1`).
- `scripts/run-suite.sh` does not compute that summary and does not fail on
  zero tests; its exit code is the Gradle exit code (source:
  `scripts/run-suite.sh`). The "final `Appium result` line" that
  `.agents/skills/run-appium-suite/SKILL.md` tells you to read therefore exists
  only on Windows runs. On macOS and Linux, read the per-test
  `PASSED` / `FAILED` / `SKIPPED` lines in `suite-run.log` (Gradle prints them
  because `testLogging.events("passed", "failed", "skipped")` is set for
  non-parallel runs in `appium-tests/build.gradle.kts`) and count the
  `tests=` attributes in the JUnit XML yourself.

The following count is not a repository command; it mirrors the XML aggregation
done in `scripts/run-suite.ps1` for use on macOS and Linux:

```bash
grep -ho 'tests="[0-9]*"' appium-tests/build/test-results/test/TEST-*.xml
```

Result locations (source: `appium-tests/README.md` "Evidence";
`scripts/run-suite.sh`; `scripts/run-suite.ps1`):

| Evidence | Path |
| --- | --- |
| Run log | `suite-run.log` (repository root) |
| JUnit XML | `appium-tests/build/test-results/test` |
| Gradle HTML report | `appium-tests/build/reports/tests/test/index.html` |
| Raw Allure results | `appium-tests/build/allure-results` |
| Static Allure report | `appium-tests/build/reports/allure-report/index.html` |
| Failure artifacts | `appium-tests/build/reports/failures` |
| Failure digests | `appium-tests/build/reports/digests` |
| Device execution evidence | `appium-tests/build/reports/device-execution/<udid>.log`, appended on every test start whenever `appium.devices` is non-empty, which both runners always set; only the pre-run deletion of this directory is limited to `-Dappium.parallel=true`, so sequential runs accumulate lines across runs (source: `rule/AppiumTestCase.kt` `startSession`, `rule/DeviceExecutionEvidence.kt`, `doFirst` in `appium-tests/build.gradle.kts`, `scripts/run-suite.sh`) |

The runners print `Allure report: <path>/index.html` after a successful report
generation, or a message that generation failed and raw results remain
(source: `scripts/run-suite.sh`, `scripts/run-suite.ps1`). Read the failure
digest first on a red run; it contains the exception, project stack, artifact
paths, visible resource IDs and a logcat slice (source: `appium-tests/README.md`).

### API suite evidence

`:api-tests:test` prints `passed`, `failed` and `skipped` events with standard
streams and full stack traces to the console (source: `api-tests/build.gradle.kts`).
Raw Allure results, including every request and response attached by the
shared REST Assured filter, are written to `api-tests/build/allure-results`
(source: `api-tests/README.md`; `api-tests/build.gradle.kts`). No repository
document names the JUnit XML or HTML report location for `api-tests`; the
Gradle default `api-tests/build/test-results/test` is UNVERIFIED as a
repository-documented path. The `--rerun` flag in the documented command is
what prevents a cached result (source: `AGENTS.md` "Build and run").

### Sandbox state changes

An adb return code, or an HTTP 200 from `POST /sandbox/state`, only means the
flag flipped. Verify the visible or API-level effect before treating a state as
applied (source: `AGENTS.md` "Sandbox states"; `fake-api/README.md` "Sandbox
states over HTTP").
