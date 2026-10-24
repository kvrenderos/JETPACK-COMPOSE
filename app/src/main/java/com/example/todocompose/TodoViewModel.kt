package com.example.todocompose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todocompose.data.model.Todo
import com.example.todocompose.repository.TodoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class TodoUiState(
    val todos: List<Todo> = emptyList(),
    val ownedIds: Set<String> = emptySet(),
    val loading: Boolean = false,
    val error: String? = null,
    val savedCount: Int = 0
)

class TodoViewModel(private val repository: TodoRepository) : ViewModel() {
    private val _state = MutableStateFlow(TodoUiState(ownedIds = repository.ownedIds()))
    val state = _state.asStateFlow()

    init { loadTodos() }

    fun clearError() { _state.value = _state.value.copy(error = null) }

    fun loadTodos() = perform {
        val todos = repository.fetchTodos()
        _state.value = _state.value.copy(todos = todos, ownedIds = repository.ownedIds())
    }

    fun addTodo(title: String, description: String) {
        if (!validTitle(title)) return
        perform {
            val todo = repository.addTodo(title.trim(), description.trim())
            _state.value = _state.value.copy(
                todos = _state.value.todos + todo,
                ownedIds = repository.ownedIds(), savedCount = _state.value.savedCount + 1
            )
        }
    }

    fun updateTodo(todo: Todo, title: String, description: String) {
        if (!validTitle(title)) return
        change(todo.copy(title = title.trim(), description = description.trim()))
    }

    fun toggleDone(todo: Todo) = change(todo.copy(done = !todo.done))

    private fun change(todo: Todo) = perform {
        val updated = repository.updateTodo(todo)
        _state.value = _state.value.copy(todos = _state.value.todos.map {
            if (it.id == updated.id) updated else it
        }, savedCount = _state.value.savedCount + 1)
    }

    fun deleteTodo(todo: Todo) = perform {
        repository.deleteTodo(todo.id)
        _state.value = _state.value.copy(
            todos = _state.value.todos.filterNot { it.id == todo.id },
            ownedIds = repository.ownedIds()
        )
    }

    private fun validTitle(title: String): Boolean {
        if (title.isNotBlank()) return true
        _state.value = _state.value.copy(error = "Escribe un título para la tarea.")
        return false
    }

    private fun perform(action: suspend () -> Unit) {
        if (_state.value.loading) return
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = when (e) {
                    is HttpException -> if (e.code() == 404)
                        "La API respondió 404. Revisa la URL del servicio en gradle.properties."
                        else "El servidor rechazó la operación (HTTP ${e.code()}). Intenta nuevamente."
                    is IOException -> "No se pudo conectar. Revisa tu conexión e intenta nuevamente."
                    is IllegalArgumentException -> e.message ?: "Operación no permitida."
                    else -> "No se pudo completar la operación. Intenta nuevamente."
                }
                _state.value = _state.value.copy(error = message)
            } finally {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }
}
