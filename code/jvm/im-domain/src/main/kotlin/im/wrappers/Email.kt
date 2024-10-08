package im.wrappers

data class Email(
    val value: String
) {
    init {
        require(value.isNotBlank()) { "User email cannot be blank" }
        require(value.length in 5..50) { "User email must be between 5 and 50 characters" }
        require(value.matches(Regex("^[A-Za-z0-9+_.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$"))) { "User email must be a valid email address" }
    }

    override fun toString(): String = value
}

fun String.toEmail(): Email = Email(this)
