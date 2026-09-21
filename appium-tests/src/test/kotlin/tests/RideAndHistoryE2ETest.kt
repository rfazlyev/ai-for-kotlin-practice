package tests

import io.qameta.allure.AllureId
import io.qameta.allure.Feature
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import rule.AppiumTestCase
import testdata.TestData

/**
 * E2E written purely as a sequence of action-backed steps - the testcase reads like
 * a scenario, with all the "how" in actions/ and pages/, and each step in the report.
 */
@Feature("E2E")
class RideAndHistoryE2ETest : AppiumTestCase() {
    @Test
    @DisplayName("Search a ride then check order history")
    @AllureId("1003")
    fun testSearchRideThenCheckHistory() {
        step("Start on the map (authorized)") {
            map.awaitReady()
        }
        step("Search a destination") {
            map.searchDestination(TestData.DESTINATION)
        }
        step("Assert the Yellow tariff price") {
            map.assertRidePrice(1, TestData.YELLOW_PRICE_ON_MAP)
        }
        step("Open order history") {
            drawer.openOrders()
        }
        step("Assert order prices") {
            orders.assertHistoryPrices(TestData.PAST_ORDERS)
        }
    }

    @Test
    @DisplayName("Completed ride appears in order history")
    @AllureId("1006")
    fun testCompletedRideAppearsInHistory() {
        step("Start on the ride form with the displayed pickup (authorized)") {
            map.awaitReady()
            map.assertPickup(TestData.PICKUP)
        }
        step("Enter the destination") {
            map.searchDestination(TestData.DESTINATION)
        }
        step("Select the Yellow tariff, tap Order and wait for a driver") {
            map.selectAndOrderRide(1)
        }
        step("Complete the ride") {
            map.completeRide(TestData.YELLOW_PRICE_IN_HISTORY)
        }
        step("Return to the ride form") {
            map.returnHomeAfterCompletion()
        }
        step("Open order history") {
            drawer.openOrders()
        }
        step("The completed ride is listed with its route and the Yellow price") {
            val expectedRoute = "${TestData.PICKUP} → ${TestData.DESTINATION}"
            orders.assertHistoryRoutes(mapOf(TestData.FIRST_COMPLETED_ORDER_ID to expectedRoute))
            orders.assertOrderPrice(TestData.FIRST_COMPLETED_ORDER_ID, TestData.YELLOW_PRICE_IN_HISTORY)
        }
    }
}
