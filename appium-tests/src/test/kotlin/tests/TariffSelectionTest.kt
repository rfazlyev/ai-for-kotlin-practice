package tests

import io.qameta.allure.AllureId
import io.qameta.allure.Feature
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import rule.AppiumTestCase
import testdata.TestData

@Feature("Ride form")
class TariffSelectionTest : AppiumTestCase() {
    @Test
    @DisplayName("Selecting another tariff moves the selection")
    @AllureId("1009")
    fun testSelectingAnotherTariffMovesSelection() {
        val yellowId = 1
        val turquoiseId = 2

        step("Enter the destination: the tariff list is displayed") {
            map.searchDestination(TestData.DESTINATION)
        }
        step("Select Yellow: only Yellow has the selection checkmark") {
            map.selectTariff(yellowId)
            map.assertOnlyTariffSelected(yellowId, TestData.RIDE_NAMES.keys)
        }
        step("Select Turquoise: the checkmark moves and only Turquoise is selected") {
            map.selectTariff(turquoiseId)
            map.assertOnlyTariffSelected(turquoiseId, TestData.RIDE_NAMES.keys)
        }
    }
}
