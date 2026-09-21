# Page object model

<!-- Agent drafting scope:
Read AGENTS.md first. Then inspect appium-tests/src/test/kotlin/pages/Element.kt,
at least three current page objects, and the action classes that use them.

Fill only this file. Keep its existing headings. Describe conventions only when
repository rules and current code support them. Compare how Element operations
locate and wait for elements, including exceptions. Quote real declarations,
and mark anything you cannot confirm as UNVERIFIED. Do not modify other files.
-->

This document starts as an AI draft. Verify every claim against the repository
before relying on it.

All paths below are relative to `appium-tests/src/test/kotlin/` unless they
start with a repository directory such as `app/` or `scripts/`.

## Role of pages

### What a page object owns

A page object is a Kotlin `object` singleton in `pages/` that holds one
`Element` value per Compose `testTag` on a screen, plus factory functions for
tags that embed an ID. It owns locators and nothing else.

- Policy: `AGENTS.md`, "Appium test architecture": `pages/`: singleton
  `Element` catalogs. Pages contain no assertions.
- Policy: `appium-tests/README.md`, "Architecture": `pages/ singleton Element
  catalogs, no assertions` and "Pages do not assert or own flows".
- Code: `rule/AppiumTestCase.kt` KDoc: "pages are catalogs of Element values,
  actions are singleton objects, and both resolve the current thread's driver
  through DriverFactory per operation".

Current catalogs: `PhoneLoginPage`, `OtpPage`, `PasskeyPage`, `GeoPage`,
`MapPage`, `OrderHistoryPage`, `NotificationsPage`, `SupportPage` and
`DriverSignupPage`. Two files in `pages/` are not catalogs: `Device.kt`
(device-level primitives, see below) and `VariantLocator.kt` (protected
infrastructure, see below).

### The shared element abstraction

`pages/Element.kt` is the only locator and wait primitive the pages layer
exposes. Its declaration:

```kotlin
class Element(
    val testTag: String,
    private val defaultTimeoutSec: Long = 10,
) {
    private val driver: AndroidDriver
        get() = requireNotNull(DriverFactory.current()) { "No active Appium session" }

    private fun by(): By = AppiumBy.id(testTag)
```

Conventions this encodes:

- **One locator strategy.** `by()` is private and always returns
  `AppiumBy.id(testTag)`. A page cannot choose XPath, accessibility ID or
  class-chain; it can only pick a tag string.
- **Tags are resource IDs because the app says so.** `Element.kt` KDoc: "the
  app sets testTagsAsResourceId = true on the root surface". Confirmed in
  `app/src/main/java/com/sandbox/qa/MainActivity.kt`
  (`.semantics { testTagsAsResourceId = true }`) and repeated on popups and
  dialogs that do not inherit it (`app/src/main/java/com/sandbox/qa/ui/
  TestTagWindow.kt`, `PhoneLoginScreen.kt`).
- **Tags are used unprefixed.** `rule/DriverFactory.kt` sets
  `appium:disableIdLocatorAutocompletion` to `true` with the comment "Compose
  testTags are plain resource-ids ("phone_title"); without this flag the driver
  rewrites them to "com.sandbox.qa:id/phone_title"". The one screen that is a
  classic Android View (`DriverSignupPage`) therefore adds the prefix itself;
  see the example section.
- **Stateless handles.** Every operation re-resolves the driver through
  `DriverFactory.current()` (a `ThreadLocal`) and re-finds the element, so
  `Element` values are safe as `val`s on singletons and never go stale
  (`Element.kt` KDoc).
- **Per-element default timeout.** `defaultTimeoutSec` defaults to 10 s. Pages
  raise it for elements that appear after a backend round trip, for example
  `Element("ride_driver_found_title", defaultTimeoutSec = 20)` in
  `pages/MapPage.kt`. Callers may still pass an explicit timeout per call.

### Supported operations and how each one waits

All of these come from `pages/Element.kt`.

| Operation | Locates through | Waits for | Timeout | Returns / on timeout |
| --- | --- | --- | --- | --- |
| `waitFor(timeoutSec)` | `locate()` | `ExpectedConditions.visibilityOfElementLocated` | argument or `defaultTimeoutSec` | `WebElement`; throws `TimeoutException` |
| `click(timeoutSec)` | `locate()` | visibility, then `.click()` | same | `Unit`; throws |
| `sendKeys(text, timeoutSec)` | `locate()` | visibility, then `.sendKeys` | same | `Unit`; throws |
| `clear(timeoutSec)` | `locate()` | visibility, then `.clear()` | same | `Unit`; throws |
| `text` (property) | `locate(defaultTimeoutSec)` | visibility, then `.text` | `defaultTimeoutSec` only | `String`; throws |
| `waitForGone(timeoutSec)` | `WebDriverWait` | `ExpectedConditions.invisibilityOfElementLocated` | argument or default | `Boolean` (`true`); throws on timeout |
| `retryClick(timeoutSec, message)` | `FluentWait(Unit)` polling every 300 ms | no visibility condition; repeats `driver.findElement(by()).click()` while ignoring exactly `NoSuchElementException`, `StaleElementReferenceException`, `ElementClickInterceptedException` | argument or default | `Unit`; throws with `message` |
| `isPresent()` | `driver.findElements(by())` | **no wait** | none | `Boolean`, immediately |

The shared path is `locate()`:

```kotlin
    private fun locate(timeoutSec: Long): WebElement =
        WebDriverWait(driver, Duration.ofSeconds(timeoutSec))
            .until(ExpectedConditions.visibilityOfElementLocated(by()))
```

Three operations deliberately step off that path:

- `waitForGone` inverts the condition (`invisibilityOfElementLocated`) and
  is the only positive way to assert disappearance with a wait. It returns a
  `Boolean`, which is why actions wrap it in `assertTrue`
  (`actions/RegionActions.kt`, `actions/DriverActions.kt`).
- `retryClick` does not wait for visibility at all. It calls `findElement`
  and `click` in a `FluentWait` loop and retries only three transient
  exception types, so "real failures (assertion errors, dead session,
  interrupts) surface immediately instead of being retried into a timeout"
  (`Element.kt` KDoc). The `message` parameter has no default and is
  required. `AGENTS.md` limits its use to "narrowly justified `retryClick`
  calls"; every current call site is a side-drawer item tapped right after
  the drawer opens (`actions/DrawerActions.kt`, `actions/DriverActions.kt`).
- `isPresent` is "the deliberate no-wait probe" (`Element.kt` KDoc) and is
  used for negative checks after the screen is known to be ready, never as a
  readiness signal by itself.

Note that the `text` property has no timeout parameter; it always uses the
element's `defaultTimeoutSec`. When an action needs a longer wait before
reading text it calls `waitFor(n)` first and reads `.text` from the returned
`WebElement`, for example `DriverSignupPage.errorText.waitFor().text` in
`actions/DriverActions.kt`.

### Prohibited locator strategies

- `AGENTS.md`, "Appium test architecture": "Use Compose `testTag` resource IDs
  through `AppiumBy.id`. Do not use text XPath locators. Do not use
  `Thread.sleep`".
- `appium-tests/README.md`, "Architecture": "`Thread.sleep` and text XPath
  locators are forbidden."
- `AGENTS.md`, "Safety boundaries": "do not edit `pages/VariantLocator.kt` or
  add fallback locator attempts." `scripts/protected-paths.txt` lists the same
  file with the rationale "editing it can hide a wrong installed build behind
  fallback locator attempts."

A grep of `pages/` and `actions/` finds no `xpath`, `Thread.sleep` or
`ExpectedConditions` usage outside `Element.kt`, so the current code matches
the rule.

`pages/VariantLocator.kt` declares one internal helper:

```kotlin
internal fun variantId(
    stable: String,
    redesign: String,
): String =
    when (val variant = System.getProperty("ui.variant", "stable")) {
```

`ui.variant` is passed from `scripts/run-suite.sh` and
`appium-tests/build.gradle.kts`. No current page object calls `variantId`;
the stable and redesign flavors currently share every tag. This is a fact
about the present tree, not a rule.

### Device-level primitives are not page elements

`pages/Device.kt` is an `object` for interactions that have no `testTag`:
`hideKeyboard()`, `pressBack()` and `pullDownToRefresh()`. Its KDoc gives the
placement rule: `pressBack` is "A device navigation event, not an in-app back
button - which is why it lives here and not on a page catalog." Like
`Element`, it resolves `DriverFactory.current()` on every call and holds no
state.

## No asserts rule

### The rule and its sources

Pages hold locators only. Assertions, interactions and multi-step flows live
in `actions/`; tests compose actions as named steps.

- `AGENTS.md`, "Appium test architecture": layer 2 `pages/` "Pages contain no
  assertions"; layer 3 `actions/` is "interactions, flows, waits and
  assertions"; layer 4 `tests/` is "JUnit scenarios written as named Allure
  steps".
- `appium-tests/README.md`, "Architecture": "Pages do not assert or own
  flows."
- `rule/AppiumTestCase.kt` KDoc: "Layering: pages (`pages/`) = element
  catalogs, NO asserts; actions = steps + checks (asserts live here);
  testcases = the action sequence."

### Representative code

Every file in `pages/` imports nothing from JUnit. The only JUnit assertion
imports (`org.junit.jupiter.api.Assertions`) in the suite are in the eight
files of `actions/`; `rule/` and `tests/` import only JUnit lifecycle,
extension and `@Test` annotations. Example, `actions/OrderActions.kt`:

```kotlin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import pages.MapPage
import pages.OrderHistoryPage
```

Where a page needs to say something about how an element should be checked,
it says it in a comment and leaves the check to the caller
(`pages/MapPage.kt`):

```kotlin
    // The tag is on a container, so tests assert presence rather than text.
    val regionBanner = Element("region_banner")
```

The check itself is in `actions/RegionActions.kt`:

```kotlin
        assertTrue(
            MapPage.regionBanner.waitFor().isDisplayed,
            "Region banner should appear when region_unavailable is enabled",
        )
```

Pages also do not own flows. `PhoneLoginPage` exposes `phoneInput` and
`continueButton` but no `login()`; the sequence is `OnboardingActions.reachOtp`
(`actions/OnboardingActions.kt`), which types, hides the keyboard, taps and
waits for the next screen's title.

### Practical benefit

The split is what lets a test read as a scenario. `tests/RideAndHistoryE2ETest.kt`
KDoc: "the testcase reads like a scenario, with all the "how" in actions/ and
pages/, and each step in the report."

One concrete consequence visible in the code: because an `Element` carries no
expectation, the same element serves opposite checks. `MapPage.ridesList` is
awaited positively in `MapActions.searchDestination`
(`MapPage.ridesList.waitFor()`) and checked negatively in
`MapActions.assertTariffsNotLoaded`
(`assertFalse(MapPage.ridesList.isPresent(), ...)`). If the page had baked a
"must be visible" assertion into the element, the negative check would need a
second declaration.

No tool enforces the no-asserts rule itself: `appium-tests/build.gradle.kts`
has one boundary task, `checkPageObjectBoundary`, but it rejects only
`Element(` construction outside `pages/`. Nothing fails on JUnit imports under
`pages/` or page imports under `tests/`; those two rules are upheld by review.

## Example from this repo

`pages/MapPage.kt` is the largest catalog and shows every convention.

### Static element declarations

```kotlin
/** Element catalog for the map screen (plus the side-drawer entry it hosts). */
object MapPage {
    val menuButton = Element("map_menu_button")
    val pickupField = Element("map_from_field")
    val destinationField = Element("map_to_field")
    val pullToRefresh = Element("rides_pull_to_refresh")
```

- The property name describes the role (`pickupField`); the tag string is the
  app's `testTag` verbatim (`map_from_field`). Names and tags need not match.
- The screen boundary follows the composable, not the visual widget. The side
  drawer is hosted by the map screen, so `drawerOrdersItem`,
  `drawerNotificationsItem`, `drawerNotificationsBadge`, `drawerHelpItem` and
  `drawerBecomeDriverButton` live on `MapPage`, not in a separate drawer page.
- Elements that arrive after a backend transition raise their default
  timeout: `driverFoundTitle`, `completedTitle` and `statusMessage` all use
  `defaultTimeoutSec = 20`. Elements that render synchronously keep the 10 s
  default.
- A tag the app is expected not to render is still catalogued so that its
  absence can be asserted: `val removedSearchButton = Element("map_search_button")`
  is used by `MapActions.assertFindOffersRemoved` through `isPresent()`.

### Dynamic locator factories

```kotlin
    fun ridePrice(rideId: Int) = Element("ride_price_$rideId", defaultTimeoutSec = 15)

    fun rideName(rideId: Int) = Element("ride_name_$rideId", defaultTimeoutSec = 15)

    fun rideOption(rideId: Int) = Element("ride_option_$rideId", defaultTimeoutSec = 15)

    fun rideSelected(rideId: Int) = Element("ride_selected_$rideId", defaultTimeoutSec = 15)
```

When a `testTag` embeds an entity ID, the page exposes a function that builds
a fresh `Element` from the ID. Creating an `Element` is cheap because it holds
only a string and a timeout, so there is no caching. The same pattern appears
in `pages/OrderHistoryPage.kt` (`orderPrice(orderId: Int)`,
`orderRoute(orderId: Int)`) and `pages/NotificationsPage.kt`, where IDs are
`Long` (`item(id: Long)`, `title(id: Long)`, `message(id: Long)`,
`newLabel(id: Long)`).

### The View-screen exception

`pages/DriverSignupPage.kt` covers a classic Android View screen, whose
resource IDs carry the package prefix that Compose tags do not:

```kotlin
/** Element catalog for the classic View screen; its resource ids need the package prefix. */
object DriverSignupPage {
    private fun viewId(id: String) = Element("com.sandbox.qa:id/$id")

    val backButton = viewId("driver_back_button")
```

The prefix is added inside the page through a private helper, so the
`AppiumBy.id` strategy in `Element` stays unchanged.

### How callers determine screen readiness

Pages expose no `isLoaded()` or `waitUntilReady()`. Readiness is expressed by
actions waiting on an anchor element of the destination screen:

- `actions/MapActions.kt`:

  ```kotlin
      /** Waits for the destination form and pull-to-refresh surface. */
      fun awaitReady() {
          MapPage.pickupField.waitFor(20)
          MapPage.destinationField.waitFor(20)
          MapPage.pullToRefresh.waitFor()
      }
  ```

- `actions/OnboardingActions.kt` ends each transition with the next screen's
  anchor, for example `OtpPage.title.waitFor()` in `reachOtp` and
  `MapPage.destinationField.waitFor(20)` in `enableLocationToMap`.
- `actions/OrderActions.kt` starts every history assertion with
  `OrderHistoryPage.title.waitFor(15)`.

## How actions use pages

`actions/OrderActions.kt` is a compact example of an action object that both
waits and asserts through a page catalog. Full source:

```kotlin
object OrderActions {
    /** Asserts each expected order price on the history screen ("29.70 €" - no tilde). */
    fun assertHistoryPrices(expected: Map<Int, String>) {
        OrderHistoryPage.title.waitFor(15)
        expected.forEach { (id, price) ->
            assertEquals(price, OrderHistoryPage.orderPrice(id).text, "price of order $id")
        }
    }

    fun assertHistoryRoutes(expected: Map<Int, String>) {
        OrderHistoryPage.title.waitFor(15)
        expected.forEach { (id, route) ->
            assertEquals(route, OrderHistoryPage.orderRoute(id).text, "route of order $id")
        }
    }

    /** Asserts the inline history load error (shown instead of the list; no retry control here). */
    fun assertHistoryError(expected: String) {
        OrderHistoryPage.title.waitFor(15)
        assertEquals(expected, OrderHistoryPage.errorLabel.text, "order history error")
    }

    fun assertOrderPrice(
        orderId: Int,
        expected: String,
    ) {
        OrderHistoryPage.title.waitFor(15)
        assertEquals(expected, OrderHistoryPage.orderPrice(orderId).text, "price of order $orderId")
    }

    fun assertOrderAbsent(orderId: Int) {
        OrderHistoryPage.title.waitFor(15)
        assertFalse(OrderHistoryPage.orderPrice(orderId).isPresent(), "order $orderId should be absent")
    }

    fun returnToRideForm() {
        OrderHistoryPage.backButton.click()
        MapPage.destinationField.waitFor()
        MapPage.pullToRefresh.waitFor()
    }
}
```

What the file shows:

- **Actions are stateless `object`s**, the same model as pages
  (`actions/DriverActions.kt` KDoc: "Stateless singleton, same model as the
  page catalogs: no constructor, no test-runtime dependency").
- **Where it waits.** Each assertion method opens with
  `OrderHistoryPage.title.waitFor(15)` as the readiness anchor. Reading
  `.text` afterwards is itself a visibility wait of the element's own
  default (15 s for `orderPrice`), so the assertion is synchronized without
  any sleep.
- **Where it asserts.** JUnit `assertEquals` and `assertFalse` with a
  message, always in the action, never in the page. The message names the
  entity (`"price of order $id"`), which is what the failure digest and
  Allure report show.
- **Negative checks come after a readiness wait.** `assertOrderAbsent` calls
  `isPresent()`, which does not wait, only after `title.waitFor(15)` proves
  the screen is rendered. Without the anchor the probe could pass on a blank
  screen.
- **Navigation actions wait, they do not assert.** `returnToRideForm` clicks
  the back button and waits on two map anchors. Whether the map is correct
  is left to a following `MapActions.assert...` step.

Two more patterns from neighbouring action files:

- `retryClick` with a mandatory message, for the drawer transition
  (`actions/DrawerActions.kt`):

  ```kotlin
      fun openOrders() {
          MapPage.menuButton.click()
          MapPage.drawerOrdersItem.retryClick(message = "could not open Order history from the drawer")
      }
  ```

- `waitForGone` wrapped in an assertion (`actions/RegionActions.kt`):

  ```kotlin
          assertTrue(
              MapPage.regionBanner.waitForGone(),
              "Region banner should disappear after disabling the state",
          )
  ```

Tests reach actions through the vocabulary fields on `rule/AppiumTestCase.kt`
(`val map = MapActions`, `val drawer = DrawerActions`, `val orders =
OrderActions`, and so on) and never touch a page directly, for example in
`tests/RideAndHistoryE2ETest.kt`:

```kotlin
        step("Open order history") {
            drawer.openOrders()
        }
        step("Assert order prices") {
            orders.assertHistoryPrices(TestData.PAST_ORDERS)
        }
```

A grep confirms no file in `tests/` imports from `pages`.
