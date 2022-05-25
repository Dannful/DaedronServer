package me.dannly.plugins

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import me.dannly.routes.conversationsRoute
import me.dannly.routes.fileRoutes
import me.dannly.routes.userRoutes

fun Application.configureRouting() {
    install(Resources)
    routing {
        static {
            resources("static")
        }
        conversationsRoute()
        userRoutes()
        fileRoutes()
    }
}
