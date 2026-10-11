package testdata

import model.Driver
import model.Order
import model.RideOption
import model.SandboxSnapshot

object ApiTestData {
    const val PHONE = "+381 64 123 45 67"
    const val SHORT_PHONE = "1234567" // 7 digits, one below the minimum
    const val EIGHT_DIGIT_PHONE = "12345678" // exactly the minimum
    const val SHORT_PHONE_ERROR = "Phone number must contain at least 8 digits"
    const val MALFORMED_BODY = "not-json" // not JSON at all: probes the parser, not the validation
    const val MALFORMED_BODY_ERROR = "Malformed request body"
    const val VALID_OTP = "1234"
    const val WRONG_OTP = "9999"
    const val WRONG_OTP_ERROR = "Invalid code" // same copy as the mobile UI
    const val OTP_SENT_STATUS = "otp_sent"
    const val FORGED_TOKEN = "sandbox-forged" // plausible-looking, but never issued by the server
    const val TOKEN_ERROR = "Missing or invalid token" // shared by missing and forged tokens
    const val INTERNAL_SERVER_ERROR = "Internal Server Error"
    const val DRIVER_NOT_FOUND_ERROR = "No cars found for this route"
    const val NO_ACTIVE_RIDE_ERROR = "No active ride"
    const val RIDE_COMPLETION_CONFLICT_ERROR = "Ride is missing or cannot be completed"
    const val DRIVER_FOUND_STATUS = "driver_found"
    const val COMPLETED_STATUS = "completed"
    const val CANCELLED_STATUS = "cancelled"
    const val RESET_STATUS = "reset"

    const val FROM = "Oak Avenue"
    const val TO = "Market Street"

    val YELLOW_TARIFF =
        RideOption(
            id = 1,
            name = "Yellow",
            seats = 4,
            priceCents = 2970,
            category = "Official taxi",
            available = true,
        )
    val TURQUOISE_TARIFF =
        RideOption(
            id = 2,
            name = "Turquoise",
            seats = 4,
            priceCents = 3454,
            category = "Official taxi",
            available = true,
        )
    val MINIVAN_TARIFF =
        RideOption(
            id = 3,
            name = "Minivan",
            seats = 8,
            priceCents = 3905,
            category = "Official taxi",
            available = true,
        )
    val TARIFFS = listOf(YELLOW_TARIFF, TURQUOISE_TARIFF, MINIVAN_TARIFF)
    val TARIFFS_WITH_MINIVAN_UNAVAILABLE =
        listOf(YELLOW_TARIFF, TURQUOISE_TARIFF, MINIVAN_TARIFF.copy(available = false))

    val DRIVER =
        Driver(
            name = "Alex Morgan",
            car = "Toyota Corolla",
            plate = "QA 1234",
            etaMinutes = 4,
        )

    val LATEST_SEEDED_ORDER =
        Order(
            id = 1,
            date = "June 12, 22:55",
            from = "Oak Avenue",
            to = "Market Street",
            priceCents = 2970,
        )
    val SECOND_SEEDED_ORDER =
        Order(
            id = 2,
            date = "June 10, 09:14",
            from = "City Center",
            to = "Airport Terminal",
            priceCents = 1450,
        )
    val OLDEST_SEEDED_ORDER =
        Order(
            id = 3,
            date = "June 2, 18:30",
            from = "Pine Street",
            to = "River Road",
            priceCents = 980,
        )
    val SEEDED_ORDERS = listOf(LATEST_SEEDED_ORDER, SECOND_SEEDED_ORDER, OLDEST_SEEDED_ORDER)

    // Sandbox state ids. The API is tested as a black box, so the ids are
    // spelled here rather than imported from the fake-api module.
    const val SLOW_BACKEND_RESPONSE = "slow_backend_response"
    const val BACKEND_ERROR = "backend_error"
    const val CAR_UNAVAILABLE = "car_unavailable"
    const val DRIVER_NOT_FOUND = "driver_not_found"
    const val REGION_UNAVAILABLE = "region_unavailable"

    val ALL_STATES_DISABLED =
        SandboxSnapshot(
            slowBackendResponse = false,
            backendError = false,
            carUnavailable = false,
            driverNotFound = false,
            intermittentBackendDelay = false,
            regionUnavailable = false,
        )
    val CAR_UNAVAILABLE_ENABLED = ALL_STATES_DISABLED.copy(carUnavailable = true)
}
