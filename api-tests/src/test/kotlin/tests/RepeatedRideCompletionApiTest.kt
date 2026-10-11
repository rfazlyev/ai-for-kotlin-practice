package tests

import client.OrdersApi
import client.RidesApi
import io.qameta.allure.AllureId
import io.qameta.allure.Feature
import model.ErrorResponse
import model.Order
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import rule.ApiTestCase
import testdata.ApiTestData

@Feature("API: Ride lifecycle")
class RepeatedRideCompletionApiTest : ApiTestCase() {
    @Test
    @DisplayName("Completing a ride twice does not duplicate order history")
    @AllureId("2010")
    fun testRepeatedCompletionDoesNotDuplicateHistory() {
        val token = obtainToken()
        lateinit var initialOrders: List<Order>
        var rideId = 0
        lateinit var completedOrder: Order

        step("Read order history: it contains exactly the three seeded orders") {
            val actual = OrdersApi.orders(token)
            assertThat(actual.statusCode).isEqualTo(200)
            assertThat(actual.body.orders).containsExactlyElementsOf(ApiTestData.SEEDED_ORDERS)
            initialOrders = actual.body.orders
        }
        step("Order the Yellow tariff: a driver is found") {
            val actual = RidesApi.create(token, ApiTestData.FROM, ApiTestData.TO, ApiTestData.YELLOW_TARIFF.id)
            assertThat(actual.statusCode).isEqualTo(201)
            assertThat(actual.body.id).isPositive()
            assertThat(actual.body.status).isEqualTo(ApiTestData.DRIVER_FOUND_STATUS)
            assertThat(actual.body.from).isEqualTo(ApiTestData.FROM)
            assertThat(actual.body.to).isEqualTo(ApiTestData.TO)
            assertThat(actual.body.option.id).isEqualTo(ApiTestData.YELLOW_TARIFF.id)
            rideId = actual.body.id
        }
        step("Complete the ride: a new order is created for the submitted route") {
            val actual = RidesApi.complete(token, rideId)
            assertThat(actual.statusCode).isEqualTo(200)
            assertThat(actual.body.ride.id).isEqualTo(rideId)
            assertThat(actual.body.ride.status).isEqualTo(ApiTestData.COMPLETED_STATUS)
            assertThat(actual.body.order.id).isPositive()
            assertThat(initialOrders.map { it.id }).doesNotContain(actual.body.order.id)
            assertThat(actual.body.order.from).isEqualTo(ApiTestData.FROM)
            assertThat(actual.body.order.to).isEqualTo(ApiTestData.TO)
            completedOrder = actual.body.order
        }
        step("Complete the same ride again: the request is rejected") {
            val actual = RidesApi.complete(token, rideId)
            assertThat(actual.statusCode).isEqualTo(409)
            assertThat(actual.error).isEqualTo(ErrorResponse(ApiTestData.RIDE_COMPLETION_CONFLICT_ERROR))
        }
        step("Order history contains the completed order once, followed by the original history") {
            val actual = OrdersApi.orders(token)
            assertThat(actual.statusCode).isEqualTo(200)
            assertThat(actual.body.orders).containsExactlyElementsOf(listOf(completedOrder) + initialOrders)
        }
    }
}
