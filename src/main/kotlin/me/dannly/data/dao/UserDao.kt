package me.dannly.data.dao

import io.ktor.server.auth.*
import me.dannly.domain.model.User
import me.dannly.data.Users
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class UserDao(id: EntityID<Int>) : IntEntity(id), Principal {

    companion object : IntEntityClass<UserDao>(Users)

    var displayName by Users.displayName
    var login by Users.login
    var imageUrl by Users.imageUrl

    fun toUser() = User(
        id = id.value,
        login = login,
        displayName = displayName,
        imageUrl = imageUrl
    )
}