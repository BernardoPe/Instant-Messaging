package im.services.channels

import im.domain.Either
import im.domain.Failure
import im.domain.channel.Channel
import im.domain.channel.ChannelRole
import im.domain.failure
import im.domain.success
import im.domain.user.User
import im.domain.wrappers.identifier.Identifier
import im.domain.wrappers.name.Name
import im.repository.pagination.Pagination
import im.repository.pagination.PaginationRequest
import im.repository.pagination.SortRequest
import im.repository.repositories.transactions.TransactionManager
import jakarta.inject.Named

@Named
class ChannelServiceImpl(
    private val transactionManager: TransactionManager,
) : ChannelService {
    companion object {
        private val validSortFields = setOf("id", "name", "createdAt")
        private const val DEFAULT_SORT = "createdAt"
        private val validDefaultRoles = setOf(ChannelRole.MEMBER, ChannelRole.GUEST)
    }

    override fun createChannel(
        name: Name,
        defaultRole: ChannelRole,
        isPublic: Boolean,
        user: User,
    ): Either<ChannelError, Channel> {
        return transactionManager.run {
            if (defaultRole !in validDefaultRoles) {
                return@run Failure(ChannelError.InvalidDefaultRole)
            }

            if (channelRepository.findByName(name, false) != null) {
                return@run Failure(ChannelError.ChannelAlreadyExists("name"))
            }

            val channel = channelRepository.save(Channel(name = name, defaultRole = defaultRole, owner = user, isPublic = isPublic))

            success(channel)
        }
    }

    override fun getChannelById(
        channelId: Identifier,
        user: User,
    ): Either<ChannelError, Channel> =
        transactionManager.run {
            val channel =
                channelRepository.findById(channelId)
                    ?: return@run Failure(ChannelError.ChannelNotFound)

            if (!channel.hasMember(user) && !channel.isPublic) {
                return@run Failure(ChannelError.CannotAccessChannel)
            }

            success(channel)
        }

    override fun updateChannel(
        channelId: Identifier,
        name: Name,
        defaultRole: ChannelRole,
        isPublic: Boolean,
        user: User,
    ): Either<ChannelError, Unit> =
        transactionManager.run {
            if (defaultRole !in validDefaultRoles) {
                return@run Failure(ChannelError.InvalidDefaultRole)
            }

            val channel =
                channelRepository.findById(channelId)
                    ?: return@run Failure(ChannelError.ChannelNotFound)

            if (channel.owner != user) {
                return@run Failure(ChannelError.CannotUpdateChannel)
            }

            channelRepository.save(channel.updateChannel(name, defaultRole, isPublic))

            success(Unit)
        }

    override fun deleteChannel(
        channelId: Identifier,
        user: User,
    ): Either<ChannelError, Unit> =
        transactionManager.run {
            val channel =
                channelRepository.findById(channelId)
                    ?: return@run Failure(ChannelError.ChannelNotFound)

            if (channel.owner != user) {
                return@run Failure(ChannelError.CannotDeleteChannel)
            }

            channelRepository.deleteById(channelId)

            success(Unit)
        }

    override fun joinChannel(
        channelId: Identifier,
        userId: Identifier,
        user: User,
    ): Either<ChannelError, Unit> =
        transactionManager.run {
            val channel =
                channelRepository.findById(channelId)
                    ?: return@run Failure(ChannelError.ChannelNotFound)

            if (!channel.isPublic) {
                return@run Failure(ChannelError.CannotJoinPrivateChannel)
            }

            if (userId != user.id) {
                return@run Failure(ChannelError.CannotAddMember)
            }

            val member = channelRepository.getMember(channel, user)

            if (member != null) {
                return@run Failure(ChannelError.UserAlreadyMember)
            }

            channelRepository.addMember(channel, user, channel.defaultRole)

            success(Unit)
        }

    override fun removeChannelMember(
        channelId: Identifier,
        userId: Identifier,
        user: User,
    ): Either<ChannelError, Unit> =
        transactionManager.run {
            val channel =
                channelRepository.findById(channelId)
                    ?: return@run Failure(ChannelError.ChannelNotFound)

            val toRemoveUser =
                userRepository.findById(userId)
                    ?: return@run Failure(ChannelError.UserNotFound)

            if (channel.owner != user && toRemoveUser != user) {
                return@run Failure(ChannelError.CannotRemoveMember)
            }

            channelRepository.getMember(channel, toRemoveUser)
                ?: return@run Failure(ChannelError.UserNotMember)

            channelRepository.removeMember(channel, toRemoveUser)

            success(Unit)
        }

    override fun getChannels(
        name: String?,
        pagination: PaginationRequest,
        sortRequest: SortRequest,
    ): Either<ChannelError, Pagination<Channel>> =
        transactionManager.run {
            val sort = sortRequest.sortBy ?: DEFAULT_SORT
            if (sort !in validSortFields) {
                return@run Failure(ChannelError.InvalidSortField(sort, validSortFields.toList()))
            }
            val channels =
                if (name != null) {
                    channelRepository.findByPartialName(
                        name,
                        filterPublic = true,
                        pagination,
                        sortRequest.copy(sortBy = sort),
                    )
                } else {
                    channelRepository.find(pagination, filterPublic = true, sortRequest.copy(sortBy = sort))
                }
            channels.items.forEach { it.members }
            success(channels)
        }

    override fun getUserChannels(
        userId: Identifier,
        sortRequest: SortRequest,
        user: User,
    ): Either<ChannelError, Map<Channel, ChannelRole>> =
        transactionManager.run {
            if (user.id != userId) {
                return@run failure(ChannelError.CannotAccessUserChannels)
            }

            val sort = sortRequest.sortBy ?: DEFAULT_SORT

            if (sort !in validSortFields) {
                return@run failure(ChannelError.InvalidSortField(sort, validSortFields.toList()))
            }

            val joinedChannels = channelRepository.findByMember(user, sortRequest.copy(sortBy = sort))
            joinedChannels.keys.forEach { it.members } // Load members
            success(joinedChannels)
        }

    override fun updateMemberRole(
        channelId: Identifier,
        userId: Identifier,
        role: ChannelRole,
        user: User,
    ): Either<ChannelError, Unit> =
        transactionManager.run {
            val channel =
                channelRepository.findById(channelId)
                    ?: return@run failure(ChannelError.ChannelNotFound)

            if (channel.owner != user || role == ChannelRole.OWNER || channel.owner.id == userId) {
                return@run failure(ChannelError.CannotUpdateMemberRole)
            }

            val (member) =
                channelRepository.getMember(
                    channel,
                    userRepository.findById(userId) ?: return@run failure(ChannelError.UserNotFound),
                )
                    ?: return@run failure(ChannelError.UserNotMember)

            channelRepository.updateMemberRole(channel, member, role)

            success(Unit)
        }
}
