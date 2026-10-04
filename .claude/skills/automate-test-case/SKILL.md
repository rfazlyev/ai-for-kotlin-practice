---
name: automate-test-case
description: Automate one supplied API or mobile test case end to end - establish its readiness status, delegate generation to gen-api-test or gen-mobile-test, establish an exact-target execution result, and check the resulting test against the case. Use for the full case workflow; use the readiness instructions, a generation skill or test-repair directly for a single operation.
---

# Automate one test case

## Inputs and scope

| Input | Requirement |
|---|---|
| Case | Exactly one complete case: ID, layer (API or mobile), preconditions, actions, every expected result. Several cases or missing fields: ask before implementing. |
| Repository | The current checkout. Record the revision and uncommitted changes under the test suites before any run. |
| Earlier record or evidence | Optional context only. It never counts as execution proof for this invocation. |

Rules:

- Follow `AGENTS.md`, `agent_docs/AI_POLICY.md` and the execution and repair
  permissions in effect.
- Expected behavior comes from the case and the contract
  (`fake-api/openapi.yaml`), not from observed product output.
- This skill authorizes no push, product change, new framework or additional
  test case.
- Each delegated skill owns its own procedure and evidence rules. This skill
  routes between them and records their outcomes.
- If a referenced instruction file is missing, stop and report it.

Outputs: the readiness report at
`.agent-state/automate-test-case/<case-id>/readiness.md` (step 1) and the
workflow record at `agent_docs/qa-workflow.md`, or
`agent_docs/qa-workflow.<case-id>.md` when the default file already holds
another case (see Workflow record). Update the record after each step.

## 1. Establish the readiness status

1. Read the case's saved status. Recognized values: `READY` (workbook label
   `READY FOR AUTOMATION`), `BLOCKED`, `NEEDS_CLARIFICATION`. Any other value
   (`TODO`, blank, a test path) means no status.
2. With a recognized status and no request to reassess: record the value and
   its source and skip the assessment.
3. Otherwise assess the case with
   `agent_docs/task-automation-readiness-instructions.md`, or the instructions
   the request names, and write the selected-case report to `readiness.md` or
   the path the request names. Leave an existing report that covers several
   cases unchanged; it is context for a reused status, not this report.

| Status | Next |
|---|---|
| `READY` | Step 2. |
| `BLOCKED` | Record the missing capability and its evidence. Stop. |
| `NEEDS_CLARIFICATION` | Record the question whose answer changes the decision. Stop. |

## 2. Generate the test

Invoke `gen-api-test` (API) or `gen-mobile-test` (mobile) with the original
case and the readiness result. The generation skill owns test identification by
assigned ID, the plan, the implementation and its own run.

Keep the assigned case ID. A test under another ID is an implementation
reference, not a substitute. If the user asked to skip implemented cases,
`ALREADY_IMPLEMENTED` from `gen-api-test` ends the workflow: record the target
and its requirement-to-source mapping, run nothing, skip step 4. It is a
source-level result, not a passing run.

| Generation result | Next |
|---|---|
| Test created or completed, or an existing assigned-ID test found | Step 3 with the target `Class.method`, the plan and the changed paths. |
| `BLOCKED`: the assigned ID maps to unrelated behavior or to several methods | Record the conflicting targets. Stop; do not remap the ID or create a duplicate. |
| Plan conflicts with the case or the contract | Record the conflict. Stop. |

## 3. Establish the execution result

Required evidence: one fresh run of the exact target from this invocation with
JUnit XML, the Allure result and the command log, matched to the current
sources under the rules of `gen-api-test` (API) or `run-appium-suite` (mobile).
If generation already ran the exact target on the final sources, reuse that
run; do not run it twice. For mobile, an earlier matching run may be reused
when the user does not request a new one: record its time and state that no
test ran in this invocation.

Without matching evidence, run the exact target:

| Layer | Command |
|---|---|
| API | API test task with `--tests package.Class.method` and `--rerun`; backend per `api-tests/README.md`. |
| Mobile | `run-appium-suite` with its test filter. |

| Result | Next |
|---|---|
| `VERIFIED`: the target passed and the evidence matches the current sources | Step 4. |
| `FAILED`: the target failed | Failure route. |
| `VERIFICATION_INCOMPLETE`: no run, setup failure, skipped or zero tests, evidence mismatch | Record the missing proof. When execution is unavailable or out of scope, step 4 may still compare the sources with the case and reports a source-only check, not conformance of a verified test. |

Failure route: with repair permission, invoke `test-repair` for the exact
target and adopt its classification and outcome. `PRODUCT_BUG`,
`NEEDS_INVESTIGATION`, `INFRASTRUCTURE_ISSUE` and `EXHAUSTED` end the workflow
with that status. After a verified fix, continue to step 4 with the new
evidence; earlier evidence no longer covers the changed code.

For an unchanged mobile test whose coverage comparison from step 2 is complete,
reuse that comparison in place of step 4.

## 4. Check the test against the test case

Inputs: the complete original case; the final test and the helpers it calls;
the plan when one exists; the exact-target JUnit, Allure and request/response
evidence from step 3 when a run exists.

Check, read-only:

1. Preconditions: the test establishes every precondition of the case (reset,
   authentication, sandbox state, prior ride).
2. Actions: it performs the case's actions in the case's order against the same
   operation and parameters.
3. Expected results: it asserts every expected result, including resulting
   state such as the active ride or order history. An assertion inside a helper
   counts; a passing run without the assertion does not.
4. Evidence: the run evidence belongs to this test and this source version;
   without a run, mark the check source-only.

Report each gap as: case requirement, test or helper path and line, missing
behavior, consequence, required change. Report anything that the inputs cannot
establish as unverified instead of guessing. A passing run is not conformance;
keep the two separate.

A correction needs authorization and stays within the test layers. After it,
revalidate the plan when the change affects it, repeat step 3, then repeat this
check.

## Workflow record

The workflow record and the final summary contain:

- case ID and source; revision and uncommitted test-suite changes;
- steps performed, instructions used, status of each;
- target `Class.method`, plan path, changed paths;
- execution status, evidence paths, the revision and source state they belong
  to, reused or new;
- case check result: each gap with requirement, location, consequence;
- remaining work and next action.

A stop with `BLOCKED`, `NEEDS_CLARIFICATION`, `FAILED`,
`VERIFICATION_INCOMPLETE` or a repair stop status is a valid result, and
`ALREADY_IMPLEMENTED` is a source-level result. Report them as such, not as a
completed passing test.
