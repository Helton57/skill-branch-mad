package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import ru.skillbranch.kotlinexample.extensions.checkPhoneNumber
import ru.skillbranch.kotlinexample.extensions.getCleanPhone

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User = User.makeUser(fullName, email, password)
        .also { user ->
            if (map.containsKey(user.login)) {
                map.forEach { println(it) }
                throw IllegalArgumentException("A user with this email already exists")
            } else {
                map[user.login] = user
                map.forEach { println(it) }
            }
        }

    fun registerUserByPhone(fullName: String, rawPhone: String): User {
        rawPhone.checkPhoneNumber()
        return User.makeUser(fullName = fullName, phone = rawPhone)
            .also { user ->
                if (map.containsKey(user.login))
                    throw IllegalArgumentException("A user with this phone already exists")
                else
                    map[user.login] = user
            }
    }

    fun loginUser(login: String, password: String): String? =
        getUserFromMap(login)?.let {
            if (it.checkPassword(password)) it.userInfo
            else null
        }

    fun requestAccessCode(login: String) =
        getUserFromMap(login)?.let { user ->
            user.changePassword(user.accessCode!!, user.generateAccessCode())
        }

    private fun getUserFromMap(login: String): User? =
        map[login.trim()] ?: map[login.getCleanPhone()]

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }
}