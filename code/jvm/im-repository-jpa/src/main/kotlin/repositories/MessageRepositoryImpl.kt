package repositories

import jakarta.persistence.EntityManager
import messages.Message
import messages.MessageRepository
import model.message.MessageDTO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface MessageRepositoryJpa : JpaRepository<MessageDTO, Long>

@Component
class MessageRepositoryImpl : MessageRepository {
    @Autowired
    private lateinit var messageRepositoryJpa: MessageRepositoryJpa

    @Autowired
    private lateinit var entityManager: EntityManager

    override fun findByChannelId(channelId: Long): Iterable<Message> {
        val query = entityManager.createQuery(
            "SELECT m FROM MessageDTO m WHERE m.channel.id = :channelId",
            MessageDTO::class.java
        )
        query.setParameter("channelId", channelId)
        return query.resultList.map { it.toDomain() }
    }

    override fun findLatest(channelId: Long, pages: Int, pageSize: Int): Iterable<Message> {
        val query = entityManager.createQuery(
            "SELECT m FROM MessageDTO m WHERE m.channel.id = :channelId ORDER BY m.createdAt DESC",
            MessageDTO::class.java
        )
        query.setParameter("channelId", channelId)
        query.firstResult = pages * pageSize
        query.maxResults = pageSize
        return query.resultList.map { it.toDomain() }
    }

    override fun save(entity: Message): Message {
        return messageRepositoryJpa.save(MessageDTO.fromDomain(entity)).toDomain()
    }

    override fun saveAll(entities: Iterable<Message>): List<Message> {
        return messageRepositoryJpa.saveAll(entities.map { MessageDTO.fromDomain(it) }).map { it.toDomain() }
    }

    override fun findById(id: Long): Optional<Message> {
        return messageRepositoryJpa.findById(id).map { it.toDomain() }
    }

    override fun findAll(): Iterable<Message> {
        return messageRepositoryJpa.findAll().map { it.toDomain() }
    }

    override fun findFirst(page: Int, pageSize: Int): List<Message> {
        val res = messageRepositoryJpa.findAll(org.springframework.data.domain.PageRequest.of(page, pageSize))
        return res.content.map { it.toDomain() }
    }

    override fun findLast(page: Int, pageSize: Int): List<Message> {
        val query = entityManager.createQuery("SELECT m FROM MessageDTO m ORDER BY m.id DESC", MessageDTO::class.java)
        query.firstResult = page * pageSize
        query.maxResults = pageSize
        return query.resultList.map { it.toDomain() }
    }

    override fun findAllById(ids: Iterable<Long>): Iterable<Message> {
        return messageRepositoryJpa.findAllById(ids).map { it.toDomain() }
    }

    override fun deleteById(id: Long) {
        messageRepositoryJpa.deleteById(id)
    }

    override fun existsById(id: Long): Boolean {
        return messageRepositoryJpa.existsById(id)
    }

    override fun count(): Long {
        return messageRepositoryJpa.count()
    }

    override fun deleteAll() {
        messageRepositoryJpa.deleteAll()
    }

    override fun deleteAll(entities: Iterable<Message>) {
        messageRepositoryJpa.deleteAll(entities.map { MessageDTO.fromDomain(it) })
    }

    override fun delete(entity: Message) {
        messageRepositoryJpa.delete(MessageDTO.fromDomain(entity))
    }

    override fun deleteAllById(ids: Iterable<Long>) {
        messageRepositoryJpa.deleteAllById(ids)
    }
}