package me.dannly.domain.repository

import me.dannly.domain.model.User

interface UserDataSource {

    fun getById(id: Int): User?
    fun authenticate(login: String, password: String): User?
    fun register(login: String, password: String): Boolean
    suspend fun update(user: User, onUpdate: (suspend () -> Unit)? = null)
}