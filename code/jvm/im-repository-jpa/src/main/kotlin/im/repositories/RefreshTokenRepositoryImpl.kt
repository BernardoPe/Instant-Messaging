package im.repositories

import im.model.token.RefreshTokenDTO
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import im.tokens.RefreshToken
import im.tokens.RefreshTokenRepository
import java.util.*

@Repository
interface RefreshTokenRepositoryJpa : JpaRepository<RefreshTokenDTO, UUID>

@Component
class RefreshTokenRepositoryImpl(
    private val refreshTokenRepositoryJpa: RefreshTokenRepositoryJpa,
    private val entityManager: EntityManager
) : RefreshTokenRepository {

    override fun save(entity: RefreshToken): RefreshToken {
        return refreshTokenRepositoryJpa.save(RefreshTokenDTO.fromDomain(entity)).toDomain()
    }

    override fun saveAll(entities: Iterable<RefreshToken>): List<RefreshToken> {
        return refreshTokenRepositoryJpa.saveAll(entities.map { RefreshTokenDTO.fromDomain(it) }).map { it.toDomain() }
    }

    override fun findById(id: UUID): RefreshToken? {
        return refreshTokenRepositoryJpa.findById(id).map { it.toDomain() }.orElse(null)
    }

    override fun findAll(): List<RefreshToken> {
        return refreshTokenRepositoryJpa.findAll().map { it.toDomain() }
    }

    override fun findFirst(page: Int, pageSize: Int): List<RefreshToken> {
        val res = refreshTokenRepositoryJpa.findAll(Pageable.ofSize(pageSize).withPage(page))
        return res.content.map { it.toDomain() }
    }

    override fun findLast(page: Int, pageSize: Int): List<RefreshToken> {
        val query = entityManager.createQuery(
            "SELECT u FROM RefreshTokenDTO u ORDER BY u.session.expiresAt DESC",
            RefreshTokenDTO::class.java
        )
        query.firstResult = page * pageSize
        query.maxResults = pageSize
        return query.resultList.map { it.toDomain() }
    }

    override fun findAllById(ids: Iterable<UUID>): List<RefreshToken> {
        return refreshTokenRepositoryJpa.findAllById(ids).map { it.toDomain() }
    }

    override fun deleteById(id: UUID) {
        refreshTokenRepositoryJpa.deleteById(id)
    }

    override fun existsById(id: UUID): Boolean {
        return refreshTokenRepositoryJpa.existsById(id)
    }

    override fun count(): Long {
        return refreshTokenRepositoryJpa.count()
    }

    override fun deleteAll() {
        refreshTokenRepositoryJpa.deleteAll()
    }

    override fun deleteAll(entities: Iterable<RefreshToken>) {
        refreshTokenRepositoryJpa.deleteAll(entities.map { RefreshTokenDTO.fromDomain(it) })
    }

    override fun delete(entity: RefreshToken) {
        refreshTokenRepositoryJpa.delete(RefreshTokenDTO.fromDomain(entity))
    }

    override fun deleteAllById(ids: Iterable<UUID>) {
        refreshTokenRepositoryJpa.deleteAllById(ids)
    }

    override fun flush() {
        entityManager.flush()
        refreshTokenRepositoryJpa.flush()
    }
}