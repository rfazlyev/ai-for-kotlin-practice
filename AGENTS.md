# AGENTS.md - AI for Kotlin Practice

This repository is a training sandbox for AI-assisted QA. Agent output remains
unverified until it is checked against source code, a real run or a log.

## AI policy

The full policy for AI-assisted work lives in [AI_POLICY.md](AI_POLICY.md).
Read it before any task that changes tests, shared helpers, locators, session
options or documentation. The rules that apply to every session:

- Never edit the product (`app/`, `fake-api/`) to make a test pass.
- Proof is an attached unfiltered run: `suite-run.log` plus JUnit XML whose
  total matches baseline plus new tests. A green CI, an exit code, a cached
  Gradle task or "the agent said so" is not proof.
- Code and run artifacts are the source of truth; a fact from prose docs is
  `UNVERIFIED` until checked.
- A check that passes only after a retry, longer timeout, weakened assertion or
  mute is a stop condition: stop and ask.
- Report blockers and unverified claims as `BLOCKED` or `UNVERIFIED`, never as
  success.
- Commits with agent-authored code carry the trailer `Assisted-by: Claude Code`.

## Safety boundaries

- Never edit `app/` or `fake-api/` to make a test pass. A test-repair task may
  change only the test layers unless the human explicitly requests a product
  change.
- Never weaken assertions, increase timeouts or add retries without evidence
  that the test expectation is correct and the synchronization is the defect.
- Never run `git push` without explicit approval for that specific push in the
  current session.
- Keep repository prose, code comments and UI text in English.
- Treat `.agents/skills` as the canonical source for shared agent skills. After
  editing one, run `python scripts/sync_agent_skills.py`; do not edit its
  generated `.claude/skills` mirror directly.
- Preserve the pinned Appium toolchain and the Appium session safeguards in
  `DriverFactory.kt`.
- Treat every path in `scripts/protected-paths.txt` as pre-supplied
  infrastructure. In particular, do not edit `pages/VariantLocator.kt` or add
  fallback locator attempts.

## Product and backend

- The Android app calls the local Ktor `fake-api` over real HTTP. The emulator
  reaches the host at `10.0.2.2:8080`.
- Start the backend with `./gradlew :fake-api:run`. Swagger UI is served at
  `http://localhost:8080/swagger` from `fake-api/openapi.yaml`.
- Bearer tokens belong to the `X-Sandbox-Session` that issued them. Sandbox
  reset restores product state but does not revoke the session's tokens.
- The main flow is phone and OTP authentication, location onboarding, pickup
  and destination entry, tariff selection, driver search, ride completion or
  cancellation, notifications and order history.
- Each sandbox session has at most one active ride. The app restores that ride
  asynchronously through `GET /rides/active` without blocking the ride form.
  Active rides have no TTL; only complete, cancel and reset release the slot.

## Build and run

- Use JDK 17 and the Gradle wrapper.
- Install the pinned Node dependencies with `npm ci`.
- Build stable with `./gradlew :app:assembleStableDebug` and redesign with
  `./gradlew :app:assembleRedesignDebug`.
- Start Appium only through `scripts/start-appium.ps1` or
  `scripts/start-appium.sh`.
- Run UI tests through `scripts/run-suite.ps1` or `scripts/run-suite.sh`.
  The runners select the APK, disable animations and force a real test run.
- Run API tests with `./gradlew :api-tests:test --rerun` while `fake-api` is
  running.
- Run `./gradlew ktlintCheck` before committing Kotlin changes.

## Appium test architecture

The layers are:

1. `rule/`: session lifecycle, driver configuration, sandbox control and
   failure evidence.
2. `pages/`: singleton `Element` catalogs. Pages contain no assertions.
3. `actions/`: interactions, flows, waits and assertions.
4. `tests/`: JUnit scenarios written as named Allure steps.
5. `testdata/`: shared values.

Use Compose `testTag` resource IDs through `AppiumBy.id`. Do not use text XPath
locators. Do not use `Thread.sleep`; synchronize through `Element.waitFor`,
`waitForGone` and narrowly justified `retryClick` calls.

`AppiumTestCase.startAuthorized` defaults to true for post-authentication
tests. Set it to false only when onboarding itself is under test.

## API test architecture

- `rule/ApiTestCase` owns common setup and token acquisition.
- `client/` owns REST Assured request construction and contains no assertions.
- `model/` contains wire DTOs local to the test module.
- `tests/` contains scenarios and assertions.
- Every stateful test uses an isolated sandbox session and resets it.

## Sandbox states

The six state IDs are `slow_backend_response`, `backend_error`,
`car_unavailable`, `driver_not_found`, `intermittent_backend_delay` and
`region_unavailable`.

```text
adb shell am broadcast -n com.sandbox.qa/.condition.ConditionReceiver --es condition slow_backend_response --ez enabled true
adb shell am broadcast -n com.sandbox.qa/.condition.ConditionReceiver --ez reset true
```

The adb return code is not proof that a state applied. Verify the visible or
API-level effect. Enable states after an Appium session starts because session
creation relaunches the app process.

## Starter tests

The repository starts with six Appium cases in:

- `OnboardingSmokeTest`
- `PasskeyPromoTest`
- `RideAndHistoryE2ETest`

The API starter contains four cases in `ApiSmokeTest` and
`RideLifecycleApiTest`. New tests must use unused repository Allure IDs and
must not duplicate existing coverage.

## CI

The GitHub workflow builds and compiles the project and runs the API starter suite.
It does not execute Appium tests. A green CI result is therefore not proof of a
green UI suite; use the local sequential runner and inspect its non-zero test
count.
