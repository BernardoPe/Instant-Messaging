package im.domain.messages

import im.domain.Either
import im.domain.failure
import im.domain.success

class MessageValidator(
    private val minLength: Int = MIN_LENGTH,
    private val maxLength: Int = MAX_LENGTH,
) {
    companion object {
        private const val MIN_LENGTH = 1
        private const val MAX_LENGTH = 300
    }

    fun validate(message: String): Either<List<MessageValidationError>, Unit> {
        val errors = mutableListOf<MessageValidationError>()

        if (message.isBlank()) {
            errors.add(MessageValidationError.ContentBlank)
        }

        if (message.length !in minLength..maxLength) {
            errors.add(MessageValidationError.ContentLength(minLength, maxLength))
        }

        if (errors.isNotEmpty()) {
            return failure(errors)
        }

        return success(Unit)
    }
}
