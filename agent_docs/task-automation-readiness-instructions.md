# Task automation readiness

## Run the assessment

Provide the test cases, the repository or relevant product and automation sources, and any case IDs or selection filter. Cases may be supplied as a spreadsheet, document or text.

```text
Follow agent_docs/task-automation-readiness-instructions.md.
Assess the supplied test cases in the requested scope.
Write a compact task-automation-readiness.md report.
Perform the assessment only. Do not implement or execute tests.
Record clarification questions in the report and complete all selected cases.
```

## Inputs and case selection

- Follow the user's requested case IDs or filter. Without an explicit selection, assess cases marked `TODO` in a coverage field if one is supplied; otherwise assess all supplied cases.
- Read each selected case's preconditions, actions and expected results. When details span sheets or files, match them by case ID. Do not assume a filename, sheet layout, case count or test-layer split.
- Record missing or conflicting case details without silently adding, omitting or substituting cases.
- Coverage markers such as `TODO` select inputs; they do not determine readiness.

## Assessment procedure

1. Read applicable repository instructions and relevant product and test documentation.
2. Check the requested behavior against the current implementation and contract. For API cases, verify HTTP methods and paths; record missing endpoint details instead of inventing them.
3. Check whether the automation stack can prepare and clean up the required data, perform the actions and observe every expected result.
4. Compare existing coverage by setup, actions and assertions. A similar name or shared endpoint alone does not prove equivalent coverage.
5. Assign a status from evidence and specify the next action. Do not force a status distribution or classify by keywords.

- Cite decisive sources using a repository-relative path and symbol or line number, or an identifiable supplied document section.
- Confirm missing capabilities from the relevant implementation or contract. A failed keyword search or missing documentation is not proof of absence; mark unresolved facts as unknown.
- Do not derive the intended requirement from what the product happens to support. When plausible interpretations change the decision, ask what was intended.

## Readiness statuses

| Status | Assign when | Required next action |
|---|---|---|
| `READY` | The full case can be implemented and verified with the existing product and automation capabilities. | Name the next implementation step and the result to assert, or cite equivalent existing coverage. |
| `BLOCKED` | A confirmed missing product or automation capability prevents full verification of the case. | Name the missing capability, the evidence for the gap and the change needed before automation can proceed. |
| `NEEDS_CLARIFICATION` | A requirement, contract, verification criterion or evidence gap prevents a decision, and no sufficient blocker is confirmed. | Ask a specific question whose answer would change the decision. |

- Use `BLOCKED` when a confirmed blocker already prevents the case; record additional clarification needs in the same row.
- Record clarification questions in the report and complete the remaining cases. Do not wait for answers or invent them.
- A missing page object, client method or test class is normal implementation work when the underlying behavior is supported.
- `READY` does not mean implemented, passed or executed.
- If equivalent coverage already exists, recommend updating the coverage record instead of creating a duplicate test.

## Scope rules

- Assess software capabilities from sources. Do not inspect installed tools, running services, connected devices or credentials. Local execution setup does not affect readiness.
- Preserve the case's requested behavior, test layer, device requirements and complete expected result.
- A mock, partial check or alternative scenario does not establish the original case's readiness unless it satisfies the complete requirement.
- Do not classify unexecuted requirements as observed failures or invent execution results.
- Write only the assessment report. Do not run tests, modify input files or product/framework code, change runtime state, install tools or consult answer keys.

## Report: task-automation-readiness.md

Save the report in the repository root, or the location requested by the user. Include only a one-line note identifying the input, selection and code revision (including relevant local changes), followed by this table:

| Case | Readiness | Reason and evidence | Next action or question |
|---|---|---|---|

- Use the case ID or, if absent, its title. Include each selected case exactly once.
- Aim for 30-60 words per row, excluding source references. Use one short reason and one concrete action or question; include every gap that changes the decision.
- Cite only the decisive sources. Quote a short ambiguous phrase when needed; do not repeat the full case, source code or investigation history.
- For `READY`, state what to implement and verify. For `BLOCKED`, name the missing capability and required change. For `NEEDS_CLARIFICATION`, ask the exact question and briefly explain why its answer matters.
- Do not add an introduction, per-case sections, implementation plans, repeated groupings, review notes or a conclusion unless requested.
- Before saving, verify coverage of the selected inputs, source support and preservation of the full requirements. Keep this check out of the report.
