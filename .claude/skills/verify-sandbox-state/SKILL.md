---
name: verify-sandbox-state
description: >-
  Covers PowerShell on Windows and bash on macOS and Linux.
  Use when asked to enable or check a state such as slow_backend_response,
  backend_error, car_unavailable, driver_not_found,
  intermittent_backend_delay or region_unavailable, reproduce a breakage, or
  confirm that a state really took effect.
---

# verify-sandbox-state

The sandbox backend can be switched into predefined product states. Each
state represents a realistic ride-hailing failure or edge case: an
unsupported region, a tariff missing from the catalog, a backend outage,
degraded latency. The source of truth is `ConditionConfig.ALL` in the code: 6 states.
The control surface uses the `Condition*`
vocabulary: `ConditionConfig`, `ConditionReceiver`, the `--es condition`
extra and the `condition_switch_*` testTags. Those are plumbing names; the
states themselves are product states.

> `adb ... am broadcast` always returns `result=0`, even
> when the state did NOT apply. "The command ran" is not the same as "the
> state took effect". Always confirm the **user-visible behavior** in the
> app.

## Order of operations, it matters

1. **Start the session/app first**, then enable the state.
   Session creation relaunches the app process, which wipes the in-memory
   `ConditionConfig`: a state enabled before the session start gets erased. The
   APK itself is installed only on the first session of a run; the
   per-session process restart is what resets states.
2. Enable the state with a targeted broadcast: **by component `-n`, not by
   action `-a`**.
3. Verify the visible effect.
4. When done: `reset`.

On Windows, call `adb` by its full path
`$env:ANDROID_HOME\platform-tools\adb.exe`: it is not on PATH in that
environment. On macOS/Linux `adb` is normally on PATH; if not, use
`$ANDROID_HOME/platform-tools/adb`.

PowerShell on Windows:

```powershell
$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"

# Enable a state
& $adb shell am broadcast -n com.sandbox.qa/.condition.ConditionReceiver `
    --es condition slow_backend_response --ez enabled true

# Disable a specific state
& $adb shell am broadcast -n com.sandbox.qa/.condition.ConditionReceiver `
    --es condition slow_backend_response --ez enabled false

# Reset all states
& $adb shell am broadcast -n com.sandbox.qa/.condition.ConditionReceiver --ez reset true
```

bash on macOS / Linux:

```bash
# Enable a state
adb shell am broadcast -n com.sandbox.qa/.condition.ConditionReceiver \
    --es condition slow_backend_response --ez enabled true

# Disable a specific state
adb shell am broadcast -n com.sandbox.qa/.condition.ConditionReceiver \
    --es condition slow_backend_response --ez enabled false

# Reset all states
adb shell am broadcast -n com.sandbox.qa/.condition.ConditionReceiver --ez reset true
```

An alternative to the broadcast is the **Settings** screen in the app: the
"Sandbox states" screen has toggles for the same 6 states.

## The 6 sandbox states: product meaning and how to verify

Exact timings and values live in AGENTS.md and the sources, see the last
section.

| state | Product meaning | What you verify in the UI |
|---|---|---|
| `slow_backend_response` | the backend responds slowly on every call, a deterministic latency spike | loading the map/ride options visibly hangs on every network call |
| `backend_error` | the backend fails: every data call returns HTTP 500, an `ApiException` | error state instead of data: `rides_error` on the map with a Retry button, `orders_error` in history, `geo_error` in onboarding |
| `car_unavailable` | the Minivan tariff is unavailable for this region or route and remains visible but grayed out | the Minivan row `ride_option_3` is present but disabled (`enabled=false`); Yellow and Turquoise remain selectable |
| `driver_not_found` | tariff search succeeds but the order cannot match a driver | ordering ends with `No cars found for this route`, no active ride is created, and the tariff list is shown again |
| `intermittent_backend_delay` | intermittent backend degradation: roughly half of the calls get a large latency spike | observe latency for individual data requests or UI transitions; a fast response does not rule out the state, and the proportion of failing suite runs is not fixed |
| `region_unavailable` | the current city is not supported by the service | `region_banner` appears with the text "Service is not available in this region yet" |

Ways to confirm the effect, bash shown; on Windows use `& $adb ...` instead
of `adb ...`:

- **Visually:** emulator screenshot: `adb exec-out screencap -p > shot.png`.
- **By timing, for slow_backend_response and intermittent_backend_delay:**
  measure how long the transition took: normal is about 600ms versus
  8000/18000ms.
- **By element, for car_unavailable and region_unavailable:** check for
  the presence of `ride_option_3` / `region_banner` via a hierarchy dump:
  `adb exec-out uiautomator dump /dev/tty`, or with the Appium locator
  `AppiumBy.id("region_banner")`.
- **Broadcast receipt logs confirm delivery only, NOT application:**
  `adb logcat -d -s ConditionReceiver` shows lines like
  `Condition 'slow_backend_response' set to true`. The log confirms delivery,
  not that the state took effect: verify the visible effect.

## Source of truth for the values: no duplication, no drift

Exact timings, tariff prices, `MINIVAN_RIDE_ID`, and the currency-string
format live in **`AGENTS.md`**, the sandbox states and load-bearing values
sections, and in `ConditionConfig.kt`, `HttpRideRepository.kt` and `fake-api`.
This
skill deliberately does NOT repeat them, so they cannot drift apart. When
verifying an effect, compare what you observe against the source, not against
memory. Locators: testTag as resource-id via `AppiumBy.id(...)`, no
xpath-by-text.

For running the tests together with sandbox states, see the
`run-appium-suite` skill; it resets all states before a clean suite run.
