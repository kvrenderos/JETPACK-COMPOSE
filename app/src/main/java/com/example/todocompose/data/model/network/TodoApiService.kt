package com.example.todocompose.data.model.network

import com.example.todocompose.data.model.Todo
import com.example.todocompose.data.model.TodoRequest
import retrofit2.Response
import retrofit2.http.*

interface TodoApiService {
    // Sin / inicial: conserva academic/v1/ de la URL base.
    @GET("to-do")
    suspend fun getTodos(): List<Todo>

    @POST("to-do")
    suspend fun addTodo(@Body todo: TodoRequest): Todo

    @PUT("to-do/{id}")
    suspend fun updateTodo(@Path("id") id: String, @Body todo: TodoRequest): Todo

    @DELETE("to-do/{id}")
    suspend fun deleteTodo(@Path("id") id: String): Response<Unit>
}
