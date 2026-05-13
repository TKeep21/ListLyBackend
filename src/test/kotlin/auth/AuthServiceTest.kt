package auth

import com.example.auth.AuthService
import com.example.auth.exceptions.TooManyCharactersInLoginException
import com.example.auth.exceptions.TooManyCharactersInPasswordException
import com.example.auth.exceptions.TooShortLoginException
import com.example.auth.exceptions.TooShortPasswordException
import com.example.auth.exceptions.UserAlreadyExistsException
import com.example.user.User
import com.example.user.UserRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.math.log
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class AuthServiceTest {
    private val repository = mockk<UserRepository>()
    private val service = AuthService(repository)

    @Test
    fun `register throws UserAlreadyExistsException if login already exists`()
    {
        val login = "login1"
        val user = User(login=login, passwordHash = "239344")

        every{repository.findByLogin(login)} returns user

        assertFailsWith<UserAlreadyExistsException> {
            service.register(user.login,user.passwordHash)
        }

        verify(exactly = 0) { repository.save(any()) }

    }

    @Test
    fun `register throws exception if login is more than 20 characters`(){
        val login = "a".repeat(21)
        val user = User(login = login, passwordHash = "23445")

        assertFailsWith<TooManyCharactersInLoginException> {
            service.register(login,user.passwordHash)
        }

        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `register throws exception if password is more than 25 characters`(){
        val password = "a".repeat(26)
        val user = User(login = "login", passwordHash = password)

        assertFailsWith<TooManyCharactersInPasswordException> {
            service.register(user.login,password)
        }

        verify(exactly = 0) { repository.save(any()) }
    }


    @Test
    fun  `register throws exception if login is less than 3 characters`(){
        val login = "dc"
        val user = User(login = login, passwordHash = "1234")

        assertFailsWith<TooShortLoginException> {
            service.register(login,user.passwordHash)
        }

        verify(exactly = 0) { repository.save(any()) }
    }
    @Test
    fun `register throws exception if password is less than 6 characters`(){
        val password = "a".repeat(5)
        val user = User(login = "login", passwordHash = password)

        assertFailsWith<TooShortPasswordException> {
            service.register(user.login,password)
        }

        verify(exactly = 0) { repository.save(any()) }
    }







    @Test
    fun `successful registration`(){
        val login = "login1"
        val user = User(login = login, passwordHash = "2344457")

        every { repository.findByLogin(login)} returns null

        every { repository.save(any()) } just Runs

        service.register(login,user.passwordHash)

        verify(exactly = 1) { repository.save(any()) }

    }
}