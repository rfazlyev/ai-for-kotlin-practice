package testdata

/** Central test data so testcases read declaratively and values live in one place. */
object TestData {
    const val PHONE = "999999999"
    const val VALID_OTP = "1234"
    const val WRONG_OTP = "9999"
    const val WRONG_OTP_ERROR = "Invalid code"

    const val PICKUP = "Oak Avenue"
    const val ALTERNATE_PICKUP = "Pine Street"
    const val DESTINATION = "Market Street"
    const val YELLOW_PRICE_ON_MAP = "~29.70 €" // map list: tilde
    const val YELLOW_PRICE_IN_HISTORY = "29.70 €"
    const val DRIVER_FOUND_NOTIFICATION = "Alex Morgan arrives in 4 min · Toyota Corolla"
    const val RIDE_COMPLETED_NOTIFICATION = "Oak Avenue → Market Street · 29.70 €"
    const val RIDE_CANCELLED_NOTIFICATION = "Oak Avenue → Market Street"

    // Seeded backend order history (no tilde). Keys are order ids.
    val PAST_ORDERS =
        mapOf(
            1 to "29.70 €",
            2 to "14.50 €",
            3 to "9.80 €",
        )
    val PAST_ORDER_ROUTES =
        mapOf(
            1 to "Oak Avenue → Market Street",
            2 to "City Center → Airport Terminal",
            3 to "Pine Street → River Road",
        )

    // Seeded orders use ids 1-3; the first ride completed after a sandbox reset becomes order 4.
    const val FIRST_COMPLETED_ORDER_ID = 4

    // Driver signup (the classic View screen; strings from res/values/strings.xml).
    const val DRIVER_NAME = "Test Driver"
    const val DRIVER_CAR = "BG 123 AB"
    const val DRIVER_SIGNUP_EMPTY_ERROR = "Fill in all fields"
    const val DRIVER_SIGNUP_SUCCESS = "Application submitted"

    // Phone validation: >= 8 digits is valid, and the field itself filters non-digits.
    const val SHORT_PHONE = "1234567" // 7 digits - one short of valid
    const val EIGHT_DIGIT_PHONE = "12345678" // exactly the 8-digit boundary
    const val PHONE_VALIDATION_ERROR = "Enter a valid phone number"
    const val MIXED_PHONE_INPUT = "abc123-45.678" // what gets typed...
    const val MIXED_PHONE_DIGITS = "12345678" // ...and what the field keeps

    // The OTP field caps at 4 digits, so typing this leaves "1234" - the VALID code.
    const val OTP_OVERFLOW_INPUT = "123499"

    const val PASSKEY_PROMO_TITLE = "Fast and secure sign-in with a passkey"

    // What every failed data call shows while the backend_error sandbox state is on.
    const val BACKEND_ERROR_MESSAGE = "HTTP 500: Internal Server Error"
    const val NO_CARS_FOUND_MESSAGE = "No cars found for this route"
    const val EMPTY_DESTINATION_HINT = "Enter a destination to see tariffs"
    const val PULL_TO_REFRESH_HINT = "Pull down to refresh tariffs"

    const val SUPPORT_FIRST_FAQ_ANSWER =
        "Enter a destination, choose a tariff, then tap Order. Pull down to refresh the tariff list."
    const val SUPPORT_CHAT_STATUS =
        "Sandbox chat started. A support specialist will reply here."
    const val SUPPORT_EMAIL_STATUS = "Email draft prepared for support@sandbox.qa."

    // Whitespace-only input: both the destination gate on the map and the driver
    // signup submit trim it down to empty.
    const val WHITESPACE_ONLY = "   "

    const val MINIVAN_RIDE_ID = 3

    // The full tariff catalog on the map. Prices in the tilde form ("~29.70 €") -
    // currency STRINGS, same warning as YELLOW_PRICE_ON_MAP.
    val RIDE_NAMES =
        mapOf(
            1 to "Yellow",
            2 to "Turquoise",
            3 to "Minivan",
        )
    val TARIFF_PRICES_ON_MAP =
        mapOf(
            1 to "~29.70 €",
            2 to "~34.54 €",
            3 to "~39.05 €",
        )
}
