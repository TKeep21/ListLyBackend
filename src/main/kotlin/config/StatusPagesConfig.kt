package com.example.plugins

import com.example.UserMedia.exceptions.*
import com.example.UserFolder.exceptions.ForbiddenUserFolderAccessException
import com.example.UserFolder.exceptions.InvalidUserFolderRequestException
import com.example.UserFolder.exceptions.UserFolderAlreadyExistsException
import com.example.UserFolder.exceptions.UserFolderNotFoundException
import com.example.auth.exceptions.EmptyFieldException
import com.example.auth.exceptions.InvalidCredentialsException
import com.example.auth.exceptions.TooManyCharactersInLoginException
import com.example.auth.exceptions.TooManyCharactersInPasswordException
import com.example.auth.exceptions.TooShortLoginException
import com.example.auth.exceptions.TooShortPasswordException
import com.example.auth.exceptions.UserAlreadyExistsException
import com.example.media.InvalidMediaRequestException
import com.example.media.MediaAlreadyExistsException
import com.example.media.MediaNotFoundException
import com.example.search.exceptions.InvalidSearchRequestException
import com.example.search.exceptions.SearchRequestFailedException
import com.example.search.exceptions.SearchUnavailableException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.*

fun Application.configureStatusPages() {

    install(StatusPages) {

        exception<UserMediaNotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to (cause.message ?: "User media not found"))
            )
        }

        exception<InvalidUserMediaRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to cause.message)
            )
        }

        exception<UserFolderNotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to (cause.message ?: "Folder not found"))
            )
        }

        exception<InvalidUserFolderRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to cause.message)
            )
        }

        exception<UserFolderAlreadyExistsException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                mapOf("error" to cause.message)
            )
        }

        exception<ForbiddenUserFolderAccessException> { call, cause ->
            call.respond(
                HttpStatusCode.Forbidden,
                mapOf("error" to cause.message)
            )
        }

        exception<InvalidMediaRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to cause.message)
            )
        }

        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to cause.message)
            )
        }

        exception < UserMediaAlreadyExistsException> { call, cause ->
            call.respond(HttpStatusCode.Conflict,mapOf("error" to cause.message))
        }

        exception<MediaAlreadyExistsException> { call, cause -> call.respond(HttpStatusCode.Conflict,mapOf("error" to cause.message))  }

        exception<MediaNotFoundException> { call, cause -> call.respond(HttpStatusCode.NotFound,mapOf("error" to cause.message)) }

        exception <InvalidCredentialsException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to cause.message)) }


        exception<UserAlreadyExistsException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, mapOf("error" to cause.message))
        }


        exception<TooManyCharactersInLoginException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to cause.message))
        }

        exception<TooManyCharactersInPasswordException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to cause.message))
        }

        exception<TooShortPasswordException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest,mapOf("error" to cause.message))
        }

        exception<TooShortLoginException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest,mapOf("error" to cause.message))
        }

        exception<EmptyFieldException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to cause.message))
        }
        
        
        exception<SearchUnavailableException> { call, cause ->
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                mapOf("error" to (cause.message ?: "Search Service unavailable"))
            )
        }

        exception<SearchRequestFailedException> { call, cause ->
            call.respond(
                HttpStatusCode.BadGateway,
                mapOf("error" to (cause.message ?: "Search failed"))
            )
        }

        exception<InvalidSearchRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (cause.message ?: "Invalid Search request")))
        }

        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Internal server error")
            )
            cause.printStackTrace()
        }
    }
}
