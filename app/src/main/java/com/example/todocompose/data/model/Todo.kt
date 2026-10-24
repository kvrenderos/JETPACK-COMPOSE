package com.example.todocompose.data.model

data class Todo(
    val id: String,
    val title: String,
    val description: String,
    val done: Boolean
)

// El servidor asigna el ID al crear el registro.
data class TodoRequest(val title: String, val description: String, val done: Boolean = false)
