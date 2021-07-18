package ru.skillbranch.skillarticles.data.adapters

import ru.skillbranch.skillarticles.data.local.User

class UserJsonAdapter : JsonAdapter<User> {
    override fun fromJson(json: String): User? {
        // FIXME: 18.07.2021 add solution
        return null
    }

    override fun toJson(obj: User?): String {
        // FIXME: 18.07.2021 add solution
        return ""
    }
}