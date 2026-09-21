package pages

/** Element catalog for the OTP screen. */
object OtpPage {
    val title = Element(variantId("otp_title", "auth_otp_screen_title"))
    val codeInput = Element(variantId("otp_input", "auth_otp_code_field"))
    val confirmButton = Element(variantId("otp_continue_button", "auth_otp_submit"))
    val errorLabel = Element(variantId("otp_error", "auth_otp_validation_message"))

    /** Disabled while the 30 s cooldown ticks; its label shows the countdown. */
    val resendButton = Element(variantId("otp_resend_button", "auth_otp_resend"))
}
