package ru.skillbranch.kotlinexample.extensions

fun String?.checkPhoneNumber(): Boolean {
    if (this.isNullOrBlank()) return false
    return if (this.getCleanPhone()?.length == 12) true
    else throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
}

fun String?.getCleanPhone(): String? {
    return this?.replace("""[^+\d]""".toRegex(), "")
}

fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T> {
    var result = listOf<T>()
    if (this.isNotEmpty()) {
        val lastIndex = this.indexOfLast(predicate)
        if (lastIndex != -1) {
            result = this.subList(0, lastIndex)
        }
    }
    return result
}