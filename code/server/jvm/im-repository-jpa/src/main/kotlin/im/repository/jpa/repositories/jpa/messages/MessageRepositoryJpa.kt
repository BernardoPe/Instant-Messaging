package im.repository.jpa.repositories.jpa.messages

import im.repository.jpa.model.message.MessageDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface MessageRepositoryJpa : JpaRepository<MessageDTO, Long> {
    @Query(
        value = "SELECT m FROM MessageDTO m JOIN FETCH m.user where m.channelId = :channelId AND m.createdAt < :before",
    )
    fun findByChannelId(
        channelId: Long,
        page: Pageable,
        before: LocalDateTime,
    ): Page<MessageDTO>

    @Query(
        value = "SELECT m FROM MessageDTO m JOIN FETCH m.user WHERE m.channelId = :channelId AND m.createdAt < :before",
    )
    fun findByChannelIdSliced(
        channelId: Long,
        page: Pageable,
        before: LocalDateTime,
    ): Slice<MessageDTO>

    fun findByChannelIdAndId(
        channelId: Long,
        id: Long,
    ): MessageDTO?

    fun findBy(pageable: Pageable): Slice<MessageDTO>
}
