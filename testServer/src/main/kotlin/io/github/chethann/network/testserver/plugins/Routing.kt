package example.com.plugins

import io.github.chethann.network.testserver.model.TestData
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.delay
import java.io.File
import java.util.UUID

fun Application.configureRouting() {
    routing {
        get("/") {
            delay(1000)
            call.respondText("Hello World!")
        }

        get("/successEndpoint") {
            delay(1000)
            call.respond(TestData(uuid = UUID.randomUUID().toString()))
        }

        get("/object") {
            delay(1000)
            call.respond(TestData(uuid = UUID.randomUUID().toString()))
        }

        get("/errorEndpoint") {
            delay(1000)
            call.respond(HttpStatusCode.Forbidden, TestData(uuid = UUID.randomUUID().toString()))
        }

        post("/postEndpoint") {
            delay(1000)
            call.respond("Post body successfully processed!")
        }

        post("/upload") {
            val multipart = call.receiveMultipart()
            var fileName: String? = null

            println("Received multipart request")

            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    fileName = part.originalFileName as String

                    // Create the uploads directory if it doesn't exist
                    val uploadDir = File("uploads")
                    if (!uploadDir.exists()) {
                        uploadDir.mkdir()
                    }

                    val file = File(uploadDir, fileName)


                    // Using the provider without .use()
                    val inputStream = part.provider()
                    val outputStream = file.outputStream().buffered()

                    // Copy data from input stream to output stream
                    inputStream.copyTo(outputStream)

                    // Close the streams manually
                    //inputStream.
                    outputStream.close()
                }
                part.dispose()
            }

            if (fileName != null) {
                call.respond(HttpStatusCode.OK, "File uploaded successfully: $fileName")
            } else {
                call.respond(HttpStatusCode.BadRequest, "No file received")
            }
        }


        // Static plugin. Try to access `/static/index.html`
        staticResources("/static", "static")
    }
}

