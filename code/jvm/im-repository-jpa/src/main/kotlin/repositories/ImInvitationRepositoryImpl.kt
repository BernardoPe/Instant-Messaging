package repositories

import invitations.ImInvitation
import invitations.ImInvitationRepository
import jakarta.persistence.EntityManager
import model.invitation.ImInvitationDTO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ImInvitationRepositoryJpa : JpaRepository<ImInvitationDTO, Long>

@Component
class ImInvitationRepositoryImpl: ImInvitationRepository {
    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var imInvitationRepositoryJpa: ImInvitationRepositoryJpa

    override fun findByToken(token: UUID): ImInvitation? {
        val query = entityManager.createQuery(
            "SELECT i FROM ImInvitationDTO i WHERE i.token = :token",
            ImInvitationDTO::class.java
        )
        query.setParameter("token", token)
        return query.resultList.firstOrNull()?.toDomain()
    }

    override fun save(entity: ImInvitation): ImInvitation {
        return imInvitationRepositoryJpa.save(ImInvitationDTO.fromDomain(entity)).toDomain()
    }

    override fun saveAll(entities: Iterable<ImInvitation>): List<ImInvitation> {
        return imInvitationRepositoryJpa.saveAll(entities.map { ImInvitationDTO.fromDomain(it) }).map { it.toDomain() }
    }

    override fun findById(id: Long): Optional<ImInvitation> {
        return imInvitationRepositoryJpa.findById(id).map { it.toDomain() }
    }

    override fun findAll(): Iterable<ImInvitation> {
        return imInvitationRepositoryJpa.findAll().map { it.toDomain() }
    }

    override fun findFirst(page: Int, pageSize: Int): List<ImInvitation> {
        val res = imInvitationRepositoryJpa.findAll(org.springframework.data.domain.PageRequest.of(page, pageSize))
        return res.content.map { it.toDomain() }
    }

    override fun findLast(page: Int, pageSize: Int): List<ImInvitation> {
        val query = entityManager.createQuery(
            "SELECT i FROM ImInvitationDTO i ORDER BY i.id DESC",
            ImInvitationDTO::class.java
        )
        query.firstResult = page * pageSize
        query.maxResults = pageSize
        return query.resultList.map { it.toDomain() }
    }

    override fun findAllById(ids: Iterable<Long>): Iterable<ImInvitation> {
        return imInvitationRepositoryJpa.findAllById(ids).map { it.toDomain() }
    }

    override fun deleteById(id: Long) {
        imInvitationRepositoryJpa.deleteById(id)
    }

    override fun existsById(id: Long): Boolean {
        return imInvitationRepositoryJpa.existsById(id)
    }

    override fun count(): Long {
        return imInvitationRepositoryJpa.count()
    }

    override fun deleteAll() {
        imInvitationRepositoryJpa.deleteAll()
    }

    override fun deleteAll(entities: Iterable<ImInvitation>) {
        imInvitationRepositoryJpa.deleteAll(entities.map { ImInvitationDTO.fromDomain(it) })
    }

    override fun delete(entity: ImInvitation) {
        imInvitationRepositoryJpa.delete(ImInvitationDTO.fromDomain(entity))
    }

    override fun deleteAllById(ids: Iterable<Long>) {
        imInvitationRepositoryJpa.deleteAllById(ids)
    }
}