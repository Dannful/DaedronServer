package me.dannly.room

import io.ktor.serialization.*
import io.ktor.server.websocket.*
import io.ktor.util.reflect.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import me.dannly.domain.model.Message
import me.dannly.domain.model.UserMessageStatusUpdate
import me.dannly.domain.repository.ConversationDataSource
import me.dannly.domain.repository.UserDataSource
import me.dannly.session.UserSession
import java.util.*

class ConnectionManager(
    private val conversationDataSource: ConversationDataSource, private val userDataSource: UserDataSource
) {

    private val connections = Collections.synchronizedSet<UserSession>(LinkedHashSet())

    suspend fun updateUser(userId: Int) {
        if (connections.none { it.user.id == userId }) return
        val user = userDataSource.getById(userId) ?: throw Throwable("No user found with the given ID.")
        val newSession = connections.first { it.user.id == userId }.copy(
            user = user
        )
        connections.removeIf { it.user.id == userId }
        connections += newSession
        conversationDataSource.getUserConversations(userId).forEach { conversation ->
            connections.filter { it.user.id in conversation.participants && it.user.id != userId }
                .map { it.webSocketServerSession }.forEach { webSocketServerSession ->
                    webSocketServerSession.sendSerialized(user)
                }
        }
    }

    fun createSession(userId: Int, webSocketServerSession: WebSocketServerSession) {
        if (connections.any { it.user.id == userId }) throw Throwable("User already in session.")
        val user = userDataSource.getById(userId) ?: throw Throwable("User not found.")
        connections += UserSession(user = user, webSocketServerSession = webSocketServerSession)
    }

    suspend fun disconnect(userId: Int) {
        connections.find { it.user.id == userId }?.webSocketServerSession?.close()
        connections.removeIf { it.user.id == userId }
    }

    private suspend fun WebSocketServerSession.receiveSocketMessage(frame: Frame) {
        try {
            val message = receive<Message>(frame) ?: return
            conversationDataSource.addMessageAndGetReceivers(message)
            message.broadcastToAllParticipants(conversationId = message.conversationId)
        } catch (ignored: Exception) {
        }
    }

    private suspend fun WebSocketServerSession.receiveUserMessageStatusUpdate(frame: Frame) {
        try {
            val userMessageStatusUpdate = receive<UserMessageStatusUpdate>(frame) ?: return
            userMessageStatusUpdate.broadcastToAllParticipants(
                participants = conversationDataSource.insertUserReadStatus(
                    userMessageStatusUpdate
                )
            )
        } catch (ignored: Exception) {
        }
    }

    suspend fun receiveSocketFrame(webSocketServerSession: WebSocketServerSession) {
        webSocketServerSession.incoming.consumeEach { frame ->
            webSocketServerSession.receiveSocketMessage(frame)
            webSocketServerSession.receiveUserMessageStatusUpdate(frame)
        }
    }

    private suspend inline fun <reified T : Any> T.broadcastToAllParticipants(
        participants: List<Int>
    ) {
        connections.filter { participants.contains(it.user.id) }.forEach {
            it.webSocketServerSession.sendSerialized(this)
        }
    }

    private suspend inline fun <reified T : Any> T.broadcastToAllParticipants(
        conversationId: String
    ) {
        val participants = conversationDataSource.getConversationById(conversationId)?.participants ?: emptyList()
        connections.filter { participants.contains(it.user.id) }.forEach {
            it.webSocketServerSession.sendSerialized(this)
        }
    }

    private suspend inline fun <reified T : Any> WebSocketServerSession.receive(frame: Frame) =
        converter?.deserialize(
            call.request.headers.suitableCharset(),
            typeInfo<T>(),
            frame
        ) as? T
}