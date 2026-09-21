package tests

import client.RidesApi
import io.qameta.allure.AllureId
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import rule.ApiTestCase
import testdata.ApiTestData

@Feature("API: Rides")
class RideOptionsApiTest : ApiTestCase() {
    @Test
    @DisplayName("Ride options list three tariffs with prices")
    @AllureId("2004")
    fun testRideOptionsListSeededTariffs() {
        val token = obtainToken()

        step("Ride options for the seeded route return all three tariffs with their prices") {
            val actual = RidesApi.options(token, ApiTestData.FROM, ApiTestData.TO)
            assertThat(actual.statusCode).isEqualTo(200)
            val actualTariffs = actual.body.options.map { it.name to it.priceCents }
            val expectedTariffs = ApiTestData.TARIFFS.map { it.name to it.priceCents }
            assertThat(actualTariffs).containsExactlyElementsOf(expectedTariffs)
        }
    }
}
