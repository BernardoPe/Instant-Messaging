package im.api.utils

import im.api.model.input.AuthenticatedUser
import im.domain.tokens.AccessToken
import im.domain.user.User
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import java.time.ZoneOffset
import java.util.UUID

private const val ACCESS_TOKEN_COOKIE_NAME = "access_token"
private const val REFRESH_TOKEN_COOKIE_NAME = "refresh_token"
private const val AUTHENTICATED_USER_ATTRIBUTE = "user"

private const val REGISTER_ROUTE = "/api/auth/register"
private const val LOGIN_ROUTE = "/api/auth/login"
private const val REFRESH_ROUTE = "/api/auth/refresh"

@Component
class RequestHelper {
    fun getNoAuthRoutes(): List<String> = listOf(REGISTER_ROUTE, LOGIN_ROUTE, REFRESH_ROUTE)

    fun setAuthenticatedUser(
        req: HttpServletRequest,
        user: User,
        accessToken: UUID,
    ) {
        req.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, AuthenticatedUser(user, accessToken))
    }

    fun getRefreshToken(req: HttpServletRequest): UUID? {
        val refreshToken =
            req.getHeader("Authorization")?.removePrefix("Bearer ")
                ?: req.cookies?.find { it.name == REFRESH_TOKEN_COOKIE_NAME }?.value
        return refreshToken?.let { UUID.fromString(it) }
    }

    fun setCookie(
        res: HttpServletResponse,
        accessToken: AccessToken,
    ) {
        val cookie =
            Cookie(ACCESS_TOKEN_COOKIE_NAME, accessToken.token.toString())
                .apply {
                    maxAge = accessToken.expiresAt.toEpochSecond(ZoneOffset.UTC).toInt()
                    isHttpOnly = true
                    secure = true
                    path = "/"
                }
        res.addCookie(cookie)
    }

    fun setCookie(
        res: HttpServletResponse,
        refreshToken: im.domain.tokens.RefreshToken,
    ) {
        val cookie =
            Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken.token.toString())
                .apply {
                    maxAge =
                        refreshToken.session.expiresAt
                            .toEpochSecond(ZoneOffset.UTC)
                            .toInt()
                    isHttpOnly = true
                    secure = true
                    path = "/"
                }
        res.addCookie(cookie)
    }

    fun clearRefreshToken(res: HttpServletResponse) {
        val cookie =
            Cookie(REFRESH_TOKEN_COOKIE_NAME, "")
                .apply {
                    maxAge = 0
                    isHttpOnly = true
                    secure = true
                    path = "/"
                }
        res.addCookie(cookie)
    }

    fun clearAccessToken(res: HttpServletResponse) {
        val cookie =
            Cookie(ACCESS_TOKEN_COOKIE_NAME, "")
                .apply {
                    maxAge = 0
                    isHttpOnly = true
                    secure = true
                    path = "/"
                }
        res.addCookie(cookie)
    }
}