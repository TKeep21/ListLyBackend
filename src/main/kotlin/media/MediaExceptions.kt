package com.example.media

import com.example.UserMedia.exceptions.UserMediaException

open class MediaException(
    message: String,
    cause: Throwable? = null

): RuntimeException(message,cause)

class MediaNotFoundException(): MediaException(
    message =  "Media not found"
)

class MediaAlreadyExistsException(mediaId:String?=null): MediaException("mediaItem $mediaId already exists")

class InvalidMediaRequestException(
    message:String
) : MediaException(
    message
)