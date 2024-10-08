package im.services.auth

import im.wrappers.Password
import im.wrappers.toPassword
import jakarta.inject.Named
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

@Named
class PasswordEncoder {

    fun encode(password: Password, iterations: Int = 1000): Password {
        val salt = generateSalt()
        val hash = hashPassword(password, salt, iterations)
        return "${Base64.getUrlEncoder().encodeToString(salt)}:$hash".toPassword()
    }

    fun verify(password: Password, storedPassword: Password, iterations: Int = 1000): Boolean {
        val parts = storedPassword.value.split(":")
        val salt = Base64.getUrlDecoder().decode(parts[0])
        val hash = parts[1]
        val passwordHash = hashPassword(password, salt, iterations)
        return passwordHash == hash
    }

    private fun hashPassword(password: Password, salt: ByteArray, iterations: Int): String {
        var hash = password.value.toByteArray() + salt
        val digest = MessageDigest.getInstance("SHA-256")
        for (i in 1..iterations) {
            hash = digest.digest(hash)
        }
        return Base64.getUrlEncoder().encodeToString(hash)
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        val secureRandom = SecureRandom()
        secureRandom.nextBytes(salt)
        return salt
    }
}