package com.example.todocompose

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.todocompose.data.model.Todo
import com.example.todocompose.data.model.TodoRequest
import com.example.todocompose.data.model.network.TodoApiService
import com.example.todocompose.repository.OwnedTodoStore
import com.example.todocompose.repository.TodoRepository
import com.example.todocompose.ui.theme.TodoComposeTheme
import com.example.todocompose.ui.theme.composables.TodoScreen
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class TodoFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun createCompleteEditAndDeleteOwnTask() {
        val store = object : OwnedTodoStore {
            var ids = emptySet<String>()
            override fun read() = ids
            override fun write(ids: Set<String>) { this.ids = ids }
        }
        val api = object : TodoApiService {
            override suspend fun getTodos() = emptyList<Todo>()
            override suspend fun addTodo(todo: TodoRequest) = Todo("test-1", todo.title, todo.description, false)
            override suspend fun updateTodo(id: String, todo: TodoRequest) = Todo(id, todo.title, todo.description, todo.done)
            override suspend fun deleteTodo(id: String) = Response.success(Unit)
        }
        lateinit var vm: TodoViewModel
        compose.runOnUiThread { vm = TodoViewModel(TodoRepository(api, store)) }
        compose.setContent { TodoComposeTheme { TodoScreen(vm) } }
        compose.onNodeWithText("Título").performTextInput("Tarea de prueba")
        compose.onNodeWithText("Descripción").performTextInput("Descripción inicial")
        compose.onNodeWithText("Agregar").performClick()
        compose.onNodeWithText("Tarea de prueba").assertIsDisplayed()
        compose.onNodeWithText("Completar").performClick()
        compose.onNodeWithText("Completada").assertIsDisplayed()
        compose.onNodeWithText("Editar").performClick()
        compose.onAllNodesWithText("Título").onLast().performTextReplacement("Tarea modificada")
        compose.onNodeWithText("Guardar").performClick()
        compose.onNodeWithText("Tarea modificada").assertIsDisplayed()
        compose.onNodeWithText("Eliminar").performClick()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Tarea modificada").assertIsDisplayed()
        compose.onNodeWithText("Eliminar").performClick()
        compose.onAllNodesWithText("Eliminar").onLast().performClick()
        compose.onNodeWithText("Tarea modificada").assertDoesNotExist()
        compose.onNodeWithText("Aún no hay tareas. Agrega la primera.").assertIsDisplayed()
    }
}
