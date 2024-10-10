package im.user

import im.wrappers.Email
import im.wrappers.Identifier
import im.wrappers.Name
import im.wrappers.Password
import im.wrappers.toEmail
import im.wrappers.toName
import im.wrappers.toPassword

/**
 * User domain class.
 *
 * @property id The unique identifier of the user.
 * @property name The name of the user.
 * @property password The password of the user.
 * @property email The email of the user.
 */
data class User(
    val id: Identifier = Identifier(0),
    val name: Name,
    val password: Password,
    val email: Email
) {

    constructor(id: Long = 0, name: String, password: String, email: String) : this(
        id = Identifier(id),
        name = name.toName(),
        password = password.toPassword(),
        email = email.toEmail()
    )
}