package tests

import client.RidesApi
import io.qameta.allure.AllureId
import io.qameta.allure.Feature
import model.ActiveRide
import model.ErrorResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import rule.ApiTestCase
import testdata.ApiTestData

@Feature("API: Ride lifecycle")
class RideConflictTest : ApiTestCase() {
    @Test
    @DisplayName("A conflicting order preserves the active ride")
    @AllureId("2008")
    fun testConflictingOrderPreservesActiveRide() {
        val token = obtainToken()

        step("Reject a second order without replacing the active ride") {
            val createRideResponse =
                RidesApi.create(token, ApiTestData.FROM, ApiTestData.TO, ApiTestData.YELLOW_TARIFF.id)
            assertThat(createRideResponse.statusCode).isEqualTo(201)
            val firstRide = createRideResponse.body
            assertFirstRideCreated(firstRide)

            val conflictingOrderResponse =
                RidesApi.create(
                    token,
                    ApiTestData.SECOND_SEEDED_ORDER.from,
                    ApiTestData.SECOND_SEEDED_ORDER.to,
                    ApiTestData.TURQUOISE_TARIFF.id,
                )
            assertThat(conflictingOrderResponse.statusCode).isEqualTo(409)
            assertThat(conflictingOrderResponse.error)
                .isEqualTo(ErrorResponse("An active ride already exists", "ACTIVE_RIDE_EXISTS"))

            val active = RidesApi.active(token)
            assertThat(active.statusCode).isEqualTo(200)
            val activeRide = active.body
            assertActiveRideUnchanged(activeRide, firstRide)
        }
    }

    private fun assertFirstRideCreated(firstRide: ActiveRide) {
        assertThat(firstRide.id).isPositive()
        assertThat(firstRide.from).isEqualTo(ApiTestData.FROM)
        assertThat(firstRide.to).isEqualTo(ApiTestData.TO)
        assertThat(firstRide.option.id).isEqualTo(ApiTestData.YELLOW_TARIFF.id)
        assertThat(firstRide.status).isEqualTo(ApiTestData.DRIVER_FOUND_STATUS)
    }

    private fun assertActiveRideUnchanged(
        activeRide: ActiveRide,
        firstRide: ActiveRide,
    ) {
        assertThat(activeRide.id).isEqualTo(firstRide.id)
        assertThat(activeRide.from).isEqualTo(firstRide.from)
        assertThat(activeRide.to).isEqualTo(firstRide.to)
        assertThat(activeRide.status).isEqualTo(firstRide.status)
        assertThat(activeRide.option.id).isEqualTo(firstRide.option.id)
    }
}
