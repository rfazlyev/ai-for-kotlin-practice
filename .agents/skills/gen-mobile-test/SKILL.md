---
name: gen-mobile-test
description: Create or validate a case-specific automation plan and generate one Kotlin/JUnit 5 Appium test from one TestOps-style Test Case in this repository. Use when asked to plan, generate or cover one mobile Test Case. Follow the pages-to-actions-to-tests architecture, verified product locators and Allure conventions; never use for API tests, product fixes or open-ended repair.
---

# Generate a mobile test

## Purpose

Create or validate one repository-specific automation plan, then create one
verified Appium test from the supplied mobile scenario. Use the Test Case for
expected behavior, the automation plan for design decisions and current
repository sources for implementation facts.

## Required inputs

| Input | Requirement |
|---|---|
| Test Case | One case with preconditions, actions and expected results |
| Plan | Required working artifact after the coverage preflight confirms a gap; create it from `agent_docs/templates/automation_plan.mobile.workflow.md.template` when missing, or validate the existing `agent_docs/automation-plans/<case-id>.md` for this Test Case |

Use the complete Test Case text supplied by the task. If the task supplies a
case file, read the whole file because its header may define test data. Do not
assume that a case exists under `fixtures/`.

## Coverage preflight

Run this preflight before validating the plan or loading implementation-specific
context:

1. Search current mobile coverage with
   `rg -n '@DisplayName|@AllureId' appium-tests/src/test/kotlin/tests`; there is no registry file.
2. Compare the Test Case behavior and expected result with current tests.
3. If current coverage already proves the complete Test Case behavior, stop.
   Cite the existing file, class and test method, retain the coverage
   comparison, and report that no files changed.
4. If the assigned `@AllureId` is occupied but the existing test does not prove
   equivalent behavior, stop. Report the conflicting target and the unmet Test
   Case behavior. Do not generate a duplicate, remap the ID or claim coverage.

Neither stop requires an automation plan. Only equivalent existing behavior is
coverage. When the preflight confirms a coverage gap and the assigned ID is
available, create or validate the plan before editing test code.

## Plan creation and validation

Use `agent_docs/templates/automation_plan.mobile.workflow.md.template` as the required schema. Do not add
sections or leave placeholders. Replace `<case-id>` in the plan path with the
supplied case ID, for example `API-2007` or `MOB-1007`.

1. If `agent_docs/automation-plans/<case-id>.md` is missing, create it for the supplied Test Case.
2. If it already describes the same Test Case, validate every claim against
   current repository sources. Correct a stale path, symbol, value or command
   only when current evidence supports one unambiguous replacement.
3. Mark the plan validated only after its Test Case mapping, citations,
   allowed files, stop conditions and verification commands are complete and
   internally consistent.
4. If an existing plan describes another Test Case, do not overwrite it unless
   the current request explicitly authorizes replacement.
5. If evidence is missing or contradictory, stop before test-code changes and
   report the exact plan item that cannot be validated.

For a planning-only request, stop after writing and validating
`agent_docs/automation-plans/<case-id>.md`. For a generation request, continue with the validated
plan.

## Required context

`AGENTS.md` is already loaded; use its repository rules and source-routing
table without reading the file again. Before planning or editing, read:

1. `agent_docs/AI_POLICY.md`;
2. `agent_docs/test_architecture.md`;
3. `agent_docs/page_object_model.md`;
4. `agent_docs/building_the_project.md`;
5. `appium-tests/README.md`;
6. `agent_docs/baseline_report.md` when present;
7. the nearest test and every page, action, locator and test-data file cited
   by the plan.

Use these sources for layer ownership, locator policy, start state, test
metadata, waits, formatting and execution commands. Keep those rules in their
source documents; do not restate them in this skill.

## Pre-generation checks

1. Verify plan citations against the nearest test, pages, actions, locators
   and test data.
2. Read product UI code only to confirm existing testTags.
3. Confirm that every planned file is permitted by the validated plan and
   repository policy.
4. Apply the assertion rules below.

Use the `@AllureId` assigned by the Test Case and verify that it is not already
present. Do not invent or remap the ID during generation.

Stop without editing when any condition applies:

- the plan remains incomplete or inconsistent with the Test Case after the
  creation or validation pass;
- the plan relies on an existing repository path, symbol, locator or value
  that current sources do not support;
- the Test Case combines behaviors that require separate scenarios;
- the oracle can pass on pre-existing or adjacent UI state;

Report the finding and stop. Do not redesign, split or relabel the scenario
during generation.

The validated plan may introduce a new action-layer method or wait condition
when the Test Case requires it, current sources contain no equivalent and the
change stays within the plan's validated test-layer files. Never invent a
product locator. A locator for an existing UI element must use its verified
current product `testTag`. A locator used only to assert that a removed element
stays absent must be explicitly required by the validated plan and supported
by the repository's migration contract.

## Assertion rules

- Map every assertion to one expected result in the Test Case.
- Assert exact copy or test-data identity only when the Test Case requires it.
- Use an observable result that fails when the behavior named in the title
  breaks.
- Do not add adjacent-content checks from an existing test.
- Keep expected values independent from the system under test.

## Verification and result

1. Delegate execution to `run-appium-suite`.
2. Run the generated class through the repository runner when the current OS
   runner supports a class filter. If it does not, use the unfiltered runner
   and verify that the generated test executed.
3. Require a fresh result for the generated Test Case with zero failures and
   errors. Do not require a fixed full-suite total for this one-test task.
4. Run the repository formatting check for changed Kotlin.

On success, report the Test Case ID, changed files, assertions, command and
observed result for the generated test. On failure, report the smallest
relevant evidence and stop. Test
repair and product modification require separate authorization.
