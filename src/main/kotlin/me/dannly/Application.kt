package me.dannly

import io.ktor.server.application.*
import me.dannly.data.Users
import me.dannly.di.mainModule
import me.dannly.plugins.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    install(KoinPlugin) {
        modules(mainModule)
    }
    setupMySQL()
    configureSerialization()
    configureSockets()
    configureAuthentication()
    configureRouting()
    configureMonitoring()
}

private fun setupMySQL() {
    val database: Database by inject()
    transaction(
        Database.connect(
            "jdbc:mysql://localhost:3306/", driver = "com.mysql.cj.jdbc.Driver",
            user = "root", password = "admin"
        )
    ) {
        SchemaUtils.createDatabase("users")
    }
    transaction(database) {
        SchemaUtils.create(Users)
    }
}