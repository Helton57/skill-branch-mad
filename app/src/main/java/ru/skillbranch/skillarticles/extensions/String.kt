package ru.skillbranch.skillarticles.extensions

fun String?.indexesOf(
    substr: String,
    ignoreCase: Boolean = true
): List<Int> {
    return if (this.isNullOrBlank() || substr == "") {
        emptyList()
    } else {
        //second variant
        var startIndex = -1
        val resultList = mutableListOf<Int>()
        do {
            startIndex = this.indexOf(substr, startIndex + 1, ignoreCase = ignoreCase)
            if (startIndex != -1) {
                resultList.add(startIndex)
            }
        } while (startIndex != -1)
        resultList
    }
}