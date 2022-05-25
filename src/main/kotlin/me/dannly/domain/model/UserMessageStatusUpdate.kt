package me.dannly.domain.model

@kotlinx.serialization.Serializable
data class UserMessageStatusUpdate(
    val conversationId: String,
    val messageId: String,
    val userId: Int,
    val read: Boolean
)
