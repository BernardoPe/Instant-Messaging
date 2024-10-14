package im

import im.domain.channel.Channel
import im.domain.channel.ChannelRole
import im.domain.invitations.ChannelInvitation
import im.domain.invitations.ChannelInvitationStatus
import im.domain.user.User
import im.domain.wrappers.Identifier
import im.repository.pagination.SortRequest
import im.repository.repositories.transactions.TransactionManager
import im.services.Failure
import im.services.Success
import im.services.invitations.InvitationError
import im.services.invitations.InvitationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
abstract class InvitationServiceTest {
    @Autowired
    private lateinit var invitationService: InvitationService

    @Autowired
    private lateinit var transactionManager: TransactionManager

    private var testUser1 = User(1L, "testUser", "password", "iseldaw@isel.pt")
    private var testUser2 = User(2L, "testUser2", "password", "iseldaw2@isel.pt")
    private var testUser3 = User(3L, "testUser3", "password", "iseldaw3@isel.pt")
    private var testChannel = Channel(1L, "testChannel", testUser1, true)
    private var testChannel2 = Channel(2L, "testChannel2", testUser2, true)

    @BeforeEach
    fun setup() {
        transactionManager.run {
            refreshTokenRepository.deleteAll()
            accessTokenRepository.deleteAll()
            imInvitationRepository.deleteAll()
            channelInvitationRepository.deleteAll()
            messageRepository.deleteAll()
            channelInvitationRepository.deleteAll()
            channelRepository.deleteAll()
            sessionRepository.deleteAll()
            userRepository.deleteAll()
        }
        transactionManager.run {
            testUser1 = userRepository.save(testUser1)
            testUser2 = userRepository.save(testUser2)
            testUser3 = userRepository.save(testUser3)
            testChannel =
                testChannel.copy(owner = testUser1, membersLazy = lazy { mapOf(testUser1 to ChannelRole.OWNER) })
            testChannel = channelRepository.save(testChannel)
            testChannel2 =
                testChannel2.copy(owner = testUser2, membersLazy = lazy { mapOf(testUser2 to ChannelRole.OWNER) })
            testChannel2 = channelRepository.save(testChannel2)
        }
    }

    @Test
    fun `create invitation successfully`() {
        val result =
            invitationService.createInvitation(
                channelId = testChannel.id,
                inviteeId = testUser2.id,
                expirationDate = LocalDateTime.now().plusDays(1),
                role = ChannelRole.MEMBER,
                inviter = testUser1,
            )
        assertIs<Success<ChannelInvitation>>(result)
        val invitation = result.value
        assertEquals(testUser1, invitation.inviter)
        assertEquals(testUser2, invitation.invitee)
        assertEquals(testChannel, invitation.channel)
        assertEquals(ChannelRole.MEMBER, invitation.role)
    }

    @Test
    fun `fail to create invitation for non-existing channel`() {
        val result =
            invitationService.createInvitation(
                channelId = Identifier(999L),
                inviteeId = testUser2.id,
                expirationDate = LocalDateTime.now().plusDays(1),
                role = ChannelRole.MEMBER,
                inviter = testUser1,
            )
        assertIs<Failure<InvitationError>>(result)
        assertIs<InvitationError.ChannelNotFound>(result.value)
    }

    @Test
    fun `fail to create invitation for invitee already a member`() {
        val result =
            invitationService.createInvitation(
                channelId = testChannel2.id,
                inviteeId = testUser2.id,
                expirationDate = LocalDateTime.now().plusDays(1),
                role = ChannelRole.MEMBER,
                inviter = testUser2,
            )
        assertIs<Failure<InvitationError>>(result)
        assertIs<InvitationError.InviteeAlreadyMember>(result.value)
    }

    @Test
    fun `fail to create invitation with invalid expiration date`() {
        val result =
            invitationService.createInvitation(
                channelId = testChannel.id,
                inviteeId = testUser2.id,
                expirationDate = LocalDateTime.now().plusMonths(10),
                role = ChannelRole.MEMBER,
                inviter = testUser1,
            )
        assertIs<Failure<InvitationError>>(result)
        assertIs<InvitationError.InvalidInvitationExpiration>(result.value)
    }

    @Test
    fun `fail to create invitation with non-existing invitee`() {
        val result =
            invitationService.createInvitation(
                channelId = testChannel.id,
                inviteeId = Identifier(999L),
                expirationDate = LocalDateTime.now().plusDays(1),
                role = ChannelRole.MEMBER,
                inviter = testUser1,
            )
        assertIs<Failure<InvitationError>>(result)
        assertIs<InvitationError.InviteeNotFound>(result.value)
    }

    @Test
    fun `fail to create invitation invite already exists`() {
        val result =
            invitationService.createInvitation(
                channelId = testChannel.id,
                inviteeId = testUser2.id,
                expirationDate = LocalDateTime.now().plusDays(1),
                role = ChannelRole.MEMBER,
                inviter = testUser1,
            )
        assertIs<Success<ChannelInvitation>>(result)

        val result2 =
            invitationService.createInvitation(
                channelId = testChannel.id,
                inviteeId = testUser2.id,
                expirationDate = LocalDateTime.now().plusDays(1),
                role = ChannelRole.MEMBER,
                inviter = testUser1,
            )
        assertIs<Failure<InvitationError>>(result2)
        assertIs<InvitationError.InvitationAlreadyExists>(result2.value)
    }

    @Test
    fun `fail to create invitation no permission`() {
        val result =
            invitationService.createInvitation(
                channelId = testChannel.id,
                inviteeId = testUser2.id,
                expirationDate = LocalDateTime.now().plusDays(1),
                role = ChannelRole.MEMBER,
                inviter = testUser2,
            )
        assertIs<Failure<InvitationError>>(result)
        assertIs<InvitationError.UserCannotInviteToChannel>(result.value)
    }

    @Test
    fun `get invitation should return invitation`() {
        val result =
            invitationService.createInvitation(
                channelId = testChannel.id,
                inviteeId = testUser2.id,
                expirationDate = LocalDateTime.now().plusDays(1),
                role = ChannelRole.MEMBER,
                inviter = testUser1,
            )
        assertIs<Success<ChannelInvitation>>(result)
        val invitation = result.value

        val result2 =
            invitationService.getInvitation(
                channelId = testChannel.id,
                inviteId = invitation.id,
                user = testUser1,
            )

        assertIs<Success<ChannelInvitation>>(result2)
        assertEquals(invitation.id, result2.value.id)
    }

    @Test
    fun `get invitation, invitation not found`() {
        val result =
            invitationService.getInvitation(
                channelId = testChannel.id,
                inviteId = Identifier(999L),
                user = testUser1,
            )
        assertIs<Failure<InvitationError>>(result)
        assertIs<InvitationError.InvitationNotFound>(result.value)
    }

    @Test
    fun `get invitation, channel not found`() {
        val result =
            invitationService.getInvitation(
                channelId = Identifier(999L),
                inviteId = Identifier(999L),
                user = testUser1,
            )
        assertIs<Failure<InvitationError>>(result)
        assertIs<InvitationError.ChannelNotFound>(result.value)
    }

    @Test
    fun `get invitation user cannot access`() {
        val result =
            invitationService.createInvitation(
                channelId = testChannel.id,
                inviteeId = testUser2.id,
                expirationDate = LocalDateTime.now().plusDays(1),
                role = ChannelRole.MEMBER,
                inviter = testUser1,
            )
        assertIs<Success<ChannelInvitation>>(result)
        val invitation = result.value

        val result2 =
            invitationService.getInvitation(
                channelId = testChannel.id,
                inviteId = invitation.id,
                user = testUser3,
            )
        assertIs<Failure<InvitationError>>(result2)
        assertIs<InvitationError.UserCannotAccessInvitation>(result2.value)
    }

    @Test
    fun `get channel invitations should return invitations`() {
        val invite =
            invitationService.createInvitation(
                channelId = testChannel.id,
                inviteeId = testUser2.id,
                expirationDate = LocalDateTime.now().plusDays(1),
                role = ChannelRole.MEMBER,
                inviter = testUser1,
            )
        assertIs<Success<ChannelInvitation>>(invite)
        val invite2 =
            invitationService.createInvitation(
                channelId = testChannel.id,
                inviteeId = testUser3.id,
                expirationDate = LocalDateTime.now().plusDays(1),
                role = ChannelRole.MEMBER,
                inviter = testUser1,
            )
        assertIs<Success<ChannelInvitation>>(invite2)

        val result2 =
            invitationService.getChannelInvitations(
                channelId = testChannel.id,
                user = testUser1,
                SortRequest("id"),
            )

        assertIs<Success<List<ChannelInvitation>>>(result2)
        assertEquals(2, result2.value.size)
        assertEquals(invite.value.id, result2.value[0].id)
        assertEquals(invite2.value.id, result2.value[1].id)
    }

    @Test
    fun `get channel invitations, channel not found`() {
        val result =
            invitationService.getChannelInvitations(
                channelId = Identifier(999L),
                user = testUser1,
                SortRequest("id"),
            )
        assertIs<Failure<InvitationError>>(result)
        assertIs<InvitationError.ChannelNotFound>(result.value)
    }

    @Test
    fun `get channel invitations, user cannot access`() {
        val result =
            invitationService.getChannelInvitations(
                channelId = testChannel.id,
                user = testUser2,
                SortRequest("id"),
            )
        assertIs<Failure<InvitationError>>(result)
        assertIs<InvitationError.UserCannotAccessInvitation>(result.value)
    }

    @Test
    fun `update invitation should update invitation`() {
        val invite =
            invitationService.createInvitation(
                channelId = testChannel.id,
                inviteeId = testUser2.id,
                expirationDate = LocalDateTime.now().plusDays(1),
                role = ChannelRole.MEMBER,
                inviter = testUser1,
            )

        assertIs<Success<ChannelInvitation>>(invite)
        assertEquals(ChannelRole.MEMBER, invite.value.role)

        val result =
            invitationService.updateInvitation(
                channelId = testChannel.id,
                inviteId = invite.value.id,
                role = ChannelRole.GUEST,
                expirationDate = LocalDateTime.now().plusDays(5),
                user = testUser1,
            )

        assertIs<Success<Unit>>(result)
        val updatedInvite =
            invitationService.getInvitation(
                channelId = testChannel.id,
                inviteId = invite.value.id,
                user = testUser1,
            )
        assertIs<Success<ChannelInvitation>>(updatedInvite)
        assertEquals(ChannelRole.GUEST, updatedInvite.value.role)
    }

    @Test
    fun `update invitation, invitation not found`() {
        val result =
            invitationService.updateInvitation(
                channelId = testChannel.id,
                inviteId = Identifier(999L),
                role = ChannelRole.GUEST,
                expirationDate = LocalDateTime.now().plusDays(5),
                user = testUser1,
            )
        assertIs<Failure<InvitationError>>(result)
        assertIs<InvitationError.InvitationNotFound>(result.value)
    }

    @Test
    fun `update invitation, channel not found`() {
        val result =
            invitationService.updateInvitation(
                channelId = Identifier(999L),
                inviteId = Identifier(999L),
                role = ChannelRole.GUEST,
                expirationDate = LocalDateTime.now().plusDays(5),
                user = testUser1,
            )
        assertIs<Failure<InvitationError>>(result)
        assertIs<InvitationError.ChannelNotFound>(result.value)
    }

    @Test
    fun `update invitation, user cannot update`() {
        val invite =
            invitationService.createInvitation(
                channelId = testChannel.id,
                inviteeId = testUser2.id,
                expirationDate = LocalDateTime.now().plusDays(1),
                role = ChannelRole.MEMBER,
                inviter = testUser1,
            )

        assertIs<Success<ChannelInvitation>>(invite)

        val result =
            invitationService.updateInvitation(
                channelId = testChannel.id,
                inviteId = invite.value.id,
                role = ChannelRole.GUEST,
                expirationDate = LocalDateTime.now().plusDays(5),
                user = testUser2,
            )
        assertIs<Failure<InvitationError>>(result)
        assertIs<InvitationError.UserCannotUpdateInvitation>(result.value)
    }

    @Test
    fun `update expired invitation should fail`() {
        val invite =
            transactionManager.run {
                channelInvitationRepository.save(
                    ChannelInvitation(
                        channel = testChannel,
                        invitee = testUser2,
                        inviter = testUser1,
                        role = ChannelRole.MEMBER,
                        status = ChannelInvitationStatus.PENDING,
                        expiresAt = LocalDateTime.now().minusDays(1),
                    ),
                )
            }
        val result =
            invitationService.updateInvitation(
                channelId = testChannel.id,
                inviteId = invite.id,
                role = ChannelRole.GUEST,
                expirationDate = LocalDateTime.now().plusDays(5),
                user = testUser1,
            )
        assertIs<Failure<InvitationError>>(result)
        assertIs<InvitationError.InvitationInvalid>(result.value)
    }

    @Test
    fun `update invitation invalid expiration date`() {
        val invite =
            invitationService.createInvitation(
                channelId = testChannel.id,
                inviteeId = testUser2.id,
                expirationDate = LocalDateTime.now().plusDays(1),
                role = ChannelRole.MEMBER,
                inviter = testUser1,
            )

        assertIs<Success<ChannelInvitation>>(invite)

        val result =
            invitationService.updateInvitation(
                channelId = testChannel.id,
                inviteId = invite.value.id,
                role = ChannelRole.GUEST,
                expirationDate = LocalDateTime.now().minusDays(1),
                user = testUser1,
            )
        assertIs<Failure<InvitationError>>(result)
        assertIs<InvitationError.InvalidInvitationExpiration>(result.value)
    }

    @Test
    fun `delete invitation successfully`() {
        val createResult =
            invitationService.createInvitation(
                channelId = testChannel.id,
                inviteeId = testUser2.id,
                expirationDate = LocalDateTime.now().plusDays(1),
                role = ChannelRole.MEMBER,
                inviter = testUser1,
            )
        assertIs<Success<ChannelInvitation>>(createResult)
        val invitation = createResult.value

        val deleteResult =
            invitationService.deleteInvitation(
                channelId = testChannel.id,
                inviteId = invitation.id,
                user = testUser1,
            )
        assertIs<Success<Unit>>(deleteResult)
    }

    @Test
    fun `delete invitation channel not found`() {
        val result =
            invitationService.deleteInvitation(
                channelId = Identifier(999L),
                inviteId = Identifier(999L),
                user = testUser1,
            )
        assertIs<Failure<InvitationError>>(result)
        assertIs<InvitationError.ChannelNotFound>(result.value)
    }

    @Test
    fun `delete invitation invitation not found`() {
        val result =
            invitationService.deleteInvitation(
                channelId = testChannel.id,
                inviteId = Identifier(999L),
                user = testUser1,
            )
        assertIs<Failure<InvitationError>>(result)
        assertIs<InvitationError.InvitationNotFound>(result.value)
    }

    @Test
    fun `fail to delete invitation as non-inviter`() {
        val createResult =
            invitationService.createInvitation(
                channelId = testChannel.id,
                inviteeId = testUser2.id,
                expirationDate = LocalDateTime.now().plusDays(1),
                role = ChannelRole.MEMBER,
                inviter = testUser1,
            )
        assertIs<Success<ChannelInvitation>>(createResult)
        val invitation = createResult.value

        val deleteResult =
            invitationService.deleteInvitation(
                channelId = testChannel.id,
                inviteId = invitation.id,
                user = testUser2,
            )
        assertIs<Failure<InvitationError>>(deleteResult)
        assertIs<InvitationError.UserCannotDeleteInvitation>(deleteResult.value)
    }

    @Test
    fun `get user invitations should return invitations`() {
        val invite =
            invitationService.createInvitation(
                channelId = testChannel.id,
                inviteeId = testUser2.id,
                expirationDate = LocalDateTime.now().plusDays(1),
                role = ChannelRole.MEMBER,
                inviter = testUser1,
            )
        assertIs<Success<ChannelInvitation>>(invite)

        val result2 =
            invitationService.getUserInvitations(
                userId = testUser2.id,
                user = testUser2,
                SortRequest("id"),
            )

        assertIs<Success<List<ChannelInvitation>>>(result2)
        assertEquals(1, result2.value.size)
        assertEquals(invite.value.id, result2.value[0].id)
    }

    @Test
    fun `get user invitations should return empty list`() {
        val result2 =
            invitationService.getUserInvitations(
                userId = testUser2.id,
                user = testUser2,
                SortRequest("id"),
            )

        assertIs<Success<List<ChannelInvitation>>>(result2)
        assertEquals(0, result2.value.size)
    }

    @Test
    fun `get user invitations, user cannot access`() {
        val result =
            invitationService.getUserInvitations(
                userId = testUser2.id,
                user = testUser1,
                SortRequest("id"),
            )
        assertIs<Failure<InvitationError>>(result)
        assertIs<InvitationError.UserCannotAccessInvitation>(result.value)
    }

    @Test
    fun `accept expired invitation`() {
        val invite =
            transactionManager.run {
                channelInvitationRepository.save(
                    ChannelInvitation(
                        channel = testChannel,
                        invitee = testUser2,
                        inviter = testUser1,
                        role = ChannelRole.MEMBER,
                        status = ChannelInvitationStatus.PENDING,
                        expiresAt = LocalDateTime.now().minusDays(1),
                    ),
                )
            }
        val result =
            invitationService.acceptOrRejectInvitation(
                userId = testUser2.id,
                invitationIdentifier = invite.id,
                status = ChannelInvitationStatus.ACCEPTED,
                user = testUser2,
            )
        assertIs<Failure<InvitationError>>(result)
        assertIs<InvitationError.InvitationInvalid>(result.value)
    }

    @Test
    fun `accept invitation, should accept invitation`() {
        val invite =
            invitationService.createInvitation(
                channelId = testChannel.id,
                inviteeId = testUser2.id,
                expirationDate = LocalDateTime.now().plusDays(1),
                role = ChannelRole.MEMBER,
                inviter = testUser1,
            )
        assertIs<Success<ChannelInvitation>>(invite)

        val result =
            invitationService.acceptOrRejectInvitation(
                userId = testUser2.id,
                invitationIdentifier = invite.value.id,
                status = ChannelInvitationStatus.ACCEPTED,
                user = testUser2,
            )
        assertIs<Success<Unit>>(result)

        val channel =
            transactionManager.run {
                val lazy = channelRepository.findById(testChannel.id)
                lazy?.members
                lazy
            }

        assertNotNull(channel)
        assertEquals(2, channel.members.size)
        assertTrue { channel.members.containsKey(testUser2) }
    }

    @Test
    fun `reject invitation, should reject invitation`() {
        val invite =
            invitationService.createInvitation(
                channelId = testChannel.id,
                inviteeId = testUser2.id,
                expirationDate = LocalDateTime.now().plusDays(1),
                role = ChannelRole.MEMBER,
                inviter = testUser1,
            )
        assertIs<Success<ChannelInvitation>>(invite)

        val result =
            invitationService.acceptOrRejectInvitation(
                userId = testUser2.id,
                invitationIdentifier = invite.value.id,
                status = ChannelInvitationStatus.REJECTED,
                user = testUser2,
            )
        assertIs<Success<Unit>>(result)

        val channel =
            transactionManager.run {
                val lazy = channelRepository.findById(testChannel.id)
                lazy?.members
                lazy
            }

        assertNotNull(channel)
        assertEquals(1, channel.members.size)
    }

    @Test
    fun `accept invitation, invitation not found`() {
        val result =
            invitationService.acceptOrRejectInvitation(
                userId = testUser2.id,
                invitationIdentifier = Identifier(999L),
                status = ChannelInvitationStatus.ACCEPTED,
                user = testUser2,
            )
        assertIs<Failure<InvitationError>>(result)
        assertIs<InvitationError.InvitationNotFound>(result.value)
    }

    @Test
    fun `accept invitation, user cannot access`() {
        val invite =
            invitationService.createInvitation(
                channelId = testChannel.id,
                inviteeId = testUser2.id,
                expirationDate = LocalDateTime.now().plusDays(1),
                role = ChannelRole.MEMBER,
                inviter = testUser1,
            )
        assertIs<Success<ChannelInvitation>>(invite)

        val result =
            invitationService.acceptOrRejectInvitation(
                userId = testUser2.id,
                invitationIdentifier = invite.value.id,
                status = ChannelInvitationStatus.ACCEPTED,
                user = testUser1,
            )
        assertIs<Failure<InvitationError>>(result)
        assertIs<InvitationError.UserCannotAccessInvitation>(result.value)
    }
}
