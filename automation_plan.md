# Automation plan: API-2004

## Status

- Test Case: `API-2004` — `Ride options return all seeded tariffs`
- Artifact status: `complete`
- Automated validation: recorded outside this file by the lesson checker.
- Generation gate: use this plan only after automated validation passes.

## Test contract

| Item | Decision |
|---|---|
| Objective | `GET /rides/options` for a valid route answers HTTP 200 with exactly the three seeded tariffs, compared by name and price in cents. |
| Preconditions | Isolated `X-Sandbox-Session` started and reset before the test (`rule/ApiTestCase.kt:14-27`, all sandbox states disabled). Bearer token from the existing `obtainToken()` (`ApiTestCase.kt:43-47`, OTP login through `client/AuthApi.kt`). |
| Request | `GET /rides/options?from=Oak Avenue&to=Market Street` with the `Authorization: Bearer` header carrying the obtained token; values `ApiTestData.FROM` and `ApiTestData.TO` (`testdata/ApiTestData.kt`). |
| Expected result | Status 200; `options` mapped to (name, priceCents) equals `ApiTestData.TARIFFS` mapped the same way: Yellow 2970, Turquoise 3454, Minivan 3905, in that order. |
| Contract source | `fake-api/openapi.yaml` path `/rides/options`, operation `get`, response `200` → `RideOptionsResponse.options[]` → `RideOption` (required `name`, `price` integer euro cents). |
| Coverage check | `grep -rn -E '@DisplayName\|@AllureId' api-tests/src/test/kotlin/tests`: IDs 2000-2003 only. `grep -rn 'RidesApi.options' api-tests/src/test/kotlin/tests`: no hits; 2001-2003 use `RidesApi.create` only. |

## Implementation map

| Request or response concern | Reuse or planned addition | Repository and contract evidence |
|---|---|---|
| Session isolation and reset | `rule/ApiTestCase.kt` `resetSandboxStates`, `releaseSandboxSession`; `client/ApiSpec.kt` `ApiSession` sets header `X-Sandbox-Session` | OpenAPI parameter `SandboxSession`; `SandboxApi.reset` returns 200 |
| Authorization | `ApiTestCase.obtainToken` → `client/AuthApi.verifyOtp` with `ApiTestData.PHONE`, `ApiTestData.VALID_OTP` | OpenAPI `security: bearerAuth` on the operation |
| Request | `client/RidesApi.kt` `options(token, from, to)` adds the bearer header and `from`, `to` query params, then `get("/rides/options")` (`RidesApi.kt:11-23`) | OpenAPI query parameters `from`, `to`, both required |
| Response decoding | `client/ApiResponse.kt` `statusCode`, `body`; `model/RideOptionModels.kt` `RideOptionsResponse.options` (list of `RideOption`), `RideOption.priceCents` bound to wire `price` (`RideOptionModels.kt:16-17`) | OpenAPI schemas `RideOptionsResponse`, `RideOption` |
| Expected tariffs | `testdata/ApiTestData.kt` `TARIFFS` = `YELLOW_TARIFF`, `TURQUOISE_TARIFF`, `MINIVAN_TARIFF` with `priceCents` 2970, 3454, 3905 | OpenAPI operation description: "Yellow 2970, Turquoise 3454, Minivan 3905" |
| Nearest test to mirror | `tests/RideLifecycleApiTest.kt`: `obtainToken`, `step`, `assertThat(actual.statusCode)`, `containsExactlyElementsOf` | Same base class and Allure conventions (`api-tests/README.md`) |
| Test class | Planned: `tests/RideOptionsApiTest.kt`, one `@Test`, `@Feature("API: Rides")`, `@AllureId("2004")`, `@DisplayName("Ride options list three tariffs with prices")` | `grep -rln RideOptionsApiTest api-tests/src`: no hits |

## Response assertions

| Expected result | Assertion | Expected-value source |
|---|---|---|
| HTTP 200 | `assertThat(actual.statusCode).isEqualTo(200)` | OpenAPI response `200` of `GET /rides/options` |
| Exactly the three seeded tariffs | `actualTariffs = actual.body.options.map { it.name to it.priceCents }`; `expectedTariffs = ApiTestData.TARIFFS.map { it.name to it.priceCents }`; `assertThat(actualTariffs).containsExactlyElementsOf(expectedTariffs)` | `ApiTestData.TARIFFS`, independent of the server; values match the OpenAPI description |

- Excluded checks: `id`, `seats`, `category`, `available` fields (full-object equality is wider than the Test Case); response time; 400, 401 and 500 branches; `car_unavailable` and latency states.

## Allowed files

| Path | Change and reason |
|---|---|
| `api-tests/src/test/kotlin/tests/RideOptionsApiTest.kt` | New class `RideOptionsApiTest : ApiTestCase()` with the single API-2004 test; client, models and test data already exist |

Any additional file requires a revised plan and another automated validation pass.

## Risks and stop conditions

- Risk: contract drift between `openapi.yaml`, `ApiTestData.TARIFFS` and the live response (order, names or prices). Mitigation: keep the ordered `containsExactlyElementsOf`; report a mismatch instead of switching to any-order or size checks.
- Risk: `fake-api` down or returning 401/500 makes `ApiTestCase` throw before the request. Mitigation: start `./gradlew :fake-api:run` first; report `BLOCKED`, do not retry.
- Stop when: `@AllureId("2004")` is occupied; `RidesApi.options` or `RideOption.priceCents` no longer match this plan; the live 200 body disagrees with the OpenAPI schema or description; the task needs a second test-layer file or a backend change.
- Do not compensate with backend edits, weaker assertions, retries or unrelated test changes.

## Verification

- Metadata inventory: `grep -rn -E '@DisplayName|@AllureId' api-tests/src/test/kotlin/tests`
- Format check: `./gradlew ktlintCheck`
- Test run: `./gradlew :fake-api:run` in one terminal, then `./gradlew :api-tests:test --rerun` (macOS; `gradlew.bat` on Windows)
- Required result: `Ride options list three tariffs with prices` listed `PASSED`; `TEST-tests.RideOptionsApiTest.xml` has `tests="1"`, `failures="0"`, `errors="0"`; summed `tests=` across the module is 5 (baseline 4 + 1)
- Evidence: `api-tests/build/test-results/test/TEST-tests.RideOptionsApiTest.xml`; request and response attachments in `api-tests/build/allure-results`; `api-tests/build/reports/tests/test/index.html`
