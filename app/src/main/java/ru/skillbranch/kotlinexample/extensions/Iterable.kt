package ru.skillbranch.kotlinexample.extensions

fun String?.checkPhoneNumber(): Boolean {
    if (this.isNullOrBlank()) return false
    val result = this.replace("""[^+\d]""".toRegex(), "")
    return if (result.length == 12) true
    else throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
}

fun String?.getCleanPhone(): String? {
    return this?.replace("""[^+\d]""".toRegex(), "")
}