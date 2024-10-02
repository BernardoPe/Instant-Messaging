package model.user


import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import user.User

@Entity
@Table(name = "users")
data class UserDTO(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false, length = 30)
    val name: String = "",

    @Column(nullable = false, length = 100)
    val password: String = "",
) {
    companion object {
        fun fromDomain(user: User): UserDTO = UserDTO(
            id = user.id,
            name = user.name,
            password = user.password,
        )
    }

    fun toDomain(): User = User(
        id = id,
        name = name,
        password = password,
    )
}
