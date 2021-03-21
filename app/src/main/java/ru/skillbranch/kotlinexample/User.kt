package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import ru.skillbranch.kotlinexample.extensions.getCleanPhone
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {
    val userInfo: String

    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .capitalize()

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().toUpperCase() }
            .joinToString(" ")

    private var phone: String? = null
        set(value) {
            field = value?.getCleanPhone()
        }

    private var _login: String? = null
    var login: String
        set(value) {
            _login = value.toLowerCase()
        }
        get() = _login!!

    private var salt: String? = null

    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    /**
     * for email
     */
    constructor(
        firstName: String,
        lastName: String?,
        email: String,
        password: String
    ) : this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        println("Secondary email constructor")
        passwordHash = encrypt(password)
    }

    /**
     * for phone
     */
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String
    ) : this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")) {
        println("Secondary phone constructor")
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        println("Phone passwordHash is $passwordHash")
        accessCode = code
        sendAccessCodeToUser(rawPhone, code)
    }

    /**
     * for import
     */
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        phone: String?,
        passwordHash: String,
        salt: String,
        meta: Map<String, Any>? = mapOf("src" to "csv")
    ) : this(firstName, lastName, email, phone, meta) {
        this.passwordHash = passwordHash
        this.salt = salt
    }

    init {
        println("First init block, primary constructor was called")
        check(firstName.isNotBlank()) { "FirstName must not be blank" }
        check(!email.isNullOrBlank() || !rawPhone.isNullOrBlank()) { "Email or Phone must not be null or blank" }

        phone = rawPhone
        login = email ?: phone!!

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()
    }

    fun checkPassword(password: String) = encrypt(password) == passwordHash.also {
        println("Checking passwordHash is $passwordHash")
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        if (checkPassword(oldPassword)) {
            passwordHash = encrypt(newPassword)
            if (!accessCode.isNullOrEmpty()) accessCode = newPassword
            println("Password $oldPassword has been changed on new password $newPassword")
        } else {
            throw IllegalArgumentException("The entered password does not match the current password")
        }
    }

    private fun encrypt(password: String): String {
        if (salt.isNullOrEmpty()) {
            salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
        }

        println("Salt while encrypt: $salt")
        return salt.plus(password).md5()
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray()) //16 byte
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    private fun sendAccessCodeToUser(phone: String, code: String) {
        println("..... sending access code: $code on $phone")
    }

    companion object Factory {
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()

            return when {
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(
                    firstName,
                    lastName,
                    email,
                    password
                )
                else -> throw IllegalArgumentException("Email or phone must not be null or blank")
            }
        }

        fun importUser(
            fullName: String?,
            email: String? = null,
            phone: String? = null,
            salt: String? = null,
            passwordHash: String? = null,
            result: (User) -> Unit
        ) {
            if (fullName.isNullOrBlank()) throw throw IllegalArgumentException("FullName is null or empty")
            if (salt.isNullOrBlank()) throw throw IllegalArgumentException("salt is null or empty")
            if (passwordHash.isNullOrBlank()) throw throw IllegalArgumentException("passwordHash is null or empty")

            val (firstName, lastName) = fullName.fullNameToPair()
            result(
                User(
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    phone = phone,
                    salt = salt,
                    passwordHash = passwordHash
                )
            )
        }

        private fun String.fullNameToPair(): Pair<String, String?> =
            this.split(" ")
                .filter { it.isNotBlank() }
                .run {
                    when (size) {
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException(
                            "FullName must contain only first name and last name, current split " +
                                    "result = ${this@fullNameToPair}"
                        )
                    }
                }
    }
}