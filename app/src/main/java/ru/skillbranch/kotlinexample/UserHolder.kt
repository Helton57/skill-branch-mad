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
            map.addUserToMap(user, "email")
        }

    fun registerUserByPhone(fullName: String, rawPhone: String): User {
        rawPhone.checkPhoneNumber()
        return User.makeUser(fullName = fullName, phone = rawPhone)
            .also { user ->
                map.addUserToMap(user, "phone")
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

    fun importUsers(list: List<String>): List<User> {
        list.filter { it.isNotBlank() }.map { importedString ->
            val importedFields = importedString.split(";")
            val fullName = importedFields.getBlankOrNullValue(0)
            val email = importedFields.getBlankOrNullValue(1)
            val saltWithHash = importedFields.getBlankOrNullValue(2)?.split(":")
            val phone = importedFields.getBlankOrNullValue(3)
            val salt = saltWithHash?.getBlankOrNullValue(0)
            val hash = saltWithHash?.getBlankOrNullValue(1)

            User.importUser(fullName, email, phone, salt, hash) { user ->
                map.addUserToMap(
                    user,
                    "imported data"
                )
            }
        }
        val usersList = mutableListOf<User>()
        map.map { item -> usersList.add(item.value) }
        return usersList
    }

    private fun List<String>.getBlankOrNullValue(index: Int): String? {
        val result = getOrNull(index)
        return if (result.isNullOrBlank()) null
        else result.trim()
    }

    private fun getUserFromMap(login: String): User? =
        map[login.trim().toLowerCase()] ?: map[login.getCleanPhone()]

    private fun Map<String, User>.addUserToMap(user: User, typeOfLogin: String) {
        if (containsKey(user.login)) {
            throw IllegalArgumentException("A user with this $typeOfLogin already exists")
        } else {
            map[user.login] = user
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }
}