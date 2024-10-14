package im.repository.jpa.repositories

import im.domain.user.User
import im.domain.wrappers.Email
import im.domain.wrappers.Identifier
import im.domain.wrappers.Name
import im.domain.wrappers.Password
import im.repository.jpa.model.user.UserDTO
import im.repository.jpa.repositories.jpa.UserRepositoryJpa
import im.repository.pagination.Pagination
import im.repository.pagination.PaginationRequest
import im.repository.pagination.SortRequest
import im.repository.repositories.user.UserRepository
import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
@Primary
class UserRepositoryImpl(
    private val userRepositoryJpa: UserRepositoryJpa,
    private val entityManager: EntityManager,
    private val utils: JpaRepositoryUtils,
) : UserRepository {
    override fun findByName(name: Name): User? = userRepositoryJpa.findByName(name.value).firstOrNull()?.toDomain()

    override fun findByEmail(email: Email): User? = userRepositoryJpa.findByEmail(email.value).firstOrNull()?.toDomain()

    override fun findByPartialName(
        name: String,
        pagination: PaginationRequest,
        sortRequest: SortRequest,
    ): Pagination<User> {
        val pageable = utils.toPageRequest(pagination, sortRequest)
        val res =
            if (pagination.getCount) {
                userRepositoryJpa.findByPartialName(name, pageable)
            } else {
                userRepositoryJpa.findByPartialNameSliced(name, pageable)
            }
        return Pagination(res.content.map { it.toDomain() }, utils.getPaginationInfo(res))
    }

    override fun findByNameAndPassword(
        name: Name,
        password: Password,
    ): User? = userRepositoryJpa.findByNameAndPassword(name.value, password.value).firstOrNull()?.toDomain()

    override fun findByEmailAndPassword(
        email: Email,
        password: Password,
    ): User? = userRepositoryJpa.findByEmailAndPassword(email.value, password.value).firstOrNull()?.toDomain()

    override fun save(entity: User): User = userRepositoryJpa.save(UserDTO.fromDomain(entity)).toDomain()

    override fun saveAll(entities: Iterable<User>): List<User> =
        userRepositoryJpa
            .saveAll(
                entities.map {
                    UserDTO.fromDomain(it)
                },
            ).map { it.toDomain() }

    override fun findById(id: Identifier): User? = userRepositoryJpa.findById(id.value).map { it.toDomain() }.orElse(null)

    override fun findAll(): List<User> = userRepositoryJpa.findAll().map { it.toDomain() }

    override fun find(
        pagination: PaginationRequest,
        sortRequest: SortRequest,
    ): Pagination<User> {
        val pageable = utils.toPageRequest(pagination, sortRequest)
        val res =
            if (pagination.getCount) {
                userRepositoryJpa.findAll(pageable)
            } else {
                userRepositoryJpa.findBy(pageable)
            }
        return Pagination(res.content.map { it.toDomain() }, utils.getPaginationInfo(res))
    }

    override fun findAllById(ids: Iterable<im.domain.wrappers.Identifier>): List<User> =
        userRepositoryJpa
            .findAllById(
                ids.map {
                    it.value
                },
            ).map { it.toDomain() }

    override fun deleteById(id: Identifier) {
        userRepositoryJpa.deleteById(id.value)
    }

    override fun existsById(id: Identifier): Boolean = userRepositoryJpa.existsById(id.value)

    override fun count(): Long = userRepositoryJpa.count()

    override fun deleteAll() {
        userRepositoryJpa.deleteAll()
    }

    override fun deleteAll(entities: Iterable<User>) {
        userRepositoryJpa.deleteAll(entities.map { UserDTO.fromDomain(it) })
    }

    override fun delete(entity: User) {
        userRepositoryJpa.delete(UserDTO.fromDomain(entity))
    }

    override fun deleteAllById(ids: Iterable<im.domain.wrappers.Identifier>) {
        userRepositoryJpa.deleteAllById(ids.map { it.value })
    }

    override fun flush() {
        userRepositoryJpa.flush()
    }
}