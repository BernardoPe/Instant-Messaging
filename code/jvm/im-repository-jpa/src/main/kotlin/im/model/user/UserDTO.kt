package im.model.user


import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import im.user.User

@Entity
@Table(name = "users")
open class UserDTO(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long = 0,

    @Column(unique = true, nullable = false, length = 30)
    open val name: String = "",

    @Column(nullable = false, length = 100)
    open val password: String = "",

    @Column(unique = true, nullable = false, length = 100)
    open val email: String = "",
) {
    companion object {
        fun fromDomain(user: User): UserDTO = UserDTO(
            id = user.id,
            name = user.name,
            password = user.password,
            email = user.email,
        )
    }

    fun toDomain(): User = User(
        id = id,
        name = name,
        password = password,
        email = email,
    )
}