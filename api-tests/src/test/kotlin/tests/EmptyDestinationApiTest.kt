package tests

import client.RidesApi
import io.qameta.allure.AllureId
import io.qameta.allure.Feature
import model.ErrorResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import rule.ApiTestCase
import testdata.ApiTestData

@Feature("API: Ride lifecycle")
class EmptyDestinationApiTest : ApiTestCase() {
    @Test
    @DisplayName("An empty destination is rejected without creating an active ride")
    @AllureId("2007")
    fun testEmptyDestinationIsRejectedWithoutActiveRide() {
        val token = obtainToken()

        step("Order a ride with an empty destination: the request is rejected") {
            val actual = RidesApi.create(token, ApiTestData.FROM, "", ApiTestData.YELLOW_TARIFF.id)
            assertThat(actual.statusCode).isEqualTo(400)
            assertThat(actual.error).isEqualTo(ErrorResponse("Both from and to are required"))
        }
        step("No active ride was created") {
            val actual = RidesApi.active(token)
            assertThat(actual.statusCode).isEqualTo(404)
            assertThat(actual.error).isEqualTo(ErrorResponse("No active ride"))
        }
    }
}
