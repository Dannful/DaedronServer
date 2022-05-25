package me.dannly.routes

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.dannly.domain.repository.UserDataSource
import me.dannly.plugins.inject
import me.dannly.room.ConnectionManager
import java.io.File
import javax.imageio.ImageIO

fun Route.fileRoutes() {
    uploadRoute()
}

private fun Route.uploadRoute() {
    val userDataSource: UserDataSource by inject()
    val connectionManager: ConnectionManager by inject()
    var name = ""
    var id = ""
    post("/upload") {
        val multiPartData = call.receiveMultipart()
        multiPartData.forEachPart { partData ->
            when (partData) {
                is PartData.FormItem -> when (partData.name) {
                    "name" -> name = partData.value
                    "id" -> id = partData.value
                }
                is PartData.FileItem -> {
                    val imageUrl = "/avatars/$id/$name"
                    val file = File("build/resources/main/static/avatars/$id", name)
                    withContext(Dispatchers.IO) {
                        file.parentFile?.deleteRecursively()
                        file.parentFile.mkdirs()
                        file.createNewFile()
                        ImageIO.write(ImageIO.read(partData.streamProvider()), "png", file.outputStream())
                    }
                    userDataSource.update(
                        userDataSource.getById(id.toInt())
                            ?.copy(imageUrl = imageUrl) ?: return@forEachPart
                    ) {
                        connectionManager.updateUser(id.toInt())
                    }
                    call.respond(HttpStatusCode.OK, imageUrl)
                }
                else -> Unit
            }
            partData.dispose()
        }
    }
}