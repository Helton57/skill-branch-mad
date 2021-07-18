package ru.skillbranch.skillarticles.data.adapters

interface JsonAdapter<T> {
    fun fromJson(json: String): T?
    fun toJson(obj: T?): String
}