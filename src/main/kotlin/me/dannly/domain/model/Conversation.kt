package me.dannly.domain.model

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@kotlinx.serialization.Serializable
data class Conversation(
    @BsonId
    val id: String = ObjectId().toString(),
    val name: String? = null,
    val image: String? = null,
    val messages: List<Message> = emptyList(),
    val participants: List<Int>
)
