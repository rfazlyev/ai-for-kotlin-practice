# Suite baseline report

Date: 2026-09-04. Appium suite ran 15:47:58–15:48:55 local time, API suite ran
15:50:24–15:50:31 local time (12:47:58.672Z–12:48:55.847Z and
12:50:24.541Z–12:50:31.619Z in the JUnit XML timestamps).

## Environment

| Item | Value |
|---|---|
| OS and version | macOS 26.2 (build 25C56) |
| Emulator: AVD name and API level | Pixel_6, API 36 (Android 16), `google_apis` / `arm64-v8a` |
| Appium server version | 2.16.2 |

Source: `environment_notes.md`. AVD image confirmed at
`~/.android/avd/Pixel_6.avd/config.ini`
(`image.sysdir.1=system-images/android-36/google_apis/arm64-v8a/`); API level
also read live as `ro.build.version.sdk=36`.

## Source state

| Item | Value |
|---|---|
| Repository revision | `4639189` |
| Working tree | No tracked file modified. Two untracked files: `baseline_report.md`, `environment_notes.md` |

Source: `git rev-parse --short HEAD` and `git status --short`.

## App build variant

`stableDebug` (the default `FLAVOR=stable`).

Source: `suite-run.log:1` — `Building stable debug APK` — and `suite-run.log:45`
— `> Task :app:assembleStableDebug UP-TO-DATE`.

Note: the task was `UP-TO-DATE`, so the APK was not recompiled during this run;
the suite exercised a `stableDebug` APK built earlier.

## Command used

```
./scripts/run-suite.sh
```

Verified: `suite-run.log` in the repository root is written by that runner
(`SUITE_LOG="$PWD/suite-run.log"`, `scripts/run-suite.sh:8`), and its content
matches a `FLAVOR=stable` run.

```
./gradlew :fake-api:run          (in one terminal)
./gradlew :api-tests:test --rerun (in another)
```

Unverified: these are the commands documented in `AGENTS.md`. No local log
records the API invocation, so the exact form actually typed — in particular
whether `--rerun` was passed — is **unknown**. Only the resulting artifacts
under `api-tests/build/` prove that the API suite ran.

## Results: Appium suite

| Total | Passed | Failed | Skipped |
|---|---|---|---|
| 6 | 6 | 0 | 0 |

Arithmetic check: 6 = 6 + 0 + 0. ✅

Duration: **56.9 s** (56 872 ms).

Evidence:

- `appium-tests/build/reports/allure-report/widgets/summary.json` —
  `total: 6, passed: 6, failed: 0, broken: 0, skipped: 0, unknown: 0`,
  `time.duration: 56872`.
- `appium-tests/build/test-results/test/TEST-tests.OnboardingSmokeTest.xml`
  (`tests="3" skipped="0" failures="0" errors="0" time="29.404"`),
  `TEST-tests.PasskeyPromoTest.xml` (`tests="2" … time="18.676"`),
  `TEST-tests.RideAndHistoryE2ETest.xml` (`tests="1" … time="9.091"`).
- `suite-run.log:72-84` lists all six tests as `PASSED`, then
  `BUILD SUCCESSFUL in 59s`.

Per the Failed definition used in this report (all unsuccessful non-skipped
tests, execution errors included): JUnit `errors="0"` in every file and Allure
`broken: 0` / `unknown: 0`, so Failed is 0.

## Results: API suite

| Total | Passed | Failed | Skipped |
|---|---|---|---|
| 4 | 4 | 0 | 0 |

Arithmetic check: 4 = 4 + 0 + 0. ✅

Duration: **7.1 s** (7 055 ms).

Evidence:

- `api-tests/build/reports/allure-report/widgets/summary.json` —
  `total: 4, passed: 4, failed: 0, broken: 0, skipped: 0, unknown: 0`,
  `time.duration: 7055`.
- `api-tests/build/test-results/test/TEST-tests.ApiSmokeTest.xml`
  (`tests="1" skipped="0" failures="0" errors="0" time="1.269"`) and
  `TEST-tests.RideLifecycleApiTest.xml` (`tests="3" … time="5.782"`).

## Allure sources

- Appium report: `appium-tests/build/reports/allure-report/index.html`
  (generation confirmed at `suite-run.log:88-89`). Raw results:
  `appium-tests/build/allure-results/`.
- API report: `api-tests/build/reports/allure-report/index.html`.
  Raw results: `api-tests/build/allure-results/`.
- Gradle HTML test report for the API run:
  `api-tests/build/reports/tests/test/index.html`.

## Cross-suite overview

- UI coverage visible in the report: onboarding (`Full onboarding opens the
  map`, `Wrong OTP shows an error`, `Ride options show prices after search`),
  the passkey promo in both directions (`Skip …`, `Create a passkey …`), and one
  end-to-end ride path (`Search a ride then check order history`). Source:
  `suite-run.log:72-82` and the three Appium JUnit XML files.
- API coverage visible in the report: token issuance opening the data endpoints
  (`ApiSmokeTest`), plus three ride-lifecycle cases — `A completed ride becomes
  the newest order`, `A cancelled ride is not added to order history`,
  `driver_not_found rejects an order without changing history`. Source:
  `api-tests/build/test-results/test/*.xml`.
- Failures, skips, or slow-test observations: no failures and no skips in either
  suite. Slowest Appium test `Ride options show prices after search` at
  11.599 s; fastest `Wrong OTP shows an error` at 7.332 s (Allure
  `minDuration: 7332`, `maxDuration: 11600`). Slowest API test `A completed ride
  becomes the newest order` at 1.951 s. The six Appium tests are tightly
  clustered between 7.3 s and 11.6 s, so no single test stands out as slow.

## Known issues and flaky observations

- Nothing behaved unstably in either run: every test passed on the first
  attempt, and `appium-tests/build/reports/failures/` — the directory
  `ArtifactsOnFailure` creates on its first capture
  (`rule/ArtifactsOnFailure.kt:44`) — does not exist, so no screenshot, logcat
  or page source was ever written.
- The `stableDebug` APK was `UP-TO-DATE` rather than freshly assembled
  (`suite-run.log:45`), so this baseline attests to a previously built APK.
- `testRuns` is empty in both Allure `summary.json` files. Expected for a
  single local run with no history, not a defect.
- The emulator, Appium server and `fake-api` were no longer running when this
  report was assembled and had to be restarted. The restarted processes are a
  different instance than the one that produced these results, so any later
  live check is not evidence about this run.
- The exact API suite command is unknown (see "Command used").

## Next action

Add UI coverage for the notifications screen. `rule/AppiumTestCase.kt:62-68`
exposes seven action groups, but the six starter tests reference only
`onboarding` (11 calls), `map` (7), `orders` (1) and `drawer` (1) — leaving
`notifications`, `region` and `support` with page objects and actions in place
and no test exercising them. Notifications are the widest gap, because the API
side already asserts the ride-completion and cancellation outcomes that the app
surfaces there.
