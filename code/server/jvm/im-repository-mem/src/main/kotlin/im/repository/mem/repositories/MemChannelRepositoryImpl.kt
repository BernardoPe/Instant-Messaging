package im.repository.mem.repositories

import im.domain.channel.Channel
import im.domain.channel.ChannelRole
import im.domain.user.User
import im.domain.wrappers.identifier.Identifier
import im.domain.wrappers.name.Name
import im.repository.mem.model.channel.ChannelDTO
import im.repository.mem.model.channel.ChannelRoleDTO
import im.repository.mem.model.user.UserDTO
import im.repository.pagination.Pagination
import im.repository.pagination.PaginationRequest
import im.repository.pagination.SortRequest
import im.repository.repositories.channel.ChannelRepository
import java.util.concurrent.ConcurrentHashMap

class MemChannelRepositoryImpl(
    private val utils: MemRepoUtils,
    private val messageRepository: MemMessageRepositoryImpl,
    private val channelInvitationRepository: MemChannelInvitationRepositoryImpl,
) : ChannelRepository {
    private val channels = ConcurrentHashMap<Long, ChannelDTO>()

    private var id = 999L // Start from 1000 to avoid conflicts with channels created in tests

    override fun findByName(
        name: Name,
        filterPublic: Boolean,
    ): Channel? = channels.values.find { it.name == name.value && (!filterPublic || it.isPublic) }?.toDomain()

    override fun findByPartialName(
        name: String,
        filterPublic: Boolean,
        pagination: PaginationRequest,
        sortRequest: SortRequest,
        after: Identifier,
    ): Pagination<Channel> {
        val filteredChannels = channels.values.filter { it.name.startsWith(name) && (!filterPublic || it.isPublic) && it.id > after.value }
        val page = utils.paginate(filteredChannels, pagination, sortRequest)
        return Pagination(page.items.map { it.toDomain() }, page.info)
    }

    override fun find(
        pagination: PaginationRequest,
        filterPublic: Boolean,
        sortRequest: SortRequest,
        after: Identifier,
    ): Pagination<Channel> {
        val filteredChannels = channels.values.filter { (!filterPublic || it.isPublic) && it.id > after.value }
        val page = utils.paginate(filteredChannels, pagination, sortRequest)
        return Pagination(page.items.map { it.toDomain() }, page.info)
    }

    override fun find(
        pagination: PaginationRequest,
        sortRequest: SortRequest,
    ): Pagination<Channel> {
        val page = utils.paginate(channels.values.toList(), pagination, sortRequest)
        return Pagination(page.items.map { it.toDomain() }, page.info)
    }

    override fun getMember(
        channel: Channel,
        user: User,
    ): Pair<User, ChannelRole>? = channel.members[user]?.let { user to it }

    override fun findByOwner(
        user: User,
        pagination: PaginationRequest,
        sortRequest: SortRequest,
        after: Identifier,
    ): Pagination<Channel> {
        val page = utils.paginate(channels.values.filter { it.owner.toDomain() == user && it.id > after.value }, pagination, sortRequest)
        return Pagination(page.items.map { it.toDomain() }, page.info)
    }

    override fun findByMember(
        user: User,
        pagination: PaginationRequest,
        sortRequest: SortRequest,
        after: Identifier,
    ): Pagination<Channel> {
        val page =
            utils.paginate(
                channels.values.filter {
                    it.members.keys.any { it.toDomain() == user } && it.id > after.value
                },
                pagination,
                sortRequest,
            )
        return Pagination(page.items.map { it.toDomain() }, page.info)
    }

    override fun addMember(
        channel: Channel,
        user: User,
        role: ChannelRole,
    ) {
        val channelDTO = channels[channel.id.value] ?: throw IllegalArgumentException("Channel not found")
        val newMembers = channelDTO.members + (UserDTO.fromDomain(user) to ChannelRoleDTO.fromDomain(role))
        channels[channel.id.value] = channelDTO.copy(members = newMembers)
    }

    override fun removeMember(
        channel: Channel,
        user: User,
    ) {
        val channelDTO = channels[channel.id.value] ?: throw IllegalArgumentException("Channel not found")
        val newMembers = channelDTO.members.filter { it.key.toDomain() != user }
        channels[channel.id.value] = channelDTO.copy(members = newMembers)
    }

    override fun updateMemberRole(
        channel: Channel,
        user: User,
        role: ChannelRole,
    ) {
        val channelDTO = channels[channel.id.value] ?: throw IllegalArgumentException("Channel not found")
        val newMembers = channelDTO.members.mapValues { if (it.key.toDomain() == user) ChannelRoleDTO.fromDomain(role) else it.value }
        channels[channel.id.value] = channelDTO.copy(members = newMembers)
    }

    private fun findRole(
        channel: ChannelDTO,
        user: User,
    ): ChannelRole =
        channel.members.entries
            .find { it.key.toDomain() == user }
            ?.value
            ?.toDomain() ?: ChannelRole.MEMBER

    override fun save(entity: Channel): Channel {
        val conflict = channels.values.find { it.name == entity.name.value || it.id == entity.id.value }
        if (conflict != null && conflict.id != entity.id.value) {
            throw IllegalArgumentException("Channel with name ${entity.name} already exists")
        }
        if (conflict != null) {
            channels[entity.id.value] = ChannelDTO.fromDomain(entity)
            return entity
        } else {
            val newId = Identifier(++id)
            val newChannel = entity.copy(id = newId)
            channels[newId.value] = ChannelDTO.fromDomain(newChannel)
            return newChannel
        }
    }

    override fun saveAll(entities: Iterable<Channel>): List<Channel> {
        val newEntities = entities.map { save(it) }
        return newEntities.toList()
    }

    override fun findById(id: Identifier): Channel? = channels[id.value]?.toDomain()

    override fun findAll(): List<Channel> = channels.values.map { it.toDomain() }

    override fun findAllById(ids: Iterable<Identifier>): List<Channel> =
        channels.values
            .filter { channel ->
                channel.id in
                    ids.map {
                        it.value
                    }
            }.map { it.toDomain() }

    override fun deleteById(id: Identifier) {
        if (channels.containsKey(id.value)) {
            delete(channels[id.value]!!.toDomain())
        }
    }

    override fun existsById(id: Identifier): Boolean = channels.containsKey(id.value)

    override fun count(): Long = channels.size.toLong()

    override fun deleteAll() {
        id = 999L
        channels.forEach { delete(it.value.toDomain()) }
    }

    override fun deleteAll(entities: Iterable<Channel>) {
        entities.forEach { delete(it) }
    }

    override fun delete(entity: Channel) {
        messageRepository.deleteAllByChannel(entity)
        channelInvitationRepository.deleteAllByChannel(entity)
        channels.remove(entity.id.value)
    }

    override fun deleteAllById(ids: Iterable<Identifier>) {
        ids.forEach { deleteById(it) }
    }

    fun deleteAllByOwner(user: User) {
        channels.values.filter { it.owner.toDomain() == user }.forEach { delete(it.toDomain()) }
    }

    override fun flush() {
        // No-op
    }
}
