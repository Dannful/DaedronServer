package me.dannly.di

import me.dannly.data.repository.ConversationDataSourceImpl
import me.dannly.data.repository.UserDataSourceImpl
import me.dannly.domain.repository.ConversationDataSource
import me.dannly.domain.repository.UserDataSource
import me.dannly.room.ConnectionManager
import org.jetbrains.exposed.sql.Database
import org.koin.dsl.module
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

val mainModule = module {
    single {
        KMongo.createClient().coroutine.getDatabase("conversations_db")
    }
    single {
        Database.connect(
            "jdbc:mysql://localhost:3306/users", driver = "com.mysql.cj.jdbc.Driver",
            user = "root", password = "admin"
        )
    }
    single<ConversationDataSource> {
        ConversationDataSourceImpl(get())
    }
    single<UserDataSource> {
        UserDataSourceImpl(get())
    }
    single {
        ConnectionManager(get(), get())
    }
}