package com.example.todocompose.repository

import com.example.todocompose.data.model.Todo
import com.example.todocompose.data.model.TodoRequest
import com.example.todocompose.data.model.network.TodoApiService
import retrofit2.HttpException

class TodoRepository(private val api: TodoApiService, private val ownedStore: OwnedTodoStore) {
    fun ownedIds(): Set<String> = ownedStore.read()
    suspend fun fetchTodos(): List<Todo> = api.getTodos()

    suspend fun addTodo(title: String, description: String): Todo {
        val result = api.addTodo(TodoRequest(title, description))
        ownedStore.write(ownedStore.read() + result.id)
        return result
    }

    suspend fun updateTodo(todo: Todo): Todo {
        require(todo.id in ownedStore.read()) { "Solo puedes modificar tus propias tareas." }
        return api.updateTodo(todo.id, TodoRequest(todo.title, todo.description, todo.done))
    }

    suspend fun deleteTodo(id: String) {
        require(id in ownedStore.read()) { "Solo puedes eliminar tus propias tareas." }
        val response = api.deleteTodo(id)
        if (!response.isSuccessful) throw HttpException(response)
        ownedStore.write(ownedStore.read() - id)
    }
}
