package im.model.input.path

import im.model.input.wrappers.Identifier
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable

data class UserIdentifier(
    @PathVariable("userId")
    @Valid
    val value: Identifier
) {
    fun toDomain() = value.toDomain()
}