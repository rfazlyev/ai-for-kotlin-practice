---
name: gen-api-test
description: Generate one Kotlin/JUnit 5 REST Assured API test from one validated
    automation_plan.md and the relevant OpenAPI operation in this repository. Use when asked to generate or cover
    one API scenario. Follow the rule/client/model/tests/testdata architecture
    and verify exact response behavior; never use for Appium tests, backend fixes or open-ended repair.
---

# Generate an API test

## Purpose

Create one verified black-box API test from one validated scenario. Treat
OpenAPI as the specified contract and a fresh response as runtime evidence.
Report any disagreement; do not rewrite either source.

## Required inputs

| Input | Requirement |
|---|---|
| Test Case | One API behavior with explicit preconditions, request data and expected results |
| Plan | Required after the coverage preflight confirms a gap; `automation_plan.md` created from `automation_plan.api.md.template` for this Test Case and accepted by the automated validator |
| Contract | Relevant operation from `fake-api/openapi.yaml` |

Use the complete Test Case text supplied by the task. Do not assume that a case
exists under `fixtures/`.

## Coverage preflight

Run this preflight before validating the plan or loading implementation-specific
context:

1. Search current API coverage and run the `@DisplayName|@AllureId` inventory
   command from `AGENTS.md`; there is no registry file.
2. Compare the Test Case behavior and expected result with current tests.
3. If current coverage already proves the behavior, or the assigned
   `@AllureId` is occupied, stop. Cite the existing file, class and test method,
   state that no duplicate will be generated and report that no files changed.

A duplicate-coverage stop does not require an automation plan. When the
preflight confirms a coverage gap, require a plan that passed automated
validation. If the plan is missing, failed validation, is based on the wrong
template or describes another Test Case, stop and request a corrected plan.
Drafting or redesigning the plan is a separate planning task. The validation
result may be supplied by the current task or lesson handoff; separate human
approval is not required.

## Required context

`AGENTS.md` is already loaded; use its repository rules and source-routing
table without reading the file again. Before planning or editing, read:

1. `AI_POLICY.md`;
2. `api-tests/README.md`;
3. `agent_docs/building_the_project.md`;
4. the relevant operation in `fake-api/openapi.yaml`;
5. the nearest API test, endpoint client, models and test data.

Use these sources for layer ownership, black-box boundaries, test metadata,
formatting and execution commands. Keep those rules in their source documents;
do not restate them in this skill.

## Pre-generation checks

1. Verify every `automation_plan.md` claim against the contract and current
   repository files.
2. Confirm that every planned file is permitted by the validated plan and
   repository policy.
3. Apply the assertion rules below.

Use the `@AllureId` assigned by the Test Case and verify that it is not already
present. Do not invent or remap the ID during generation.

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

1. Start `fake-api` and run the full API suite with the commands documented in
   `api-tests/README.md` and `agent_docs/building_the_project.md`.
2. Require a fresh run that bypasses the Gradle test cache.
3. Inspect the Allure scenario step and attached HTTP request and response.

On success, report the Test Case ID, changed files, assertions, command and
observed result for the generated test. On failure, report the smallest
relevant evidence and stop. Test repair and backend modification require
separate authorization.
