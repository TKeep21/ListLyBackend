package com.example.auth.exceptions


open class AuthException(message: String) : RuntimeException(message)

class InvalidCredentialsException :
    AuthException("Invalid login or password")

class UserAlreadyExistsException :
    AuthException("User already exists")

class TooManyCharactersInLoginException:
        AuthException("Your login must be 20 characters or below")

class TooManyCharactersInPasswordException:
        AuthException("Your password must be 25 characters or below")
class TooShortLoginException:
        AuthException("Your login must be more than 3 characters")
class TooShortPasswordException:
        AuthException("Your password must be more than 6 characters")
class EmptyFieldException:
        AuthException("This field can't be empty")
