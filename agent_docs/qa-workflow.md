# QA workflow record: API-2007

Instructions: `.agents/skills/automate-test-case/SKILL.md`.

## Case

- ID: `API-2007`, layer API.
- Source: `test-cases/test-cases.xlsx`, sheets `Case Summary` and `Steps`.
- Title: An empty destination is rejected without creating an active ride.
- Preconditions: a fresh authorized sandbox session exists with default
  settings and no active ride.
- Actions and expected results:
  1. `POST /rides` with the bearer token and body
     `{"from":"Oak Avenue","to":"","rideOptionId":1}` -> HTTP 400 with error
     "Both from and to are required".
  2. `GET /rides/active` with the same token and sandbox session -> HTTP 404
     with error "No active ride".

## Repository state

- Revision: `4a92b70176d038b41f94fb59f60072984225fd53`.
- Uncommitted changes under `api-tests/` and `appium-tests/`: none at start.
  Uncommitted changes outside the suites: Week 4 package installation and the
  move of `AI_POLICY.md` and `task-automation-readiness.md` into `agent_docs/`.

## Steps

| Step | Instructions | Status |
|---|---|---|
| 1. Readiness status | `automate-test-case` step 1 | `READY`, reused |
| 2. Generate the test | `gen-api-test` | Test created |
| 3. Execution result | `gen-api-test` verification | `VERIFIED`, new run in this invocation |
| 4. Case check | `automate-test-case` step 4 | Conforms, no gaps |

### 1. Readiness status

`READY`, reused from the workbook label `READY FOR AUTOMATION` in
`test-cases/test-cases.xlsx` (`Case Summary`, column `Automated Test`). No
reassessment was requested, so no new assessment ran and no
`.agent-state/automate-test-case/API-2007/readiness.md` was written.
Context: the earlier multi-case report `agent_docs/task-automation-readiness.md`
also assesses API-2007 as `READY` and is left unchanged.

### 2. Generate the test

`gen-api-test` received the complete case and the `READY` result.

- Assigned ID: `@AllureId("2007")` was free (inventory before generation:
  2000-2004, 2006, 2008). No existing method covered an empty destination.
- Plan: `agent_docs/automation-plans/API-2007.md`, created from
  `agent_docs/templates/automation_plan.api.workflow.md.template` and
  validated against the case, `fake-api/openapi.yaml` (`POST /rides` 400,
  `GET /rides/active` 404) and current `api-tests` sources. OpenAPI does not
  fix the 400 message, so a fresh isolated-session probe confirmed
  `400 {"error":"Both from and to are required"}` and
  `404 {"error":"No active ride"}`.
- Target: `tests.EmptyDestinationApiTest.testEmptyDestinationIsRejectedWithoutActiveRide`.
- Changed paths: `api-tests/src/test/kotlin/tests/EmptyDestinationApiTest.kt`
  (new), `agent_docs/automation-plans/API-2007.md` (new). Helpers reused
  unchanged: `ApiTestCase`, `RidesApi.create`/`active`, `ErrorResponse`,
  `ApiTestData.FROM`, `ApiTestData.YELLOW_TARIFF`.
- `./gradlew ktlintCheck`: passed.

### 3. Execution result

`VERIFIED`. New run in this invocation; nothing reused. The generator's
exact-target run is the workflow evidence and was not repeated.

- Command: `./gradlew :api-tests:test --tests tests.EmptyDestinationApiTest.testEmptyDestinationIsRejectedWithoutActiveRide --rerun`
  with `fake-api` started by `./gradlew :fake-api:run`.
- JUnit: `tests="1" skipped="0" failures="0" errors="0"`, timestamp
  2026-10-04T18:09:19Z.
- Allure: result `passed`, `AS_ID` 2007; step "Order a ride with an empty
  destination" attaches `POST /rides` body `{"from":"Oak Avenue","to":"","rideOptionId":1}`
  and response `400 {"error":"Both from and to are required"}`; step "No
  active ride was created" attaches `GET /rides/active` with the same token and
  `X-Sandbox-Session` and response `404 {"error":"No active ride"}`.
- Evidence (git-ignored): `.agent-state/automate-test-case/API-2007/evidence/target-run/`
  (`command.log`, JUnit XML, `allure-results/`).
- Source state: revision `4a92b70` plus the untracked test file above.
- Additional `AI_POLICY.md` proof, not a substitute for the target run:
  unfiltered `./gradlew :api-tests:test --rerun` executed 8 tests (7 existing
  plus 1 new), 0 failures, 0 errors, 0 skipped. Evidence:
  `.agent-state/automate-test-case/API-2007/evidence/full-suite/`.

### 4. Case check (read-only)

Inputs: the case above, `EmptyDestinationApiTest.kt`, the helpers it calls,
the plan and the step 3 evidence. Result: the test conforms to the case; no
gaps.

| Case requirement | Test location | Result |
|---|---|---|
| Fresh sandbox session with default settings | `rule/ApiTestCase.kt:15-27` (new session ID, `POST /sandbox/reset`, HTTP 200 checked) | Met |
| Authorized session | `EmptyDestinationApiTest.kt:19` `obtainToken()` | Met |
| No active ride before the request | New session plus reset; the test creates no ride before action 1 | Met by setup (not asserted separately; not required by the case) |
| Action 1: `POST /rides` `{"from":"Oak Avenue","to":"","rideOptionId":1}` with the bearer token | `EmptyDestinationApiTest.kt:22`; Allure request body matches exactly | Met |
| Expected 1: HTTP 400, error "Both from and to are required" | `EmptyDestinationApiTest.kt:23-24` | Met |
| Action 2: `GET /rides/active`, same token and session | `EmptyDestinationApiTest.kt:27`; Allure shows the same token and `X-Sandbox-Session` | Met |
| Expected 2: HTTP 404, error "No active ride" | `EmptyDestinationApiTest.kt:28-29` | Met |
| Evidence belongs to this test and source state | Step 3 target-run JUnit and Allure | Met |

Note: `ErrorResponse` equality also requires `code` to be absent. The case
does not mention `code` and the current responses carry none; if the backend
adds a `code` to these errors, the test will fail and the change must be
reviewed against the case.

## Remaining work

- Commit the test and plan with the `Assisted-by: Claude Code` trailer when
  approved; not done in this workflow.
- The workbook `Automated Test` cell still says `READY FOR AUTOMATION`; update
  it to the test reference if the workbook is maintained.
