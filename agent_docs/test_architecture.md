# Test architecture

<!-- Agent drafting scope:
Read AGENTS.md first. Then inspect appium-tests/README.md and the current sources
under appium-tests/src/test/kotlin/rule/, pages/, actions/, tests/ and testdata/.

Fill only this file. Keep its existing headings. Map claims to real source paths,
avoid duplicating volatile test inventory, and mark anything you cannot confirm
as UNVERIFIED. Do not modify other files.
-->

This document starts as an AI draft. Verify every claim against the repository
before relying on it.

All paths below are relative to `appium-tests/src/test/kotlin/` unless they
start with a repository directory such as `appium-tests/`, `api-tests/`,
`scripts/` or `app/`. Line numbers refer to the tree at the time of drafting
and drift; the quoted declarations are the stable anchor.

## Layers

The Appium suite has five layers. The policy text and the code agree on the
split; the sources for each claim are listed with the layer.

| Layer | Directory | Owns | Must not contain |
| --- | --- | --- | --- |
| 1. Rule | `rule/` | Session lifecycle, driver configuration, sandbox state control, failure evidence | Locators, flows, assertions about the product |
| 2. Pages | `pages/` | One `Element` per Compose `testTag`, factories for ID-bearing tags, device primitives | Assertions, flows, waits other than the ones `Element` itself offers |
| 3. Actions | `actions/` | Interactions, multi-step flows, readiness waits, JUnit assertions | `Element(...)` construction, direct driver access |
| 4. Tests | `tests/` | JUnit scenarios as named Allure steps, calling actions only | Page imports, assertions, locators |
| 5. Test data | `testdata/` | Shared constant values | Logic |

Policy sources:

- `AGENTS.md`, "Appium test architecture", numbers the same five layers:
  `rule/` "session lifecycle, driver configuration, sandbox control and
  failure evidence"; `pages/` "singleton `Element` catalogs. Pages contain no
  assertions"; `actions/` "interactions, flows, waits and assertions";
  `tests/` "JUnit scenarios written as named Allure steps"; `testdata/`
  "shared values".
- `appium-tests/README.md`, "Architecture", repeats the tree with the same
  one-line responsibilities and adds: "Pages do not assert or own flows.
  Tests use camelCase methods, `@DisplayName`, `@AllureId`, `@Feature` and
  named `step` blocks."
- `rule/AppiumTestCase.kt` KDoc (lines 31 to 45) is the in-code statement:
  "Layering: pages (`pages/`) = element catalogs, NO asserts; actions =
  steps + checks (asserts live here); testcases = the action sequence."
  "Waits and retries live on Element (waitFor / waitForGone / retryClick);
  device-level primitives live on Device." "Failure artifacts (screenshot +
  logcat + page source) come from ArtifactsOnFailure; FailureDigest condenses
  them".

### Layer 1: `rule/`

Five files, each with one job:

- `rule/AppiumTestCase.kt`: the abstract base every test extends. It owns
  `@BeforeEach startSession` and `@AfterEach endSession`, the `step()`
  reporter, and the "vocabulary" fields (`val onboarding = OnboardingActions`,
  `val map = MapActions`, `val drawer = DrawerActions`, `val orders =
  OrderActions`, `val notifications = NotificationActions`, `val region =
  RegionActions`, `val support = SupportActions`, `val driverSignup =
  DriverActions`). It is annotated
  `@ExtendWith(FailureDigest::class, ArtifactsOnFailure::class)`.
- `rule/DriverFactory.kt`: creates the `AndroidDriver`, holds it in a
  `ThreadLocal` (`current()`), manages the optional device pool
  (`appium.devices`) and the once-per-device forced install. Protected path
  (`scripts/protected-paths.txt`: "Appium session config: silent-wrong-build
  protection lives here (enforceAppInstall, disableIdLocatorAutocompletion)").
- `rule/ConditionControl.kt`: `enable`, `disable` and `reset` for the sandbox
  states, over local adb first and the Appium `mobile: shell` transport
  second.
- `rule/ArtifactsOnFailure.kt`: JUnit `TestExecutionExceptionHandler` that
  captures screenshot, logcat and page source on failure.
- `rule/FailureDigest.kt`: JUnit `AfterTestExecutionCallback` that writes one
  condensed text digest per failure.
- `rule/DeviceExecutionEvidence.kt`: appends "which test ran on which
  emulator" lines for the device pool.

Nothing in `rule/` imports from `pages/`; `rule/AppiumTestCase.kt` imports
`actions.*` only to expose the vocabulary fields.

### Layer 2: `pages/`

Singleton `object`s holding `Element` values. `pages/Element.kt` is the only
locator and wait primitive; `pages/Device.kt` holds the three device-level
primitives (`hideKeyboard`, `pressBack`, `pullDownToRefresh`);
`pages/VariantLocator.kt` is protected infrastructure. Locator and wait detail
is in `agent_docs/page_object_model.md`; this document does not repeat it.

Proof that pages contain no assertions: a grep for
`org.junit.jupiter.api.Assertions` across `appium-tests/src/test/kotlin`
matches only the eight files in `actions/` (`DrawerActions`, `DriverActions`,
`MapActions`, `NotificationActions`, `OnboardingActions`, `OrderActions`,
`RegionActions`, `SupportActions`). No file in `pages/`, `rule/`, `tests/` or
`testdata/` imports JUnit assertions.

### Layer 3: `actions/`

Stateless `object`s that call page elements, wait for readiness anchors and
assert with JUnit messages. `actions/RegionActions.kt` is the smallest
complete example of the layer doing all three:

```kotlin
object RegionActions {
    fun showRegionUnavailable() {
        ConditionControl.enable("region_unavailable")
        assertTrue(
            MapPage.regionBanner.waitFor().isDisplayed,
            "Region banner should appear when region_unavailable is enabled",
        )
    }
```

Actions are the only layer allowed to talk to `rule/ConditionControl`; the
only call sites are `actions/RegionActions.kt` lines 10 and 18. The base
class calls `ConditionControl.reset()` itself, in both `startSession` and
`endSession`.

What actions must not do is enforced by Gradle, not only by review.
`appium-tests/build.gradle.kts` registers `checkPageObjectBoundary` and wires
it into `check`:

```kotlin
val checkPageObjectBoundary by tasks.registering {
    group = "verification"
    description = "Reject Element construction outside the pages layer."
    val sources =
        fileTree("src/test/kotlin") {
            include("**/*.kt")
            exclude("pages/**")
        }
```

The task greps every non-`pages/` Kotlin file for `Element(` and fails with
"Element must be constructed only in pages/: ...". This guards `Element`
construction only; there is no equivalent task rejecting JUnit imports under
`pages/` or page imports under `tests/`. Those two rules are upheld by review
and are currently satisfied (see the grep results in this document).

### Layer 4: `tests/`

JUnit 5 classes extending `AppiumTestCase`. `tests/OnboardingSmokeTest.kt`
shows the full shape:

```kotlin
@Feature("Onboarding")
class OnboardingSmokeTest : AppiumTestCase() {
    // Onboarding IS the subject here: sessions must start on the login
    // screen, not skip past it with the "authenticated" launch extra.
    override val startAuthorized = false

    @Test
    @DisplayName("Full onboarding opens the map")
    @AllureId("1000")
    fun testFullOnboardingOpensMap() {
        step("Valid phone opens OTP") {
            onboarding.reachOtp()
        }
```

Rules visible here and in policy:

- Tests call actions through the base-class vocabulary fields and never a
  page. A grep for `^import pages\.` in `tests/` returns nothing.
- Every test body is a sequence of `step("...") { }` blocks
  (`appium-tests/README.md`; `rule/AppiumTestCase.kt` `step()`).
- `startAuthorized` defaults to `true` and is overridden to `false` only when
  onboarding is under test (`AGENTS.md`, "Appium test architecture";
  `rule/AppiumTestCase.kt` lines 52 to 59).
- New tests need unused Allure IDs and must not duplicate coverage
  (`AGENTS.md`, "Starter tests").

### Layer 5: `testdata/`

One file, `testdata/TestData.kt`: `object TestData` with `const val`s and
two `Map<Int, String>` seeds. Its KDoc: "Central test data so testcases read
declaratively and values live in one place." It has no dependencies on any
other layer.

## Where things live

### Appium suite

| What | Path | Source |
| --- | --- | --- |
| Session base class, `step()` | `rule/AppiumTestCase.kt` | file |
| Driver creation, `ThreadLocal` session, device pool | `rule/DriverFactory.kt` | file; protected in `scripts/protected-paths.txt` |
| Sandbox state broadcasts | `rule/ConditionControl.kt` | file |
| Failure capture (screenshot, logcat, page source) | `rule/ArtifactsOnFailure.kt` | file |
| Failure digest | `rule/FailureDigest.kt` | file |
| Device assignment log | `rule/DeviceExecutionEvidence.kt` | file |
| Locator and wait primitive | `pages/Element.kt` | file |
| Device primitives | `pages/Device.kt` | file |
| Flavor-aware tag helper (protected, currently unused) | `pages/VariantLocator.kt` | file; `scripts/protected-paths.txt` |
| Page catalogs | `pages/*Page.kt` | listed in `agent_docs/page_object_model.md` |
| Actions | `actions/*Actions.kt` | directory |
| Scenarios | `tests/*Test.kt` | directory |
| Shared values | `testdata/TestData.kt` | file |
| Gradle test task, system properties, boundary check | `appium-tests/build.gradle.kts` | file |
| Suite runners | `scripts/run-suite.sh`, `scripts/run-suite.ps1` | `AGENTS.md`, "Build and run" |

For the authoritative list of test classes and cases, read the `tests/`
directory or the generated Allure report rather than this document.
`AGENTS.md` ("Starter tests") and `appium-tests/README.md` name the three
starter classes and the count of six cases; that inventory changes as tests
are added and is not repeated here.

### Runtime wiring

The Gradle `test` task in `appium-tests/build.gradle.kts` is the only place
that turns command-line flags into what the code reads:

| System property | Default in `build.gradle.kts` | Read by |
| --- | --- | --- |
| `appium.url` | `http://127.0.0.1:4723` | `rule/DriverFactory.kt` |
| `app.apk` | `app/build/outputs/apk/stable/debug/app-stable-debug.apk` | `rule/DriverFactory.kt` (`error` if unset) |
| `ui.variant` | `stable` | `pages/VariantLocator.kt` |
| `allure.results.directory` | `appium-tests/build/allure-results` | Allure JUnit 5 listener |
| `appium.devices` | empty | `rule/DriverFactory.kt` pool; `rule/ConditionControl.kt` `-s <udid>` |
| `appium.parallel` | `false` | JUnit parallel switches in the same task |
| `appium.device.evidence.dir` | `appium-tests/build/reports/device-execution` | `rule/DeviceExecutionEvidence.kt` |

`scripts/run-suite.sh` passes `-Dapp.apk`, `-Dui.variant` and
`-Dappium.devices="$DEVICE"` on every run (line 91 to 94), so the device
pool has exactly one slot even in a sequential run.

### API suite

The API tests are a separate Gradle module with their own layering, stated in
`AGENTS.md`, "API test architecture": `rule/ApiTestCase` owns setup and token
acquisition; `client/` owns REST Assured requests with no assertions;
`model/` holds wire DTOs; `tests/` holds scenarios and assertions. The tree
under `api-tests/src/test/kotlin/` has exactly those directories plus
`testdata/ApiTestData.kt`. The API module shares no code with
`appium-tests`; the two `rule/` and `testdata/` packages are namesakes only.

## Waits and retries

### Policy

- `AGENTS.md`, "Appium test architecture": "Do not use `Thread.sleep`;
  synchronize through `Element.waitFor`, `waitForGone` and narrowly justified
  `retryClick` calls."
- `AGENTS.md`, "Safety boundaries": "Never weaken assertions, increase
  timeouts or add retries without evidence that the test expectation is
  correct and the synchronization is the defect."
- `appium-tests/README.md`, "Architecture": "`Thread.sleep` and text XPath
  locators are forbidden."

Proof the tree complies: a grep for `Thread.sleep`, `xpath`,
`ExpectedConditions`, `FluentWait`, `WebDriverWait` and `implicitlyWait`
across `appium-tests/src/test/kotlin` matches only `pages/Element.kt`
(lines 10 to 12, 36, 37, 43, 44, 61). No implicit wait is configured
anywhere, so every wait in the suite is one of the explicit helpers below.

### The helpers, all on `pages/Element.kt`

| Helper | Mechanism | Default | Intended use | Misuse |
| --- | --- | --- | --- | --- |
| `waitFor(timeoutSec)` | `WebDriverWait` + `visibilityOfElementLocated` | `defaultTimeoutSec` (10 s unless the page raises it) | Readiness anchor for a screen; returns the `WebElement` | Calling it on an element that may legitimately be absent |
| `click`, `sendKeys`, `clear`, `text` | Same visibility wait, then the action | same | Ordinary interaction | Assuming `text` takes a timeout (it does not; call `waitFor(n).text`) |
| `waitForGone(timeoutSec)` | `WebDriverWait` + `invisibilityOfElementLocated` | same | Assert disappearance, wrapped in `assertTrue` | Using it as a "maybe gone" probe; it throws on timeout |
| `retryClick(timeoutSec, message)` | `FluentWait(Unit)` polling every 300 ms, retrying only `NoSuchElementException`, `StaleElementReferenceException`, `ElementClickInterceptedException` | same, `message` required | A tap that lands mid-transition | Any other flakiness; it retries nothing else by design |
| `isPresent()` | `findElements(...).isNotEmpty()` | none, no wait | Negative check after a readiness anchor | Using it as a readiness signal |

Source for the retry narrowness, `pages/Element.kt` lines 50 to 55:
"Deliberately narrow: only those three exception types are retried, so real
failures (assertion errors, dead session, interrupts) surface immediately
instead of being retried into a timeout."

### How the helpers are used in the current tree

- **Readiness anchors instead of sleeps.** `actions/MapActions.kt`
  `awaitReady()` waits on `pickupField` and `destinationField` with 20 s and
  `pullToRefresh` with the default. Every screen transition in
  `actions/OnboardingActions.kt` ends on an anchor of the next screen,
  usually its `title` element; the last transition to the map waits on
  `MapPage.destinationField.waitFor(20)` (line 153).
- **Longer defaults live on the element, not in the action.** Pages raise
  `defaultTimeoutSec` to 15 or 20 for elements that appear after a backend
  round trip (`pages/MapPage.kt`, `pages/OrderHistoryPage.kt`). Actions pass
  an explicit timeout only for anchors such as `awaitReady()`.
- **`retryClick` is confined to the side drawer.** All call sites are
  `actions/DrawerActions.kt` lines 16, 22 and 32 and
  `actions/DriverActions.kt` line 23, each right after `menuButton.click()`.
  The `DrawerActions` KDoc states the justification and its limit:
  "retryClick guards the drawer's open animation so the tap settles even if
  it lands mid-transition. (Animations should be OFF in run-suite/CI anyway -
  this is belt-and-suspenders, not a substitute for that.)"
  `scripts/run-suite.sh` lines 74 to 77 set the three animation scales to 0
  before the run.
- **`waitForGone` is always an assertion or a transition gate.** Call sites:
  `actions/RegionActions.kt` line 20 and `actions/DriverActions.kt` line 82
  wrap it in `assertTrue`; `actions/SupportActions.kt` line 37,
  `actions/NotificationActions.kt` lines 21, 26 and 27 and
  `actions/DriverActions.kt` line 94 use it as a gate before the next step.
- **`isPresent` is always a negative assertion.** All six call sites are
  inside `assertFalse(...)`: `actions/OrderActions.kt` line 40,
  `actions/SupportActions.kt` line 12, `actions/MapActions.kt` lines 70, 74,
  154 and 160. Three of them wait inside the same method first
  (`OrderActions.assertOrderAbsent` on `title.waitFor(15)`,
  `SupportActions.awaitReady` on `title` and `fourthFaq`,
  `MapActions.assertOnlyTariffSelected` on the selected tariff). The other
  three (`assertFindOffersRemoved`, `assertTariffsNotLoaded`,
  `assertNoSelectedTariff`) have no in-method anchor and rely on an earlier
  step such as `map.awaitReady()` having proven the screen is rendered. When
  adding a negative check, prefer the first form.

### Retries and waits outside `Element`

These exist in `rule/` and are infrastructure, not test synchronization:

- **Session creation retry**, `rule/DriverFactory.kt`
  `createSessionWithHealthRetry`: `repeat(2)`, retrying once and only when
  `isTransientDeviceFailure()` matches "device offline", "not in the list of
  connected devices" or "adbexec" in the message. Before each attempt
  `awaitDeviceReady` runs `adb -s <udid> wait-for-device` with a 30 s cap.
  Session capabilities: `newCommandTimeout` 120 s,
  `uiautomator2ServerLaunchTimeout` 60 s. This file is protected; do not
  tune these values to fix a red test.
- **Sandbox broadcast timeout**, `rule/ConditionControl.kt`: the adb process
  gets 15 s (`process.waitFor(15, TimeUnit.SECONDS)`), then the Appium
  `mobile: shell` transport is tried. Neither transport waits for the state
  to apply. The KDoc is explicit: "Delivery only proves the command reached
  a device, never that the state took effect. The OBSERVABLE effect must be
  asserted by the test". `actions/RegionActions.kt` is the model: enable,
  then `waitFor()` the banner; disable, then `waitForGone()`.
- **No test-level retry.** No JUnit `@RepeatedTest`, rerun extension or
  Gradle `retry` plugin appears in `appium-tests/build.gradle.kts` or the
  sources. The `--rerun` flag in `scripts/run-suite.sh` line 91 only defeats
  Gradle's up-to-date check so a cached green result is never reused
  (`appium-tests/README.md`: "Do not use a bare cached `:appium-tests:test`
  result as evidence").

## Failure artifacts

### What is captured, by whom, and where it goes

Both failure extensions are wired once, on the base class
(`rule/AppiumTestCase.kt` line 47):

```kotlin
@ExtendWith(FailureDigest::class, ArtifactsOnFailure::class)
abstract class AppiumTestCase {
```

Paths in the code are relative (`File("build/reports/failures")`,
`File("build/reports/digests")`). The Gradle test JVM runs with the module
directory as working directory and `appium-tests/build.gradle.kts` sets no
`workingDir`, so they resolve to `appium-tests/build/...`, which is what
`appium-tests/README.md` ("Evidence") lists. The whole `build/` tree is
gitignored (`.gitignore`: `build/`).

| Artifact | Producer | File on disk | Allure attachment | When |
| --- | --- | --- | --- | --- |
| Screenshot | `rule/ArtifactsOnFailure.kt` | `appium-tests/build/reports/failures/<displayName>-<epochMillis>.png` | "Failure screenshot" (`image/png`) | Test threw |
| Device logcat | `rule/ArtifactsOnFailure.kt` | `.../failures/<displayName>-<epochMillis>.logcat` | "Device logcat" (`text/plain`) | Test threw |
| UI page source | `rule/ArtifactsOnFailure.kt` | `.../failures/<displayName>-<epochMillis>.xml` | "UI page source" (`application/xml`) | Test threw |
| Digest | `rule/FailureDigest.kt` | `appium-tests/build/reports/digests/test_log_<method>_<try>.txt` | none | Test threw |
| Screen after each step | `rule/AppiumTestCase.kt` `attachScreenState()` | none | "Screen after step" (`image/png`), or "Screen capture failed" with the stack trace | Every `step()`, pass or fail |
| Device assignment | `rule/DeviceExecutionEvidence.kt` | `appium-tests/build/reports/device-execution/<udid>.log` | none, but `Allure.label("device", udid)` is set | Every test start when `appium.devices` is non-empty |
| Console stack trace | Gradle `testLogging` | `suite-run.log` at the repository root (via `tee`) | none | Every run |

The file name stem for the raw trio is built in `rule/ArtifactsOnFailure.kt`
lines 44 to 46:

```kotlin
        val dir = File("build/reports/failures").apply { mkdirs() }
        val base = context.displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val name = "$base-${System.currentTimeMillis()}"
```

The display name is the `@DisplayName` text with every character outside
`[A-Za-z0-9._-]` replaced by `_`, so "Wrong OTP shows an error" becomes
`Wrong_OTP_shows_an_error-<millis>.png`.

The digest file name uses the method name and a try counter
(`rule/FailureDigest.kt` lines 15 to 21 and 47):

```kotlin
        val dir = File("build/reports/digests").apply { mkdirs() }
        val name =
            context.testMethod
                .map { it.name }
                .orElse(context.displayName)
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
        val tryNumber = nextTryNumber(dir, name)
```

`nextTryNumber` is one more than the largest existing suffix for that method,
so repeated repair attempts keep every earlier digest
(`test_log_testWrongOtpShowsError_1.txt`, `_2.txt`, and so on). Nothing in
the suite or `scripts/run-suite.sh` deletes `failures/` or `digests/`; only
`allure-results` and `test-results/test` are removed before a run
(`scripts/run-suite.sh` line 88).

### Order of capture and what each extension relies on

1. The test body throws inside a `step()`. The `finally` in `step()` attaches
   "Screen after step" first (`rule/AppiumTestCase.kt` lines 108 to 120).
2. JUnit invokes `ArtifactsOnFailure.handleTestExecutionException` while the
   test is still executing and before `@AfterEach`, so the driver is alive.
   It resolves the driver from `DriverFactory.current()`; if that is `null`
   it prints "no live Appium session; failure artifacts were not captured"
   and returns. Each of the three artifacts is produced once and written to
   both destinations independently, so a failed file write does not stop the
   Allure attachment or the next artifact (`capture()` KDoc, lines 95 to 99).
   The original throwable is always rethrown (line 33).
3. JUnit invokes `FailureDigest.afterTestExecution`. It returns immediately
   when `context.executionException` is empty (line 13). Otherwise it looks
   up the newest `failures/<displayName>-*` trio by `lastModified` and reads
   the logcat from that file instead of the driver, because "Appium drains
   the device log buffer on read" (lines 123 to 124). The
   `rule/AppiumTestCase.kt` KDoc states the dependency: "The artifact
   exception handler runs before the digest's after-test-execution callback,
   allowing the digest to reuse the captured logcat." The order follows from
   JUnit 5's extension model (exception handlers run during test execution,
   after-test-execution callbacks after it), not from the order in
   `@ExtendWith`.
4. `@AfterEach endSession` resets sandbox state, quits the driver and
   releases the device slot.

If `ArtifactsOnFailure` left no files, the digest still writes and marks the
gaps: "(no page source: ArtifactsOnFailure left no XML for this test)" and
"(no logcat: ArtifactsOnFailure left no file for this test)".

### Digest contents

`rule/FailureDigest.kt` lines 24 to 45 build six sections:

- Header: `test_log <method> try <n>`, display name, test class.
- `FAILURE :` the exception `toString()`.
- `--- project stack frames ---`: only frames whose class starts with
  `tests.`, `actions.`, `pages.`, `rule.` or `testdata.`; falls back to the
  first 8 raw frames when none match.
- `--- failure artifacts ---`: absolute paths of the raw trio or `(missing)`.
- `--- visible UI resource IDs ---`: every distinct `resource-id="..."` in the
  captured page source, sorted. This is the fastest way to see which
  `testTag`s were on screen at failure.
- `--- logcat slice ---`: if a `FATAL EXCEPTION` line is followed by
  `Process: com.sandbox.qa`, 120 lines from that point; otherwise the last 40
  lines. Lines are cut at 240 characters because Appium echoes base64
  screenshots into logcat. The anchor deliberately ignores the UiAutomator2
  server's own fatal on teardown (comment at lines 129 to 132).

`appium-tests/README.md` ("Evidence") gives the triage order: "Read it first;
open the raw artifacts when it is not decisive."

### Reports around the artifacts

| Output | Path | Produced by |
| --- | --- | --- |
| JUnit XML | `appium-tests/build/test-results/test/TEST-*.xml` | Gradle; deleted before each run by `scripts/run-suite.sh` line 88 and read back for the summary by `scripts/run-suite.ps1` lines 107 to 121 |
| Gradle HTML | `appium-tests/build/reports/tests/test/index.html` | Gradle default; listed in `appium-tests/README.md` |
| Allure raw results | `appium-tests/build/allure-results` | `allure-junit5` listener via `allure.results.directory`; deleted before each run |
| Allure static report | `appium-tests/build/reports/allure-report/index.html` | `node_modules/.bin/allure generate` in `scripts/run-suite.sh` lines 99 to 105, also on a red run |
| Full console log | `suite-run.log` at the repository root | `scripts/run-suite.sh` line 8 to 9 (`tee`); `scripts/run-suite.ps1` lines 102 to 104 (`Tee-Object`); gitignored by `/*.log` |

Console visibility of the capture messages depends on the mode:
`appium-tests/build.gradle.kts` sets `showStandardStreams = true` and
`exceptionFormat = FULL` for sequential runs, so the `println` lines from
`ArtifactsOnFailure` ("saved ... to ...") and `FailureDigest` ("wrote ...")
appear in `suite-run.log`. In parallel mode (`appium.parallel=true`)
`showStandardStreams` is `false` and only failed and skipped events are
logged, so the files on disk are the evidence.

### Observed state of the working tree

At drafting time `appium-tests/build/reports/` contained `allure-report`,
`device-execution` and `tests` but no `failures/` or `digests/` directory.
That is consistent with the code: both directories are created lazily by
`mkdirs()` on the first failure, so their absence means no test has failed in
this build directory, not that capture is disabled. Whether the last run
recorded in `suite-run.log` was fully green: UNVERIFIED from the directory
listing alone; read the log's Gradle summary.
