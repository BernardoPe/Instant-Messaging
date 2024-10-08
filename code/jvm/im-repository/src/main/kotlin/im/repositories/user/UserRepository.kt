package im.repositories.user

import im.repositories.Repository
import im.channel.Channel
import im.channel.ChannelRole
import im.invitations.ChannelInvitation
import im.sessions.Session
import im.user.User

/**
 * [Repository] for [User] entities.
 */
interface UserRepository : Repository<User, Long> {
    /**
     * Finds a user by their name.
     *
     * @param name the name of the user
     * @return the user with the given name, or `null` if no such user exists
     */
    fun findByName(name: String): User?

    /**
     * Finds a user by their email.
     *
     * @param email the email of the user
     * @return the user with the given email, or `null` if no such user exists
     */
    fun findByEmail(email: String): User?

    /**
     * Finds users whose name starts with the given string, case-insensitive.
     *
     * @param name the partial name of the users
     * @return the users whose name contains the given string
     */
    fun findByPartialName(name: String): List<User>

    /**
     * Finds a user by their name and password.
     *
     * @param name the name of the user
     * @param password the password of the user
     * @return the user with the given name and password, or `null` if no such user exists
     */
    fun findByNameAndPassword(name: String, password: String): User?

    /**
     * Finds a user by their email and password.
     *
     * @param email the email of the user
     * @param password the password of the user
     * @return the user with the given email and password, or `null` if no such user exists
     */
    fun findByEmailAndPassword(email: String, password: String): User?

    /**
     * Finds all channels owned by the user.
     *
     * @param user the user
     * @return the channels owned by the user
     */
    fun getOwnedChannels(user: User): List<Channel>

    /**
     * Finds all channels the user is part of.
     *
     * @param user the user
     * @return the channels where the user is part of
     */
    fun getJoinedChannels(user: User): Map<Channel, ChannelRole>

    /**
     * Finds all invitations of the user.
     *
     * @param user the user
     * @return the invitations of the user
     */
    fun getInvitations(user: User): List<ChannelInvitation>

    /**
     * Finds all sessions of the user.
     *
     * @param user the user
     * @return the sessions of the user
     */
    fun getSessions(user: User): List<Session>
}