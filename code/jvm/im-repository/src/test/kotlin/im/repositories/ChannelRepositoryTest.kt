package im.repositories

import im.TestApp
import im.domain.channel.Channel
import im.domain.channel.ChannelRole
import im.domain.invitations.ChannelInvitation
import im.domain.invitations.ChannelInvitationStatus
import im.domain.messages.Message
import im.domain.user.User
import im.domain.wrappers.name.toName
import im.repository.pagination.PaginationRequest
import im.repository.pagination.Sort
import im.repository.pagination.SortRequest
import im.repository.repositories.transactions.TransactionManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@ContextConfiguration(classes = [TestApp::class])
abstract class ChannelRepositoryTest {
    @Autowired
    private lateinit var transactionManager: TransactionManager

    private lateinit var testChannel1: Channel
    private lateinit var testChannel2: Channel
    private lateinit var testOwner: User
    private lateinit var testInvitation: ChannelInvitation
    private lateinit var testMessage: Message
    private lateinit var testMember: User

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
        insertData()
    }

    private fun insertData() {
        transactionManager.run {
            testOwner = userRepository.save(User(1, "Owner", "Password123", "user1@daw.isel.pt"))
            testMember = userRepository.save(User(2, "Member", "Password123", "user2@daw.isel.pt"))

            testChannel1 =
                Channel(
                    id = 1L,
                    name = "General",
                    owner = testOwner,
                    isPublic = true,
                    createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
                )

            testChannel2 =
                Channel(
                    id = 2L,
                    name = "Gaming",
                    owner = testOwner,
                    isPublic = false,
                    createdAt = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.MILLIS),
                )

            testInvitation =
                ChannelInvitation(
                    id = 1L,
                    channel = testChannel1,
                    inviter = testOwner,
                    invitee = testOwner,
                    status = ChannelInvitationStatus.PENDING,
                    role = ChannelRole.MEMBER,
                    expiresAt = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MILLIS),
                )

            testMessage =
                Message(
                    id = 1L,
                    channel = testChannel1,
                    content = "Hello",
                    createdAt = LocalDateTime.now(),
                    user = testOwner,
                    editedAt = null,
                )
        }
    }

    @Test
    open fun `save with same name should throw exception`() {
        assertThrows<Exception> {
            transactionManager.run {
                channelRepository.save(testChannel1)
                testChannel2 = testChannel2.copy(name = testChannel1.name)
                channelRepository.save(testChannel2)
            }
        }
    }

    @Test
    open fun `should save multiple channels`() {
        transactionManager.run {
            val channels = listOf(testChannel1, testChannel2)
            val savedChannels = channelRepository.saveAll(channels)
            assertEquals(2, savedChannels.size)
        }
    }

    @Test
    open fun `should find channel by id`() {
        transactionManager.run {
            val savedChannel = channelRepository.save(testChannel1)
            val foundChannel = channelRepository.findById(savedChannel.id)
            assertNotNull(foundChannel)
            assertEquals(savedChannel.id, foundChannel?.id)
        }
    }

    @Test
    open fun `should find channel by name`() {
        transactionManager.run {
            channelRepository.save(testChannel1)
            val foundChannel = channelRepository.findByName("General".toName(), false)
            assertNotNull(foundChannel)
            assertEquals("General", foundChannel!!.name.value)
        }
    }

    @Test
    open fun `find filter public should return only public channels`() {
        transactionManager.run {
            channelRepository.save(testChannel1)
            channelRepository.save(testChannel2)
            val (channels) = channelRepository.find(PaginationRequest(1, 10), true, SortRequest("id"))
            assertEquals(1, channels.size)
            assertEquals(testChannel1.name, channels.first().name)
        }
    }

    @Test
    open fun `should delete on owner delete`() {
        transactionManager.run {
            channelRepository.save(testChannel1)
            assertEquals(1, channelRepository.count())
            userRepository.delete(testOwner)

            userRepository.flush()
            channelRepository.flush()

            assertEquals(0, channelRepository.count())
        }
    }

    @Test
    open fun `should return empty for non-existent channel name`() {
        transactionManager.run {
            val foundChannel = channelRepository.findByName("NonExistentChannel".toName(), false)
            assertNull(foundChannel)
        }
    }

    @Test
    open fun `should find channels by partial name`() {
        transactionManager.run {
            channelRepository.save(testChannel1)
            channelRepository.save(testChannel2)
            val (foundChannels) =
                channelRepository.findByPartialName(
                    "Gen",
                    false,
                    PaginationRequest(1, 10),
                    SortRequest("id"),
                )
            assertEquals(1, foundChannels.count())
            assertEquals(testChannel1.name, foundChannels.first().name)
        }
    }

    @Test
    open fun `should find all channels`() {
        transactionManager.run {
            channelRepository.save(testChannel1)
            channelRepository.save(testChannel2)
            val channels = channelRepository.findAll()
            assertEquals(2, channels.count())
        }
    }

    @Test
    open fun `should find first page of channels`() {
        transactionManager.run {
            channelRepository.save(testChannel1)
            channelRepository.save(testChannel2)

            val res = channelRepository.find(PaginationRequest(1, 1), SortRequest("id"))

            val (firstChannels, pagination) = res

            assertEquals(2, pagination!!.total)
            assertEquals(1, pagination.currentPage)
            assertEquals(2, pagination.nextPage)
            assertEquals(2, pagination.totalPages)
            assertEquals(null, pagination.prevPage)

            assertEquals(1, firstChannels.size)
            assertEquals(testChannel1.name, firstChannels.first().name)
        }
    }

    @Test
    open fun `should find last page of channels ordered by id desc`() {
        transactionManager.run {
            channelRepository.save(testChannel1)
            channelRepository.save(testChannel2)

            val (lastChannels, pagination) =
                channelRepository.find(
                    PaginationRequest(
                        1,
                        2,
                    ),
                    SortRequest("id", Sort.DESC),
                )

            assertEquals(2, pagination!!.total)
            assertEquals(1, pagination.currentPage)
            assertEquals(null, pagination.nextPage)
            assertEquals(1, pagination.totalPages)
            assertEquals(null, pagination.prevPage)

            assertEquals(2, lastChannels.size)
            assertEquals(testChannel2.name, lastChannels.first().name)
            assertEquals(testChannel1.name, lastChannels.last().name)
        }
    }

    @Test
    fun `pagination no count`() {
        transactionManager.run {
            channelRepository.save(testChannel1)
            channelRepository.save(testChannel2)

            val res = channelRepository.find(PaginationRequest(1, 1, getCount = false), false, SortRequest("id"))

            val (firstChannels, pagination) = res

            assertNotNull(pagination)
            assertEquals(1, firstChannels.size)
            assertEquals(testChannel1.name, firstChannels.first().name)
            assertNull(pagination!!.total)
            assertNull(pagination.totalPages)
            assertEquals(1, pagination.currentPage)
            assertEquals(2, pagination.nextPage)
            assertNull(pagination.prevPage)
        }
    }

    @Test
    open fun `should update channel`() {
        transactionManager.run {
            val savedChannel = channelRepository.save(testChannel1)
            val updatedChannel = savedChannel.copy(name = "UpdatedName".toName(), isPublic = false)
            val result = channelRepository.save(updatedChannel)
            assertEquals("UpdatedName", result.name.value)
            assertFalse(result.isPublic)
        }
    }

    @Test
    open fun `get joined channels should return empty list`() {
        transactionManager.run {
            testChannel1 = testChannel1.copy(owner = testOwner)
            testChannel2 = testChannel2.copy(owner = testOwner)

            testChannel1 = channelRepository.save(testChannel1)
            testChannel2 = channelRepository.save(testChannel2)

            val channels = channelRepository.findByMember(testMember, SortRequest("id"))
            assertTrue(channels.none())
        }
    }

    @Test
    open fun `get joined channels should return 1 channel`() {
        transactionManager.run {
            testChannel1 = testChannel1.addMember(testMember, ChannelRole.MEMBER)
            testChannel1 = testChannel1.copy(owner = testOwner)
            testChannel2 = testChannel2.copy(owner = testOwner)

            testChannel1 = channelRepository.save(testChannel1)
            testChannel2 = channelRepository.save(testChannel2)

            val channels = channelRepository.findByMember(testMember, SortRequest("id"))
            assertTrue(channels.size == 1)
            assertEquals(testChannel1, channels.keys.first())
            assertEquals(ChannelRole.MEMBER, channels.values.first())
        }
    }

    @Test
    open fun `get owned channels should be empty`() {
        transactionManager.run {
            testChannel1 =
                testChannel1.copy(owner = testOwner, membersLazy = lazy { mapOf(testMember to ChannelRole.MEMBER) })
            testChannel2 =
                testChannel2.copy(owner = testOwner, membersLazy = lazy { mapOf(testMember to ChannelRole.MEMBER) })

            channelRepository.save(testChannel1)
            channelRepository.save(testChannel2)

            val channels = channelRepository.findByOwner(testMember, SortRequest("id"))
            assertTrue(channels.none())
        }
    }

    @Test
    open fun `get owned channels should return 1 channel`() {
        transactionManager.run {
            testChannel1 = testChannel1.copy(owner = testOwner)
            testChannel2 = testChannel2.copy(owner = testMember)

            testChannel1 = channelRepository.save(testChannel1)
            testChannel2 = channelRepository.save(testChannel2)

            val channels = channelRepository.findByOwner(testOwner, SortRequest("id"))
            assertEquals(1, channels.size)
            assertEquals(testChannel1, channels.first())
        }
    }

    @Test
    open fun `get owned channels should return empty list`() {
        transactionManager.run {
            val channels = channelRepository.findByOwner(testOwner, SortRequest("id"))
            assertTrue(channels.none())
        }
    }

    @Test
    open fun `get member should return empty`() {
        transactionManager.run {
            val savedChannel = channelRepository.save(testChannel1)
            val member = channelRepository.getMember(savedChannel, testMember)
            assertNull(member)
        }
    }

    @Test
    open fun `get member should return member`() {
        transactionManager.run {
            val savedChannel = channelRepository.save(testChannel1)
            val newChannel = savedChannel.addMember(testMember, ChannelRole.MEMBER)
            val updatedChannel = channelRepository.save(newChannel)
            val member = channelRepository.getMember(updatedChannel, testMember)
            assertNotNull(member)
            assertEquals(ChannelRole.MEMBER, member!!.second)
            assertEquals(testMember, member.first)
        }
    }

    @Test
    open fun `should add member to channel with role Member`() {
        transactionManager.run {
            val savedChannel = channelRepository.save(testChannel1)
            val newChannel = savedChannel.addMember(testMember, ChannelRole.MEMBER)
            val updatedChannel = channelRepository.save(newChannel)
            assertEquals(2, updatedChannel.members.size) // Owner + Member
            assertEquals(ChannelRole.MEMBER, updatedChannel.members[testMember])
        }
    }

    @Test
    open fun `should add member to channel with role Guest`() {
        transactionManager.run {
            val savedChannel = channelRepository.save(testChannel1)
            val newChannel = savedChannel.addMember(testMember, ChannelRole.GUEST)
            val updatedChannel = channelRepository.save(newChannel)
            assertEquals(2, updatedChannel.members.size) // Owner + Guest
            assertEquals(ChannelRole.GUEST, updatedChannel.members[testMember])
        }
    }

    @Test
    open fun `should remove member from channel`() {
        transactionManager.run {
            val savedChannel = channelRepository.save(testChannel1)

            val newChannel = savedChannel.addMember(testMember, ChannelRole.MEMBER)
            val updatedChannel = channelRepository.save(newChannel)
            assertEquals(2, updatedChannel.members.size)
            assertEquals(ChannelRole.MEMBER, updatedChannel.members[testMember])
            // Remove Member
            val newChannel2 = updatedChannel.removeMember(testMember)
            val updatedChannel2 = channelRepository.save(newChannel2)

            assertEquals(1, updatedChannel2.members.size) // Owner
        }
    }

    @Test
    open fun `should delete channel by id`() {
        transactionManager.run {
            val savedChannel = channelRepository.save(testChannel1)
            channelRepository.deleteById(savedChannel.id)
            assertEquals(0, channelRepository.count())
        }
    }

    @Test
    open fun `should delete multiple channels by ids`() {
        transactionManager.run {
            val savedChannel1 = channelRepository.save(testChannel1)
            val savedChannel2 = channelRepository.save(testChannel2)

            channelRepository.deleteAllById(listOf(savedChannel1.id, savedChannel2.id))
            assertEquals(0, channelRepository.count())
        }
    }
}
