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
class UnknownRideOptionApiTest : ApiTestCase() {
    @Test
    @DisplayName("An unknown ride option is rejected")
    @AllureId("2009")
    fun testUnknownRideOptionIsRejected() {
        val token = obtainToken()

        step("Order a ride with an unknown ride option: the request is rejected") {
            val actual = RidesApi.create(token, ApiTestData.FROM, ApiTestData.TO, UNKNOWN_RIDE_OPTION_ID)
            assertThat(actual.statusCode).isEqualTo(400)
            assertThat(actual.error).isEqualTo(ErrorResponse("Unknown ride option 999"))
        }
    }

    private companion object {
        const val UNKNOWN_RIDE_OPTION_ID = 999
    }
}
