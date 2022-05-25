package me.dannly.domain.model

@kotlinx.serialization.Serializable
data class User(
    val authToken: String? = null,
    val id: Int,
    val login: String,
    val displayName: String,
    val imageUrl: String?
)
