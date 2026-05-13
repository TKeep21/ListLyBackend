package com.example.UserMedia.exceptions

open class UserMediaException(
    message: String,
    cause: Throwable? = null

): RuntimeException(message,cause)

class UserMediaNotFoundException(userId: String?=null,
    userMediaId:String?=null): UserMediaException(
        message = if (userId!=null && userMediaId!=null) "userMedia not found for userId=$userId and mediaId=$userMediaId" else "UserMedia not found"
    )

class UserMediaAlreadyExistsException(userId:String?=null, userMediaId:String?=null): UserMediaException("mediaItem $userMediaId already exists in $userId collection")

class InvalidUserMediaRequestException(
    message:String
) : UserMediaException(
    message
)
