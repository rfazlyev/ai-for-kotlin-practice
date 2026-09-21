# AI Policy: Allowed Changes, Forbidden Changes, Human Gates

This policy governs AI-assisted work in this repository. It applies to every
agent session and to every human who reviews an AI-assisted change. The agent
sees the critical lines through `AGENTS.md`; this file holds the full rules.

An actionable rule names the boundary, the evidence or decision it requires,
and the way a reviewer checks that it was followed. Every rule below is
written that way.

Nothing in this repository runs the section 2 detectors automatically. The
only wired structural check is the Gradle task `checkPageObjectBoundary` in
`appium-tests/build.gradle.kts`, which rejects `Element` construction outside
`pages/`. Every other detector is a command the reviewer runs on the diff
before approving. "Must stay at zero hits" means zero hits on the changed
branch, checked by the reviewer, not by CI.

## Blast radius

Autonomy follows blast radius: the more of the project a wrong change can
affect, the stronger the gate.

| What changes | Blast radius | Gate |
| --- | --- | --- |
| One test in `tests/` | That test only | Agent works alone; normal diff review |
| `testdata/`, a page object in `pages/`, an action in `actions/`, a DTO in `model/` | Every test that reads it | Review with the dependents in mind (`grep -rn <symbol> appium-tests/src api-tests/src` before approving) |
| Framework core: `rule/` (`AppiumTestCase`, `ArtifactsOnFailure`, `ConditionControl`, `DeviceExecutionEvidence`, `FailureDigest`), `Element` waits, `retryClick`, `client/ApiSpec.kt`, the runners in `scripts/`, `appium-tests/build.gradle.kts` and `api-tests/build.gradle.kts` (the test task's filter, system properties and reruns live there) | The whole suite; a small tweak can hide flakes or narrow the executed set everywhere | Human approval before the change |
| Anything in `scripts/protected-paths.txt` (`app/`, `fake-api/`, `.github/workflows/`, `rule/DriverFactory.kt`, `pages/VariantLocator.kt`, `package.json`, `package-lock.json`, `fixtures/`) | The product, the build selection, the CI, or the pinned toolchain | Forbidden without an explicit human request naming the path |

`fixtures/` is listed as protected but does not exist at revision `4639189`.
If it is created, it inherits the protected gate from the first commit.

## 1. Allowed changes

"Allowed" means the agent may draft and verify the change. It never means the
change merges unreviewed. Section 3 still applies.

| Allowed | Condition that makes it acceptable | How the reviewer detects missing or insufficient proof |
| --- | --- | --- |
| New Appium tests, page objects, actions | A fresh run of `scripts/run-suite.sh` or `scripts/run-suite.ps1` against the target APK, with no `--tests`, `-TestFilter`, `-Dapp.apk` or `-Dui.variant` argument added to the runner. The summed `tests=` attribute across `appium-tests/build/test-results/test/*.xml` equals the baseline total plus the number of new tests (baseline: 6, see `baseline_report.md`). New tests use unused Allure IDs (current IDs: 1000 to 1005 in `appium-tests`, 2000 to 2003 in `api-tests`) | The change description quotes the exact runner invocation. A filtered run, a missing total, or a total below baseline plus new tests sends the change back. `suite-run.log` alone is not enough, because the bash runner does not record extra Gradle arguments |
| New API tests | `fake-api` was running, `./gradlew :api-tests:test --rerun` executed, and the summed `tests=` across `api-tests/build/test-results/test/*.xml` equals baseline (4) plus new tests, with `failures="0"` and `errors="0"` in every file | Same arithmetic check on the JUnit XML. A `--tests` filter on the command sends the change back |
| Locator migrations | Same full-suite run and same total-count check as for new tests; every migrated test is listed as `PASSED`; every new locator is a Compose `testTag` used through `AppiumBy.id` | Count check plus the locator grep from section 2 |
| Triage drafts, bug reports, log slices | Every claim cites the artifact it came from (file and line in `suite-run.log`, a JUnit XML attribute, an Allure `summary.json` field, a logcat line under `appium-tests/build/reports/failures/`), never a recollection | A claim without a file-and-line citation is marked `UNVERIFIED` by the reviewer or rejected |
| Refactors of test scaffolding, helpers, waiters, `actions/` | The attached full run matches `baseline_report.md`: same test count (6 UI, 4 API), zero failures, zero skips, and no test that newly needs `retryClick` | Diff on `retryClick` call sites plus the JUnit XML arithmetic |
| Documentation under `agent_docs/` | Every factual claim is verified against code or a run, or explicitly marked `UNVERIFIED` as in `verification_report.md` | Reviewer spot-checks cited file and line numbers |

`baseline_report.md` is the enforcement instrument for refactors and for the
total-count check above. Without a baseline, "the refactor did not break
anything" and "the whole suite ran" cannot be checked. Re-capture the baseline
whenever the suite intentionally changes size.

## 2. Forbidden changes

Each ban is paired with a detector. A "must not" without a detector is a
guideline, not a rule, and does not belong here.

| Forbidden | Why | How we detect the violation |
| --- | --- | --- |
| Editing `app/` or `fake-api/` to make a test pass | The test goes green because the product changed, not because a defect was fixed; the regression is masked | `git diff --stat` on a test task touches `app/` or `fake-api/`: reject and report the scope violation. Submission also rejects diffs against `scripts/protected-paths.txt` |
| Silently changing the session options that decide which build is installed and tested | The suite goes green against the wrong build | Diff review on `rule/DriverFactory.kt`, `pages/VariantLocator.kt`, `scripts/run-suite.*` and the `app.apk` and `ui.variant` properties in `appium-tests/build.gradle.kts`. The change description quotes the exact runner invocation with no extra `-D` arguments. On the bash runner `suite-run.log` must contain `Building stable debug APK` or `Building redesign debug APK`; the PowerShell runner does not write that line to the log, so on Windows the quoted `-Flavor` argument is the evidence and the reviewer reruns if in doubt |
| Hard-coded sleeps to "stabilize" a test | Hides the real timing problem and bloats runtime | Detector D1 below must stay at zero hits. It matches the call shape, not one class name: `TimeUnit` is already imported in `rule/`, so `TimeUnit.SECONDS.sleep` needs no new import. Synchronize through `Element.waitFor`, `waitForGone` and a justified `retryClick` |
| Locators bound to visible text, XPath or other unstable strategies | Break on the first copy change or localization; hide a wrong build behind a lucky match | Detector D2 below must stay at zero hits. The convention is `AppiumBy.id` on a Compose `testTag` |
| Weakening an assertion until it passes | The check stops seeing the bug it exists for, for example a bare number asserted where the UI renders a formatted price | Review every changed `assert*` line against what the screen or API actually renders. A test-repair diff that removes or loosens an assertion is rejected unless the expectation is shown wrong by source code |
| Adding retries, raising timeouts, muting or conditionally skipping a test to get green | Converts a defect into a flaky-looking pass | Diff on `retryClick` call sites and timeout constants, plus detector D3 below must stay at zero hits (the modules use JUnit Jupiter 6; `@Ignore` does not exist here). Any hit requires the failure evidence from `appium-tests/build/reports/failures/` and a stated root cause |
| Assertions inside `pages/` or `client/` | Breaks the layer contract; the failure point becomes untraceable | Detector D4 below must stay at zero hits. `error(...)` is permitted only as a precondition guard for a missing session or an unsupported variant, as in `pages/Device.kt`, `pages/VariantLocator.kt` and `client/ApiSpec.kt`; any `error(...)` that compares a product value is an assertion and is rejected in review |
| Reporting a count or fact from prose docs | Docs go stale; code and run artifacts are the source of truth | The claim must cite the code, the JUnit XML, the Allure summary or the log line. A citation to a README or to `agent_docs/` alone is not evidence |
| Sending real secrets, credentials, tokens, signing keys or personal data to a model | Protected data leaves its approved handling boundary and may be retained | Prompts and fixtures committed to the repository use only synthetic or redacted data. The reviewer greps the diff for credential shapes (`Bearer `, `token=`, `password`, `BEGIN .* PRIVATE KEY`, phone numbers outside `testdata/`). This repository has no automated secret scanning; the reviewer's grep is the only detector. If the data cannot be verified as safe, stop and do not send it |
| `git push` without approval for that specific push | Unreviewed work reaches the shared remote | The agent must quote the human approval for the push in the session before running it |

Detectors. Run each from the repository root on the changed branch; the
expected output is empty. Any hit names the offending file and line.

```bash
# D1: blocking sleeps
grep -rnE '\.sleep\(|Thread\.sleep|\bdelay\(|\.wait\(|parkNanos|pollDelay' appium-tests/src api-tests/src

# D2: unstable locator strategies
grep -rnE 'xpath|@text|accessibilityId|By\.(className|name|linkText|partialLinkText|tagName)|AppiumBy\.(className|androidUIAutomator)' appium-tests/src

# D3: muted or conditionally skipped tests
grep -rnE '@Disabled|assumeTrue|assumeFalse|assumingThat|@DisabledIf|@DisabledOn|@EnabledIf' appium-tests/src api-tests/src

# D4: assertions in the assert-free layers
grep -rnE 'assert\w*\(|\bcheck\(|\brequire\(|\bfail\(|\.then\(\)|\.statusCode\(|\bexpect\(' appium-tests/src/test/kotlin/pages api-tests/src/test/kotlin/client

# Scope: a test task must not touch protected paths
git diff --stat main...HEAD -- app fake-api .github/workflows package.json package-lock.json \
  appium-tests/src/test/kotlin/rule/DriverFactory.kt appium-tests/src/test/kotlin/pages/VariantLocator.kt
```

All four detectors return zero hits at revision `4639189`.

Safe context to give a model: the rules in `AGENTS.md` and this file, the
test conventions in `agent_docs/`, the load-bearing test data the task needs
from `testdata/`, and durable project context such as `baseline_report.md`.

## Verification standard

Verification must match the layer that changed.

- UI test layer: attach a fresh run from `scripts/run-suite.sh` or
  `scripts/run-suite.ps1` against the target APK, invoked without extra
  Gradle arguments. The proof is the root `suite-run.log` plus the JUnit XML
  under `appium-tests/build/test-results/test/` whose summed `tests=` matches
  the expected total. The PowerShell runner sums the totals itself and fails
  with "No tests executed. The run is not valid proof."; the bash runner
  passes `--rerun` but does not check totals, so on macOS and Linux the
  reviewer sums the XML.
- API test layer: start `./gradlew :fake-api:run`, execute
  `./gradlew :api-tests:test --rerun`, and attach the JUnit XML under
  `api-tests/build/test-results/test/` with the expected total and zero
  failures.
- Kotlin changes: `./gradlew ktlintCheck` passes.

Not proof: a green CI run (the workflow in `.github/workflows/ci.yml` never
executes Appium), "the agent said it passed", a Gradle task reported as
`UP-TO-DATE` or `FROM-CACHE` for the test task, or exit code 0 from a helper
command. A bare `./gradlew :appium-tests:test` is not a valid UI run: without
`--rerun` Gradle can report `BUILD SUCCESSFUL` from cache with zero tests
executed. A filtered run (`--tests`, `-TestFilter`) proves only the filtered
tests. An adb return code is not proof that a sandbox state applied; the
visible or API-level effect is.

## Marking AI-generated code

Every commit whose code an agent authored carries the trailer:

```text
Assisted-by: Claude Code
```

Where the tracker supports labels, the pull request also carries
`ai-assisted`. One convention, applied consistently, lets the team later ask
how much of the suite is machine-drafted and whether breakage clusters there.

Check: the reviewer runs
`git log --format='%h %(trailers:key=Assisted-by,valueonly)' main..HEAD` and
rejects a pull request in which an agent-authored commit lacks the trailer.
No commit at revision `4639189` carries it; the convention starts with the
first AI-assisted commit after this policy.

## 3. Human gates

Human approval is required before:

- merging any AI-assisted change;
- touching a path in `scripts/protected-paths.txt` or changing shared test
  infrastructure in `rule/`, `Element`, the runners, the two test
  `build.gradle.kts` files, or `testdata/`;
- accepting a "flaky test" verdict, muting or conditionally skipping a test,
  or weakening an existing check;
- running a filtered suite as acceptance evidence for anything other than a
  single-test change;
- any `git push`.

A gate is a decision point, not a review step that can be skipped under
deadline.

Review checklist for an AI-assisted change. Look at the complete set of
changed files together with the run evidence:

1. Does the diff touch `app/`, `fake-api/` or another protected path while
   the task is a test change? Reject under section 2.
2. Is a real run log attached, is the runner invocation quoted, and does the
   JUnit XML total match the expected count? No log, a filtered run, or a
   cached or `UP-TO-DATE` test task sends the change back.
3. Run detectors D1 to D4 and the scope diff from section 2 on the
   branch; then diff-review session options and loosened assertions.
4. Do checks live where the convention puts them? `pages/` and `client/`
   stay assert-free; the checks belong to `tests/` and `actions/`.
5. Is every factual claim in the description traceable to code or an
   artifact rather than to prose documentation?

Red flags that require rejection:

- production code touched to make a test pass;
- session options changed without a run traceable to the target build;
- no run log, or a log from a filtered run presented as a full run;
- "it worked" backed only by an exit code;
- a fact cited from stale docs;
- a new test reusing an existing Allure ID or duplicating existing coverage.

## 4. Stop conditions

The agent must stop and request human approval when:

- the task cannot be completed without changing something outside the agreed
  scope, including any protected path;
- a key claim cannot be verified against source code, a test result or a log;
  the agent stops rather than presenting it as fact;
- a check passes only after a retry, a timeout increase, a weakened assertion,
  reduced coverage, a mute, a skip, a test filter, or any other change that
  may hide the original failure;
- the task requires data whose safety for a model cannot be verified;
- the intended build flavor cannot be confirmed: on bash from the `Building
  <flavor> debug APK` line in `suite-run.log`, on PowerShell from the
  `-Flavor` argument of the invocation the agent itself ran.

## 5. Safe failure modes

Incomplete or unverified work is reported as such, never as success.

- If the emulator, Appium server or `fake-api` is unavailable, report
  `BLOCKED` and name the failed prerequisite. Do not report the change as
  verified.
- Mark every unsupported conclusion `UNVERIFIED`, following the convention in
  `verification_report.md`.
- Preserve and report failed checks. Do not hide, suppress, mute or rerun
  them away.
- Report the exact command run, including every argument passed to the
  runner, and the artifact path that proves the result, so the reviewer can
  reproduce the check.

A loud red test beats a quiet green.

## Control review log

Date: 2026-09-07. The policy was reviewed as an implementation control by an
agent with no prior context, then each candidate was verified against the
repository before patching.

Patched:

- Runner argument pass-through. `scripts/run-suite.sh:91-94` appends `"$@"`
  after its own `-Dapp.apk` and `-Dui.variant`, Gradle keeps the last value,
  and the log does not record the arguments; `scripts/run-suite.ps1:98-99`
  adds `--tests`. A filtered or re-targeted run therefore satisfied the old
  wording. Fix: the acceptance condition now requires the summed JUnit XML
  total to equal baseline plus new tests, the quoted invocation, and no extra
  runner arguments. `appium-tests/build.gradle.kts` and
  `api-tests/build.gradle.kts` were added to framework core.
- Sleep detector matched only `Thread.sleep` and `delay(`. `TimeUnit` is
  already imported in `rule/DriverFactory.kt` and `rule/ConditionControl.kt`.
  Fix: pattern widened to the call shape.
- Assertion detector matched the substring `assert`, which hit two comments
  in `pages/Element.kt:54` and `pages/MapPage.kt:31` and missed `check(`,
  `require(`, `.then()` and `.statusCode(`. Fix: pattern on code constructs,
  with the existing `error(...)` precondition guards enumerated as allowed.
- Factual corrections: `SandboxControl` renamed to `ConditionControl`;
  nonexistent `docs/` removed; `fixtures/` marked as absent at `4639189`;
  `@Ignore` replaced by the JUnit Jupiter markers; the claim that secret
  scanning runs on push removed; the claim that both runners force a real
  run corrected (only the PowerShell runner enforces a non-zero count); the
  PowerShell log noted as not containing the flavor line.

Recorded, not applied:

- Making the runners write their effective Gradle arguments into
  `suite-run.log` would close the pass-through gap durably. It changes
  `scripts/`, which is framework core under this policy, so it needs human
  approval and is left as a follow-up.
- Wiring the section 2 greps into a Gradle verification task next to
  `checkPageObjectBoundary` would make them run automatically. Same gate,
  same follow-up.
