package com.example.todocompose.ui.theme.composables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.todocompose.data.model.Todo
import com.example.todocompose.ui.theme.TodoComposeTheme

@Composable
fun TodoList(
    todos: List<Todo>, ownedIds: Set<String> = emptySet(), enabled: Boolean = true,
    onToggle: (Todo) -> Unit = {}, onEdit: (Todo) -> Unit = {},
    onDelete: (Todo) -> Unit = {}, modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)) {
        items(todos, key = { it.id }) { todo ->
            TodoItem(todo, todo.id in ownedIds, enabled, onToggle, onEdit, onDelete)
        }
    }
}

@Composable
fun TodoItem(
    todo: Todo, owned: Boolean = false, enabled: Boolean = true,
    onToggle: (Todo) -> Unit = {}, onEdit: (Todo) -> Unit = {}, onDelete: (Todo) -> Unit = {}
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("ID: ${todo.id}", style = MaterialTheme.typography.labelSmall)
            Text(todo.title, style = MaterialTheme.typography.titleMedium)
            Text(todo.description.ifBlank { "Sin descripción" }, style = MaterialTheme.typography.bodyMedium)
            Text(if (todo.done) "Completada" else "Pendiente",
                color = if (todo.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            if (owned) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { onToggle(todo) }, enabled = enabled) {
                        Text(if (todo.done) "Reabrir" else "Completar")
                    }
                    TextButton(onClick = { onEdit(todo) }, enabled = enabled) { Text("Editar") }
                    TextButton(onClick = { onDelete(todo) }, enabled = enabled) { Text("Eliminar") }
                }
            } else {
                Text("Solo lectura · Creada por otro usuario", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TodoPreviewScreen() {
    TodoComposeTheme {
        TodoList(listOf(
            Todo("1", "Comprar leche", "Ir al supermercado antes de las 6 PM.", false),
            Todo("2", "Estudiar Kotlin", "Repasar funciones de alto orden y lambdas.", true),
            Todo("3", "Hacer ejercicio", "Salir a correr al parque por 30 minutos.", false)
        ), ownedIds = setOf("1", "2", "3"), modifier = Modifier.padding(16.dp))
    }
}
