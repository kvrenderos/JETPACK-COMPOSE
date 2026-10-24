package com.example.todocompose.repository

import android.content.SharedPreferences

interface OwnedTodoStore {
    fun read(): Set<String>
    fun write(ids: Set<String>)
}

// Identifica únicamente los registros creados por esta instalación.
class PreferencesOwnedTodoStore(private val preferences: SharedPreferences) : OwnedTodoStore {
    override fun read(): Set<String> = preferences.getStringSet("owned_ids", emptySet())!!.toSet()
    override fun write(ids: Set<String>) {
        preferences.edit().putStringSet("owned_ids", ids.toSet()).apply()
    }
}
