---
name: run-appium-suite
description: >-
  Build the APK and run the Appium UI suite on an Android emulator. Covers
  PowerShell on Windows and bash on macOS and Linux. Use when asked to "run
  the tests", "run the suite", "run-suite", check that tests pass on the
  emulator, or collect proof (logs). Checks the real run result in the root
  suite-run.log on every supported OS.
---

# run-appium-suite

Cross-platform runner for the Appium suite. Use the repository scripts on
every OS: `scripts/run-suite.ps1` on Windows and `scripts/run-suite.sh` on
macOS/Linux. Do not reproduce the run sequence by hand unless diagnosing a
script failure.

This skill always targets one emulator. If several devices are connected,
select one serial explicitly. This workflow does not support multi-device execution.

> Proof that "the change works" is the output of a
> local run on the emulator.
> Always read the real test numbers in the log.

## Prerequisites: check before the run

Two processes must be running. If they are not, start them.

**AVD note:** the system image must match the host CPU. The setup scripts use
`x86_64` and `sandbox_x64` on Intel/AMD, or `arm64-v8a` and `sandbox_arm64` on
ARM hosts.

**Appium toolchain is pinned** in `package.json`: appium 2.16.2 and the
uiautomator2 driver 3.9.8. Run `npm ci` once in the repo root and start Appium
only through the OS-specific `scripts/start-appium` launcher. It selects the
npm-pinned driver instead of another project's registry.
This project pins Appium 2.16.2; do not accept an audit fix that upgrades the server or driver
across a major version.

PowerShell on Windows:

```powershell
# 1. Create or validate the CPU-compatible 2 GB AVD and start it visibly
.\scripts\setup-emulator.ps1

# 2. Verify the complete local environment
.\scripts\bootstrap.ps1

# 3. fake-api in a separate terminal
.\gradlew.bat :fake-api:run

# 4. Appium server on loopback:4723 in another terminal
.\scripts\start-appium.ps1
```

bash on macOS / Linux:

```bash
# 1. Create or validate the AVD and start it
./scripts/setup-emulator.sh

# 2. Verify the complete local environment
./scripts/bootstrap.sh

# 3. fake-api in a separate terminal
./gradlew :fake-api:run

# 4. Appium server on loopback:4723 in another terminal
./scripts/start-appium.sh
```

Start fake-api and Appium in separate terminals or background shell sessions.
Before continuing, require `http://127.0.0.1:8080/swagger` and
`http://127.0.0.1:4723/status` to respond; the runner checks both again.

The setup script waits for boot and disables all emulator animations. The run
scripts repeat the animation reset before the suite.

## Run steps

The default flavor is `stable`. For the locator-migration exercise use
`redesign`.

macOS / Linux: use the script. It builds the APK, checks for a booted device,
disables animations, resets the sandbox states, forces `--rerun`, and after
the test run generates the Allure HTML report:

```bash
./scripts/run-suite.sh

# redesign flavor:
FLAVOR=redesign ./scripts/run-suite.sh
```

The script records combined stdout and stderr in the repository-root
`suite-run.log` on both macOS and Linux. It forwards extra arguments to the
Gradle test task. To run one class:

```bash
./scripts/run-suite.sh --tests tests.CompletedRideHistoryTest
```

Confirm that the selected target's fresh JUnit result is present; a successful
command or an unrelated passing test does not verify the target.

PowerShell on Windows: use the native runner. It performs the same checks and
report generation. If more than one emulator is connected, pass the target
serial explicitly so a recording cannot silently use the wrong device.

```powershell
.\scripts\run-suite.ps1

# one generated class on a chosen emulator
.\scripts\run-suite.ps1 -Device emulator-5554 -TestFilter "tests.CompletedRideHistoryTest"

# redesign flavor
.\scripts\run-suite.ps1 -Flavor redesign -Device emulator-5554
```

## Verify the result, do not skip

Gradle exiting 0 does not yet mean the tests actually ran. Check:

1. **What ran.** For an unfiltered suite run, derive the expected inventory
   from the `@Test` methods under `appium-tests/src/test/kotlin/tests/` and
   compare it with the JUnit XML totals. For a filtered run, require the
   requested class or method in JUnit XML and evaluate that target's result;
   do not compare a filtered total with the full source inventory.
2. **JUnit report:** `appium-tests/build/reports/tests/test/index.html` and
   `appium-tests/build/test-results/test/*.xml` hold the exact pass/fail
   numbers. The human-friendly Allure report (steps, screenshots) is at
   `appium-tests/build/reports/allure-report/index.html`; the XML stays the
   source of truth for the numbers.
3. **False-green markers:** `0 tests`, `NO-SOURCE`, or `UP-TO-DATE` on the
   `:appium-tests:test` task mean the cache was served: rerun with
   `--rerun`.
4. **enforceAppInstall:** install-once. `DriverFactory` sets
   `enforceAppInstall=true` on the first session of a run, which forces the
   APK install; later sessions of the same run reuse that build. This is
   correct: both flavors share `applicationId`/`versionCode`, and without
   the first-session forced install Appium silently tests the wrong build.
   Do NOT remove this logic.

In your reply to the user, quote the real numbers from the log, X passed and
Y failed, not a "BUILD SUCCESSFUL" paraphrase.

## What NOT to do

- Do NOT edit `app/` to "fix" the tests: that masks a regression and is
  detected when the diff touches `app/`.
- Do NOT drop `--rerun` for speed: you get the cache instead of a run.

After a failure, see the `verify-sandbox-state` skill: a sandbox state may
have been left enabled, for example `intermittent_backend_delay`.
