package im.channel

import im.user.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ChannelTest {

    @Test
    fun `should update channel`() {
        val user = User(1, "user", "password", "user1@daw.isel.pt")
        val channel = Channel(1, "im/channel", user, true)
        val updatedChannel = channel.updateChannel("new channel", false)
        assertEquals("new channel", updatedChannel.name)
        assertFalse(updatedChannel.isPublic)
    }

    @Test
    fun `should add member`() {
        val user = User(1, "user", "password", "user1@daw.isel.pt")
        val user2 = User(2, "user2", "password", "user2@daw.isel.pt")
        val channel = Channel(1, "im/channel", user, true)
        val updatedChannel = channel.addMember(user2, ChannelRole.MEMBER)
        assertEquals(2, updatedChannel.members.size)
        assertEquals(ChannelRole.MEMBER, updatedChannel.members[user2])
    }

    @Test
    fun `should remove member`() {
        val user = User(1, "user", "password", "user1@daw.isel.pt")
        val user2 = User(2, "user2", "password", "user2@daw.isel.pt")
        val channel = Channel(1, "im/channel", user, true)
        val updatedChannel = channel.addMember(user2, ChannelRole.MEMBER)
        val updatedChannel2 = updatedChannel.removeMember(user2)
        assertEquals(1, updatedChannel2.members.size)
    }

    @Test
    fun `should change the role of a member`() {
        val user = User(1, "user", "password", "user1@daw.isel.pt")
        val user2 = User(2, "user2", "password", "user2@daw.isel.pt")
        val channel = Channel(1, "im/channel", user, true)
        val updatedChannel = channel.addMember(user2, ChannelRole.MEMBER)
        val updatedChannel2 = updatedChannel.addMember(user2, ChannelRole.OWNER)
        assertEquals(2, updatedChannel2.members.size)
        assertEquals(ChannelRole.OWNER, updatedChannel2.members[user2])
    }
}