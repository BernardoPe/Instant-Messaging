package im.invitations

import im.channel.Channel
import im.channel.ChannelRole
import im.user.User
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ChannelInvitationTest {
    @Test
    fun `should accept invitation`() {
        val user1 = User(1, "user", "password", "user1@daw.isel.pt")
        val user2 = User(2, "user2", "password", "user2@daw.isel.pt")
        val channel = Channel(1, "im/channel", user1, true)

        val invitation = ChannelInvitation(1, channel, user1, user2, ChannelInvitationStatus.PENDING, ChannelRole.MEMBER, LocalDateTime.now())
        val acceptedInvitation = invitation.accept()

        assertEquals(ChannelInvitationStatus.ACCEPTED, acceptedInvitation.status)
    }

    @Test
    fun `should reject invitation`() {
        val user1 = User(1, "user", "password", "user1@daw.isel.pt")
        val user2 = User(2, "user2", "password", "user2@daw.isel.pt")
        val channel = Channel(1, "im/channel", user1, true)

        val invitation = ChannelInvitation(1, channel, user1, user2, ChannelInvitationStatus.PENDING, ChannelRole.MEMBER, LocalDateTime.now())
        val rejectedInvitation = invitation.reject()

        assertEquals(ChannelInvitationStatus.REJECTED, rejectedInvitation.status)
    }

    @Test
    fun `should update invitation`() {
        val user1 = User(1, "user", "password", "user1@daw.isel.pt")
        val user2 = User(2, "user2", "password", "user2@daw.isel.pt")
        val channel = Channel(1, "im/channel", user1, true)

        val invitation = ChannelInvitation(
            1,
            channel,
            user1,
            user2,
            ChannelInvitationStatus.PENDING,
            ChannelRole.MEMBER,
            LocalDateTime.now()
        )
        val updatedInvitation = invitation.update(ChannelRole.GUEST, LocalDateTime.now().plusDays(1))

        assertEquals(ChannelRole.GUEST, updatedInvitation.role)
        assertEquals(LocalDateTime.now().plusDays(1), updatedInvitation.expiresAt)
    }
}