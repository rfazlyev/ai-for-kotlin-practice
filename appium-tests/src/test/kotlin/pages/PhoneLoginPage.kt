package pages

/** Element catalog for the phone-login screen. */
object PhoneLoginPage {
    val title = Element(variantId("phone_title", "auth_phone_screen_title"))
    val phoneInput = Element(variantId("phone_input", "auth_phone_number_field"))
    val continueButton = Element(variantId("phone_continue_button", "auth_phone_submit"))
    val errorLabel = Element(variantId("phone_error", "auth_phone_validation_message"))
}
