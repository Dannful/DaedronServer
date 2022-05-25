package me.dannly.session

import io.ktor.server.websocket.*
import me.dannly.domain.model.User

data class UserSession(
    val user: User,
    val webSocketServerSession: WebSocketServerSession
)
