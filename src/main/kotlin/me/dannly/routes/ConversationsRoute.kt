package me.dannly.routes

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.resources.serialization.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import me.dannly.domain.model.Conversation
import me.dannly.domain.repository.ConversationDataSource
import me.dannly.plugins.inject
import me.dannly.room.ConnectionManager

@kotlinx.serialization.Serializable
@Resource("/conversations")
private class Conversations {

    @kotlinx.serialization.Serializable
    @Resource("new")
    class New(val conversations: Conversations = Conversations(), val conversation: Conversation)

    @kotlinx.serialization.Serializable
    @Resource("{id}")
    class Get(val conversations: Conversations = Conversations(), val id: String)

    @kotlinx.serialization.Serializable
    @Resource("chat-socket")
    class ChatSocket(val conversations: Conversations = Conversations())
}

fun Route.conversationsRoute() {
    authenticate("auth-jwt") {
        createConversation()
        getConversation()
        chatSocket()
    }
}

private fun Route.chatSocket() {
    val connectionManager: ConnectionManager by inject()
    webSocket(href(resourcesFormat = ResourcesFormat(), resource = Conversations.ChatSocket())) {
        val principal = call.principal<JWTPrincipal>() ?: run {
            close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "An internal error occurred."))
            return@webSocket
        }
        val userId = principal.payload.getClaim("userId").asInt() ?: run {
            close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "An internal error occurred."))
            return@webSocket
        }
        try {
            connectionManager.createSession(userId, this)
            connectionManager.receiveSocketFrame(this)
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(HttpStatusCode.Conflict, e.localizedMessage)
        } finally {
            connectionManager.disconnect(userId)
        }
    }
}

private fun Route.createConversation() {
    val conversationDataSource: ConversationDataSource by inject()
    post<Conversations.New> { new ->
        conversationDataSource.insertConversation(new.conversation)
        call.respond(HttpStatusCode.OK)
    }
}

private fun Route.getConversation() {
    val conversationDataSource: ConversationDataSource by inject()
    get<Conversations.Get> { get ->
        val conversation = conversationDataSource.getConversationById(get.id) ?: run {
            call.respond(HttpStatusCode.BadRequest, "Invalid ID.")
            return@get
        }
        call.respond(HttpStatusCode.OK, conversation)
    }
}