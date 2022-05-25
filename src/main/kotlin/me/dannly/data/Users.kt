package me.dannly.data

import org.jetbrains.exposed.dao.id.IntIdTable

object Users : IntIdTable() {
    val login = varchar("login", 10)
    val displayName = varchar("display_name", 20)
    val password = varchar("password", 64)
    val imageUrl = varchar("image", 2000).nullable()
}