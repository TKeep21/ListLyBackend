package com.example.search.exceptions

open class SearchException(message:String, cause: Throwable? = null): RuntimeException(message,cause)

class SearchUnavailableException(
    message: String,
    cause: Throwable? = null
) : SearchException(message, cause)

class SearchRequestFailedException(
    val statusCode: Int,
    val responseBody: String
) : SearchException("Search request failed with status=$statusCode: $responseBody")

class InvalidSearchRequestException(message: String) : SearchException(message)

class SearchParsingException(
    message: String,
    cause: Throwable
) : SearchException(message, cause)

class MeiliClientException(message: String,cause: Throwable?= null) : RuntimeException(message,cause)
