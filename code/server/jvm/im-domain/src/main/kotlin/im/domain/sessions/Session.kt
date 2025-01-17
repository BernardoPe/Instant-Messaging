package im.domain.sessions

import im.domain.user.User
import im.domain.wrappers.identifier.Identifier
import im.domain.wrappers.identifier.toIdentifier
import java.time.LocalDateTime

/**
 * Represents a session for a user.
 *
 * @property id The unique identifier of the session.
 * @property user The user that owns the session.
 * @property expiresAt The date and time when the session expires.
 * @property expired Indicates if the session has expired.
 */
data class Session(
    val id: Identifier = Identifier(0),
    val user: User,
    val expiresAt: LocalDateTime,
) {
    val expired: Boolean
        get() = expiresAt.isBefore(LocalDateTime.now())

    companion object {
        operator fun invoke(
            id: Long = 0,
            user: User,
            expiresAt: LocalDateTime,
        ): Session =
            Session(
                id = id.toIdentifier(),
                user = user,
                expiresAt = expiresAt,
            )
    }

    /**
     * Refreshes the session by setting a new expiration date.
     *
     * @param newExpiresAt the new expiration date
     * @return a new session with the updated expiration date
     */
    fun refresh(newExpiresAt: LocalDateTime): Session = copy(expiresAt = newExpiresAt)
}
