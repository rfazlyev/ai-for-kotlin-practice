# Agent docs verification report

Date: 2026-09-06. Repository revision `4639189` with the three `agent_docs/`
drafts uncommitted in the working tree. Every claim below was checked against
the file and line it names or against the command shown, in this session, on
macOS. Paths under `appium-tests/src/test/kotlin/` are written relative to it
where the docs do the same.

Summary:

| File | Claims checked | Verified | Fixed | UNVERIFIED left as is |
| --- | --- | --- | --- | --- |
| `agent_docs/building_the_project.md` | 24 | 23 | 1 (line 363) | 4, all correctly marked by the draft |
| `agent_docs/page_object_model.md` | 21 | 20 | 1 (lines 189 to 192, plus lines 240 to 243 sharpened) | 0 after the fix |
| `agent_docs/test_architecture.md` | 34 | 34 | 0, draft was clean | 1, correctly marked by the draft |

## `agent_docs/building_the_project.md`

### Verified claims

Claim 1 (lines 26 to 33): Gradle 9.5.1 from the wrapper, AGP 9.2.0, Kotlin
2.3.20, ktlint plugin 14.2.0 with engine 1.8.0, Appium 2.16.2, UiAutomator2
driver 3.9.8, Allure CLI 2.43.0.

Evidence:

```text
$ grep -n distributionUrl gradle/wrapper/gradle-wrapper.properties
3:distributionUrl=https\://services.gradle.org/distributions/gradle-9.5.1-bin.zip
build.gradle.kts:2  id("com.android.application") version "9.2.0" apply false
build.gradle.kts:3  id("org.jetbrains.kotlin.android") version "2.3.20" apply false
build.gradle.kts:9  id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
build.gradle.kts:17 version.set("1.8.0")
package.json:6-8    "allure-commandline": "2.43.0", "appium": "2.16.2",
                    "appium-uiautomator2-driver": "3.9.8"
```

Claim 2 (lines 307 to 309 and 329 to 336): the PowerShell runner sums the
JUnit XML totals, appends an `Appium result: ...` line to `suite-run.log` and
exits 1 with "No tests executed. The run is not valid proof." when the total
is zero, while the bash runner writes no such summary and exits with the
Gradle exit code.

Evidence: `scripts/run-suite.ps1:107` reads `TEST-*.xml`, `:120-121` build and
tee the `Appium result:` line, `:132-135` contain `if ($tests -eq 0) {
Write-Error "No tests executed. The run is not valid proof."; exit 1 }`.
`scripts/run-suite.sh:90-94` capture `suite_rc` from Gradle and `:107` is
`exit "$suite_rc"`; the file contains no `tests=` aggregation.

Other claims confirmed the same way: `isDefault = true` on the `stable`
flavor and `abortOnError = true` (`app/build.gradle.kts:25,43`); the CI
command `./gradlew :app:assembleDebug :app:lintStableDebug` and
`:appium-tests:compileTestKotlin` (`.github/workflows/ci.yml:34,54`);
`FAKE_API_PORT` with default 8080 (`fake-api/.../Main.kt:16`); the API test
target default `http://localhost:8080` and the `doFirst` delete of
`allure-results` (`api-tests/build.gradle.kts:28-38`); the nine runner steps
in order (`scripts/run-suite.sh:22-105`); `start-appium.sh` unsetting
`APPIUM_HOME` and reading `PORT` (`scripts/start-appium.sh:11-15`); the AVD
defaults `Pixel_6`, RAM 1536 to 4096 with default 2048 and the ABI switch
(`scripts/setup-emulator.sh:7-24`); `/*.log` and `local.properties` in
`.gitignore:5,14`; the six plus four `@Test` inventory
(`grep -rc '@Test'` gives 3+2+1 under `appium-tests/.../tests/` and 1+3 under
`api-tests/.../tests/`).

### Fix

Line 363, "Device execution evidence" row of the result table. The draft said
the directory is "written only when `-Dappium.parallel=true`". That is
wrong. `rule/AppiumTestCase.kt:76-81` calls `DeviceExecutionEvidence.record`
on every `startSession` whenever `DriverFactory.currentDeviceUdid()` is
non-null, and both runners always pass `-Dappium.devices` (`scripts/run-suite.sh:94`),
so a sequential run writes the log too. Only the pre-run deletion of the
directory is parallel-only (`appium-tests/build.gradle.kts:74-78`). The working
tree proves it: `appium-tests/build/reports/device-execution/emulator-5554.log`
exists from the sequential run of 2026-09-04 (the matching `suite-run.log`
shows per-test `PASSED` lines, which Gradle only prints in the non-parallel
branch of `testLogging`), and it already holds duplicate lines for
`RideAndHistoryE2ETest` because nothing clears it between sequential runs.

The row now reads: `appium-tests/build/reports/device-execution/<udid>.log`,
appended on every test start whenever `appium.devices` is non-empty, which
both runners always set; only the pre-run deletion is limited to
`-Dappium.parallel=true`, so sequential runs accumulate lines across runs,
with the four source files named.

The draft's four UNVERIFIED markers (Python version, `local.properties`,
release builds, `api-tests` JUnit XML path) were left in place; none of them
is answered by a repository document, so the markers are correct.

## `agent_docs/page_object_model.md`

### Verified claims

Claim 1 (lines 46 to 55, 87 to 96 and 100 to 104): the `Element` declaration,
the private `by()` returning `AppiumBy.id(testTag)`, the shared `locate()`
path with `visibilityOfElementLocated`, `waitForGone` on
`invisibilityOfElementLocated`, `retryClick` as a `FluentWait(Unit)` polling
every 300 ms that ignores exactly `NoSuchElementException`,
`StaleElementReferenceException` and `ElementClickInterceptedException`, a
`text` property that takes no timeout, and `isPresent()` with no wait.

Evidence: `pages/Element.kt:26-33` (class header and `by()`), `:35-37`
(`locate`), `:42-44` (`waitForGone`), `:57-72` (`retryClick`, with
`.pollingEvery(Duration.ofMillis(300))` at `:63` and the three `.ignoring`
calls at `:65-67`), `:86-87` (`val text: String get() = locate(defaultTimeoutSec).text`),
`:90` (`fun isPresent(): Boolean = driver.findElements(by()).isNotEmpty()`).

Claim 2 (lines 68 to 73 and 295 to 301): the driver sets
`appium:disableIdLocatorAutocompletion` to `true` so Compose tags are used
unprefixed, and `DriverSignupPage` adds the `com.sandbox.qa:id/` prefix itself
through a private `viewId` helper.

Evidence: `rule/DriverFactory.kt:124-125` (comment "...the driver rewrites them
to "com.sandbox.qa:id/phone_title"" and
`.amend("appium:disableIdLocatorAutocompletion", true)`);
`pages/DriverSignupPage.kt:3-7` (`private fun viewId(id: String) =
Element("com.sandbox.qa:id/$id")`, `val backButton = viewId("driver_back_button")`).

Other claims confirmed: the nine catalog names match `ls pages/` minus
`Device.kt`, `Element.kt`, `VariantLocator.kt`; the quoted `MapPage` block and
the four `defaultTimeoutSec = 15` factories (`pages/MapPage.kt:3-8,40-46`);
`driverFoundTitle`, `completedTitle`, `statusMessage` at 20 s
(`pages/MapPage.kt:20,25,29`); the `Long` IDs in `pages/NotificationsPage.kt:9-15`;
`testTagsAsResourceId = true` in `MainActivity.kt:53`, `TestTagWindow.kt:21`
and `PhoneLoginScreen.kt:92`; `variantId` unused outside
`pages/VariantLocator.kt` (grep); the full `OrderActions` source is verbatim
(`actions/OrderActions.kt:8-48`); `grep -rn '^import pages\.' tests/` returns
nothing; `grep -rn -E 'Thread\.sleep|xpath|ExpectedConditions|FluentWait|WebDriverWait|implicitlyWait'`
matches only `pages/Element.kt`.

### Fix

Lines 189 to 190 said: "Every file in `pages/` imports nothing from JUnit.
The only JUnit imports in the suite are in `actions/`". The second sentence is
false. `grep -rln 'import org.junit' appium-tests/src/test/kotlin` lists 14
files: the eight in `actions/`, plus `rule/AppiumTestCase.kt`,
`rule/ArtifactsOnFailure.kt`, `rule/FailureDigest.kt` and the three test
classes, which import `@BeforeEach`, `@AfterEach`, `@ExtendWith`, extension
interfaces, `@Test` and `@DisplayName`. What is true is narrower: only the
eight `actions/` files import `org.junit.jupiter.api.Assertions`. The
paragraph now says exactly that and names what `rule/` and `tests/` import
instead.

In the same pass lines 240 to 243 were sharpened. The draft marked "whether any
tool enforces the rule" as UNVERIFIED and said no check was found in
`appium-tests/build.gradle.kts`. A check does exist there,
`checkPageObjectBoundary` (`appium-tests/build.gradle.kts:95-125`), wired into
`check`; it rejects `Element(` construction outside `pages/` and nothing else.
The paragraph now states that, drops the UNVERIFIED marker, and keeps the
conclusion that the no-asserts and no-page-imports-in-tests rules are upheld by
review only.

## `agent_docs/test_architecture.md`

### Verified claims

Claim 1 (lines 120 to 139): Gradle enforces the actions boundary through a
`checkPageObjectBoundary` task wired into `check`, which greps every non-`pages/`
Kotlin file for `Element(` and fails with "Element must be constructed only in
pages/: ...".

Evidence: `appium-tests/build.gradle.kts:95-102` match the quoted block
character for character; `:105` `Regex("""\bElement\s*\(""")`; `:117-119`
`check(violations.isEmpty()) { "Element must be constructed only in pages/: ..." }`;
`:123-125` `tasks.named("check") { dependsOn(checkPageObjectBoundary) }`.

Claim 2 (lines 284 to 308): every `retryClick`, `waitForGone` and `isPresent`
call site, with line numbers.

Evidence, from `grep -rn` over `actions/`, `tests/` and `rule/`:

```text
retryClick:  DrawerActions.kt:16,22,32  DriverActions.kt:23
waitForGone: RegionActions.kt:20  DriverActions.kt:82,94  SupportActions.kt:37
             NotificationActions.kt:21,26,27
isPresent:   OrderActions.kt:40  SupportActions.kt:12  MapActions.kt:70,74,154,160
```

All 15 line numbers in the draft match, and every `isPresent()` call is inside
`assertFalse(...)` as claimed.

Other claims confirmed: the KDoc quotes and line ranges in
`rule/AppiumTestCase.kt` (`@ExtendWith` at `:47`, `startAuthorized` KDoc and
default `true` at `:52-59`, `step()` with `finally { attachScreenState() }` at
`:108-120`, `ConditionControl.reset()` in both lifecycle methods at `:86,95`);
`ConditionControl` call sites only at `actions/RegionActions.kt:10,18` outside
`rule/`; the `Assertions` import list of exactly eight `actions/` files;
`pages/Element.kt` line references `10-12, 36, 37, 43, 44, 61` and the KDoc at
`:50-55`; `scripts/run-suite.sh:8-9` (`tee`), `:74-77` (animation scales),
`:88` (`rm -rf` of results and JUnit dirs), `:91-94` (Gradle flags), `:99-105`
(Allure generate); `scripts/run-suite.ps1:102-104` and `:107-121`;
`rule/ArtifactsOnFailure.kt:33` (`throw throwable`), `:44-46` (name stem),
`:95-99` (`capture()` KDoc); `rule/FailureDigest.kt:13,15-21,47,123-124,129-132`;
`rule/ConditionControl.kt:57` (`process.waitFor(15, TimeUnit.SECONDS)`) and
`:51` (`-s <udid>` from `currentDeviceUdid()`); `rule/DriverFactory.kt:113`
(`repeat(2)`), `:122` (`uiautomator2ServerLaunchTimeout` 60 s), `:154-156`
(30 s `wait-for-device` cap); the system-property table against
`appium-tests/build.gradle.kts:20-73`; the `api-tests/src/test/kotlin` tree
(`client`, `model`, `rule`, `testdata`, `tests`); and the observed
`appium-tests/build/reports/` listing (`allure-report`, `device-execution`,
`tests`, no `failures/` or `digests/`).

### Fix

None needed. 34 claims were checked, including all 40-plus line-number
references, and every one matched the current tree. The draft's single
UNVERIFIED marker (whether the last recorded run was fully green) is resolvable
from `suite-run.log`, which shows six `PASSED` lines and `BUILD SUCCESSFUL`, but
the draft correctly refused to infer it from the directory listing, so the
marker stays.

One wording nit was noted and left alone because the facts are right: line
115 says actions are "the only layer allowed to talk to
`rule/ConditionControl`" and the next sentence says the base class in `rule/`
calls `reset()` itself. Read together they are consistent (product-state
changes come from actions; hygiene resets come from the rule layer).

## Consumer test

Run after the fixes above, in a fresh general-purpose agent with no access to
this session's context. The agent was allowed to open only the three
`agent_docs/*.md` files and was told not to open source, scripts, build files
or READMEs.

Question asked:

> I am on macOS. `./scripts/run-suite.sh` just exited with code 0. How do I
> confirm how many Appium tests actually executed, and does the
> device-execution evidence log under `appium-tests/build/reports/device-execution`
> get written on a sequential (non-parallel) run?

Which documents the agent used: it opened all three and answered from
`agent_docs/building_the_project.md`, sections "Distinguish executed tests
from a green command" (lines 299 to 319), "Appium suite evidence" (lines 321
to 349) and the result-location table row at line 363. It cited
`agent_docs/test_architecture.md` only as corroboration ("Runtime wiring"
lines 224 to 226 and the "Device assignment" table row at line 362) and
reported `agent_docs/page_object_model.md` as opened but not relevant.

The agent's answer, condensed:

- Exit code 0 is the Gradle exit code only. The bash runner writes no
  `Appium result:` line and does not fail on zero tests; that behaviour is
  PowerShell-only. On macOS read the per-test `PASSED` / `FAILED` / `SKIPPED`
  lines in `suite-run.log` at the repository root and sum the `tests=`
  attributes in `appium-tests/build/test-results/test/TEST-*.xml`, then compare
  against the current `@Test` inventory rather than the number six.
- Yes, the device-execution log is written on a sequential run, because both
  runners always pass `-Dappium.devices`; only its pre-run deletion is
  parallel-only, so the file accumulates lines across runs and is not a
  per-run test count.

Match against verified facts: both parts match. Part one matches
`scripts/run-suite.sh:90-107` and `scripts/run-suite.ps1:107-135` as checked
above. Part two matches `rule/AppiumTestCase.kt:76-81`,
`rule/DeviceExecutionEvidence.kt:19-29`, `appium-tests/build.gradle.kts:74-78`
and the on-disk `emulator-5554.log` with its duplicated lines. Had the
question been asked against the unfixed draft, the agent would have answered
"no, parallel mode only" from the old line 363, which the tree contradicts.

Ambiguities the agent raised that are real and worth a follow-up in the docs:

- The docs give no repository-blessed macOS equivalent of the PowerShell
  summary; the `grep -ho 'tests="[0-9]*"'` line prints per-file counts and
  the reader still sums them.
- `test_architecture.md` lines 394 to 396 say only `allure-results` and
  `test-results/test` are removed before a run, which is true for the runner
  but omits the parallel-only Gradle deletion of `device-execution`. The two
  statements are consistent, the second is incomplete on its own.
- Neither doc says the `<udid>.log` lines carry no timestamps. Confirmed: the
  file holds bare `Class > display name` lines, so the current run cannot be
  isolated from earlier ones by reading the file.
