package me.dannly.data.repository

import me.dannly.data.Users
import me.dannly.data.dao.UserDao
import me.dannly.data.util.sha256
import me.dannly.domain.model.User
import me.dannly.domain.repository.UserDataSource
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

class UserDataSourceImpl(
    private val database: Database
) : UserDataSource {

    override fun authenticate(login: String, password: String): User? {
        return transaction(database) {
            addLogger(StdOutSqlLogger)
            UserDao.find { Users.login.eq(login) and Users.password.eq(password.sha256()) }.singleOrNull()?.toUser()
        }
    }

    override fun getById(id: Int): User? {
        return transaction(database) {
            addLogger(StdOutSqlLogger)
            UserDao.find {
                Users.id eq id
            }.singleOrNull()
        }?.toUser()
    }

    override suspend fun update(user: User, onUpdate: (suspend () -> Unit)?) {
        newSuspendedTransaction(db = database) {
            addLogger(StdOutSqlLogger)
            val userDao = UserDao[user.id]
            userDao::class.memberProperties.mapNotNull { it as? KMutableProperty<*> }.filter { property ->
                user::class.memberProperties.any { it.name == property.name }
            }.forEach { property ->
                property.setter.call(userDao, user::class.memberProperties.first {
                    it.name == property.name
                }.getter.call(user))
            }
            onUpdate?.invoke()
        }
    }

    override fun register(login: String, password: String): Boolean {
        if (transaction(database) {
                UserDao.find { Users.login eq login }.singleOrNull()
            } != null)
            return false
        transaction(database) {
            addLogger(StdOutSqlLogger)
            Users.insert {
                it[this.login] = login
                it[displayName] = login
                it[Users.password] = password.sha256()
            }
        }
        return true
    }
}