package me.dannly.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.resources.*
import io.ktor.server.resources.put
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.dannly.domain.model.User
import me.dannly.domain.repository.ConversationDataSource
import me.dannly.domain.repository.UserDataSource
import me.dannly.plugins.inject
import me.dannly.room.ConnectionManager
import java.util.*
import java.util.concurrent.TimeUnit

@kotlinx.serialization.Serializable
@Resource("/users")
private class Users {

    @kotlinx.serialization.Serializable
    @Resource("register")
    class Register(val parent: Users = Users(), val username: String, val password: String)

    @kotlinx.serialization.Serializable
    @Resource("login")
    class Login(val parent: Users = Users(), val username: String, val password: String)

    @kotlinx.serialization.Serializable
    @Resource("update")
    class Update(val parent: Users = Users(), val user: User)

    @kotlinx.serialization.Serializable
    @Resource("get")
    class Get(val parent: Users = Users(), val id: Int) {

        @kotlinx.serialization.Serializable
        @Resource("conversations")
        class Conversations(val parent: Get)
    }
}

fun Route.userRoutes() {
    registerUser()
    updateUser()
    getUser()
    getUserConversations()
    loginRoute()
}

private fun Route.registerUser() {
    val userDataSource: UserDataSource by inject()
    post<Users.Register> { register ->
        if (userDataSource.register(register.username, register.password)) call.respond(HttpStatusCode.Created)
        else call.respond(HttpStatusCode.Conflict, "Username already taken.")
    }
}

private fun Route.updateUser() {
    val userDataSource: UserDataSource by inject()
    val connectionManager: ConnectionManager by inject()
    put<Users.Update> { update ->
        userDataSource.update(update.user) {
            connectionManager.updateUser(update.user.id)
        }
        call.respond(HttpStatusCode.OK)
    }
}

private fun Route.getUser() {
    val userDataSource: UserDataSource by inject()
    get<Users.Get> { get ->
        call.respond(HttpStatusCode.OK, userDataSource.getById(get.id) ?: run {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        })
    }
}

private fun Route.getUserConversations() {
    val conversationDataSource: ConversationDataSource by inject()
    authenticate("auth-jwt") {
        get<Users.Get.Conversations> { conversations ->
            call.respond(HttpStatusCode.OK, conversationDataSource.getUserConversations(conversations.parent.id))
        }
    }
}

private fun Route.loginRoute() {
    val secret = environment?.config?.property("jwt.secret")?.getString() ?: return
    val issuer = environment?.config?.property("jwt.issuer")?.getString() ?: return
    val audience = environment?.config?.property("jwt.audience")?.getString() ?: return
    val userDataSource: UserDataSource by inject()
    post<Users.Login> {
        val user = userDataSource.authenticate(it.username, it.password) ?: run {
            call.respond(HttpStatusCode.BadRequest, "Invalid credentials.")
            return@post
        }
        val token = JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", user.id)
            .withExpiresAt(Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)))
            .sign(Algorithm.HMAC256(secret))
        call.respond(
            HttpStatusCode.OK, user.copy(authToken = token)
        )
    }
}