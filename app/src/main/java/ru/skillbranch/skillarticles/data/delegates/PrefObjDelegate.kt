package ru.skillbranch.skillarticles.data.delegates

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.skillbranch.skillarticles.data.PrefManager
import ru.skillbranch.skillarticles.data.adapters.JsonAdapter
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PrefObjDelegate<T>(
    private val adapter: JsonAdapter<T>,
    private val customKey: String? = null
) {
    operator fun provideDelegate(
        thisRef: PrefManager,
        prop: KProperty<*>
    ): ReadWriteProperty<PrefManager, T?> {
        return object : ReadWriteProperty<PrefManager, T?> {
            var storedValue: T? = null

            val key = stringPreferencesKey(customKey ?: prop.name)

            override fun getValue(thisRef: PrefManager, property: KProperty<*>): T? {
                if (storedValue == null) {
                    val flowValue = thisRef.dataStore.data.map { preferences ->
                        preferences[key] ?: ""
                    }
                    storedValue = runBlocking {
                        flowValue
                            .map { adapter.fromJson(it) }
                            .first()
                    }
                }
                return storedValue
            }

            override fun setValue(thisRef: PrefManager, property: KProperty<*>, value: T?) {
                storedValue = value
                @Suppress("UNCHECKED_CAST")
                thisRef.scope.launch {
                    thisRef.dataStore.edit { settings ->
                        settings[key] = adapter.toJson(value)
                    }
                }
            }
        }
    }
}