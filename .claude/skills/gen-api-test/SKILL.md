---
name: gen-api-test
description: Assess existing API test coverage when requested, or plan, implement and verify assigned API Test Cases with Kotlin, JUnit 5 and REST Assured. Follow the rule/client/model/tests/testdata architecture and the relevant OpenAPI operations. Use for API coverage analysis and test automation, not Appium tests, backend fixes or open-ended repair.
---

# Generate an API test

## Purpose

Assess existing API coverage or implement and verify assigned API Test Cases,
according to the user's request. Treat OpenAPI as the specified contract and a
fresh response as runtime evidence. Report any disagreement; do not rewrite
either source.

## Required inputs

| Input | Requirement |
|---|---|
| Scope | Supplied API Test Cases with preconditions, request data and expected results; for a coverage assessment, the selected cases or requirements to compare |
| Plan | Required before creating or changing test code; create it from `agent_docs/templates/automation_plan.api.workflow.md.template` when missing, or validate the existing `agent_docs/automation-plans/<case-id>.md` for this Test Case |
| Contract | Relevant operation from `fake-api/openapi.yaml` |

Use the complete Test Case text supplied by the task. Do not assume that a case
exists under `fixtures/`.

## Choose the requested task

| Request | Action |
|---|---|
| Assess existing coverage | Compare the selected requirements with current tests and report the evidence. No plan, code change or test run is required by this assessment alone. |
| Plan or automate supplied Test Cases | Process each selected case under its assigned Allure ID. Continue with assigned-test identification below; coverage under another ID does not remove a case from the assignment. |
| Implement only missing coverage | Assess coverage first, then implement the missing or incomplete cases within the requested scope. |

A supplied case or list of cases followed by an instruction to implement or
automate them selects automation. Do not insert an unsolicited coverage
assessment that skips those cases. For a batch, preserve the user's selection and order and
apply the implementation procedure to each case separately.

## Assess existing coverage

Use this procedure for a coverage request, including an explicit request to
implement only missing coverage:

1. Search current API test sources using the `@DisplayName|@AllureId` inventory
   command from `AGENTS.md`. Read candidate methods and their relevant helpers.
2. Compare the selected preconditions, actions and every expected result with
   the setup, requests and assertions in those tests. A shared endpoint, similar
   name or matching ID alone does not establish coverage. Equivalent behavior
   may be implemented under another ID; record that target and ID explicitly.
3. For each case or requirement, report whether coverage is complete, partial,
   absent, or cannot be established from the available evidence. Cite exact
   test targets and source locations, and name the missing assertions or
   uncertain requirements.
4. Distinguish source coverage from execution proof. Cite any available run
   evidence with its scope and freshness; do not describe source inspection as
   a fresh passing run.

After an assessment-only request, return the findings. Continue to planning or
implementation only when that work is part of the user's request. If coverage
analysis accompanies an instruction to automate all supplied cases, retain all
of them. Omit cases only when the user explicitly asks for missing coverage or
for already implemented assigned cases to be skipped as described below.

## Identify the assigned test

Search the current API test sources for the Test Case's assigned `@AllureId`.

- If no method has that ID, implement the assigned case as a new test.
- If one method implements this case, use it and complete any missing case
  requirements. For an automation request, if it already satisfies the full case,
  proceed to verification without creating another test or a new plan.
- If the ID belongs to unrelated behavior or multiple methods, stop and report
  the conflicting targets. Do not remap the ID or create another test with it.

Tests with other IDs may provide implementation examples. They do not discharge
an explicit assignment to automate this case, even when their behavior is
equivalent. Create or validate the plan before changing test code.

If the request explicitly says to skip already implemented cases, compare the
assigned-ID test and its helpers with the complete case before editing or running.
Check preconditions, actions, every expected result and resulting state. When all
requirements are already implemented by an enabled JUnit test, return
`ALREADY_IMPLEMENTED` with the exact
target and a requirement-to-source mapping. Do not create a plan, change files or
execute the test for this outcome. It records a source-level implementation
decision, not a fresh passing run. An ID match alone is insufficient. If the test
is incomplete, implement the missing requirements and verify it normally.

## Plan creation and validation

Use `agent_docs/templates/automation_plan.api.workflow.md.template` as the required schema. Do not add
sections or leave placeholders.

1. If `agent_docs/automation-plans/<case-id>.md` is missing, create it for the supplied Test Case.
2. If it already describes the same Test Case, validate every claim against
   the relevant OpenAPI operation and current repository sources. Correct a
   stale path, symbol, value or command only when current evidence supports one
   unambiguous replacement.
3. Use a fresh response when the plan depends on runtime behavior that OpenAPI
   does not specify. Stop on contract drift instead of rewriting the Test Case,
   contract or backend.
4. Mark the plan validated only after its Test Case mapping, contract and
   repository citations, allowed files, stop conditions and verification
   commands are complete and internally consistent.
5. If an existing plan describes another Test Case, do not overwrite it unless
   the current request explicitly authorizes replacement.
6. If evidence is missing or contradictory, stop before test-code changes and
   report the exact plan item that cannot be validated.

For a planning-only request, stop after writing and validating
`agent_docs/automation-plans/<case-id>.md`. For a generation request, continue with the validated
plan.

## Required context

`AGENTS.md` is already loaded; use its repository rules and source-routing
table without reading the file again. Before assessing, planning or editing, read:

1. `agent_docs/AI_POLICY.md`;
2. `api-tests/README.md`;
3. `agent_docs/building_the_project.md`;
4. the relevant operation in `fake-api/openapi.yaml`;
5. the nearest API test, endpoint client, models and test data.

Use these sources for layer ownership, black-box boundaries, test metadata,
formatting and execution commands. Keep those rules in their source documents;
do not restate them in this skill.

## Pre-generation checks

1. Verify every `agent_docs/automation-plans/<case-id>.md` claim against the contract and current
   repository files.
2. Confirm that every planned file is permitted by the validated plan and
   repository policy.
3. Apply the assertion rules below.

Preserve the Test Case's assigned `@AllureId` on its one implementation. Before
adding a new method, verify that the ID is still available. Do not invent or
remap the ID during generation.

Abort generation before editing when any condition applies:

- expected behavior is ambiguous or conflicts with verified runtime behavior;
- the scenario contains more than one independent objective;
- an exact value has no support in the scenario or contract;
- the oracle can pass on pre-existing state;

## Assertion rules

| Data or behavior | Required assertion |
|---|---|
| Contract field | Assert the field and the value or invariant required by the scenario |
| Example or test-data value | Assert exact identity only when the scenario or contract requires it |
| Generated resource | Capture its ID or response identity and correlate later lifecycle or collection reads |
| Error outcome | Assert the documented status and meaningful error body |

Reject status-only and list-size-only checks when the scenario names response
values. Reject full-object equality when unrelated response fields are outside
the expected result. Keep expected values independent from the system under
test.

## Verification and result

1. Start `fake-api` with the setup documented in `api-tests/README.md` and
   `agent_docs/building_the_project.md`.
2. Run only the assigned test's exact `package.Class.method` with the API test
   task, `--tests package.Class.method` and `--rerun` (or `--rerun-tasks`). Do
   not run the whole API suite as verification for this one-case workflow.
3. Require fresh JUnit XML containing exactly one matching, passing,
   non-skipped test. A cached task, zero matching tests, or another passing
   target does not verify the assigned test, including an existing implementation.
4. Inspect the matching Allure scenario step and attached HTTP request and
   response.

On success, report the Test Case ID, exact target, changed files, assertions,
command and observed result for the generated test. On failure, report the
smallest relevant evidence and stop. Test repair and backend modification
require separate authorization.
