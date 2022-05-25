package me.dannly.domain.model

import org.bson.codecs.pojo.annotations.BsonId

@kotlinx.serialization.Serializable
data class Message(
    @BsonId
    val id: String,
    val conversationId: String,
    val text: String,
    val authorId: Int,
    val authorName: String,
    val authorImage: String?,
    val timestamp: Long,
    val messageStatus: Map<Int, Boolean>
)
