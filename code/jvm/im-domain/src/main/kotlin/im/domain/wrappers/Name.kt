package im.domain.wrappers

/**
 * Name wrapper class that enforces name validation rules.
 *
 * A name must not be blank and must be between 3 and 30 characters.
 */
data class Name(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Name cannot be blank" }
        require(value.length in 3..30) { "Name must be between 3 and 30 characters" }
    }

    override fun toString(): String = value
}

fun String.toName(): Name = Name(this)