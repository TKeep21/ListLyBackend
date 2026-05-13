package com.example.auth


import com.example.auth.exceptions.EmptyFieldException
import com.example.auth.exceptions.InvalidCredentialsException
import com.example.auth.exceptions.TooManyCharactersInLoginException
import com.example.auth.exceptions.TooManyCharactersInPasswordException
import com.example.auth.exceptions.TooShortLoginException
import com.example.auth.exceptions.TooShortPasswordException
import com.example.auth.exceptions.UserAlreadyExistsException
import com.example.security.JwtService
import com.example.security.PasswordHasher
import com.example.user.User
import com.example.user.UserRepository
import com.example.util.AuthConstraints
import io.ktor.server.sessions.TooLateSessionSetException
import kotlin.math.log

class AuthService(private val userRepository: UserRepository) {

    fun login(login:String, password:String ): String {
        val user = userRepository.findByLogin(login)?: throw InvalidCredentialsException()

        if (!PasswordHasher.verify(password,user.passwordHash)){
            throw InvalidCredentialsException()
        }

        return JwtService.generateToken(user)
    }

    fun register(login: String, password: String) {


        if (login.isBlank() || password.isBlank()) {
            throw EmptyFieldException()
        }


        if (login.length < AuthConstraints.MIN_LOGIN_LENGTH) {
            throw TooShortLoginException()
        }

        if (login.length > AuthConstraints.MAX_LOGIN_LENGTH) {
            throw TooManyCharactersInLoginException()
        }

        if (password.length < AuthConstraints.MIN_PASSWORD_LENGTH) {
            throw TooShortPasswordException()
        }

        if (password.length > AuthConstraints.MAX_PASSWORD_LENGTH) {
            throw TooManyCharactersInPasswordException()
        }


        if (userRepository.findByLogin(login) != null) {
            throw UserAlreadyExistsException()
        }


        val hash = PasswordHasher.hash(password)
        userRepository.save(User(login = login, passwordHash = hash))
    }

}