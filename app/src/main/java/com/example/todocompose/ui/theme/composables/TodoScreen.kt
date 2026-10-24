package com.example.todocompose.ui.theme.composables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todocompose.TodoViewModel
import com.example.todocompose.data.model.Todo

@Composable
fun TodoScreen(viewModel: TodoViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    var editTitle by rememberSaveable { mutableStateOf("") }
    var editDescription by rememberSaveable { mutableStateOf("") }
    var pendingSave by rememberSaveable { mutableStateOf(false) }
    var submittedCount by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(state.savedCount, state.loading) {
        if (pendingSave && !state.loading) {
            if (state.savedCount > submittedCount) {
                if (editingId != null) editingId = null
                else { title = ""; description = "" }
            }
            pendingSave = false
        }
    }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Mis Tareas", style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 16.dp))
            Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") },
                    singleLine = true, enabled = !state.loading, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it },
                    label = { Text("Descripción") }, maxLines = 3, enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    submittedCount = state.savedCount
                    pendingSave = true
                    viewModel.addTodo(title, description)
                }, enabled = title.isNotBlank() && !state.loading, modifier = Modifier.fillMaxWidth()) {
                    Text("Agregar")
                }
            }
            state.error?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${state.todos.size} tareas", modifier = Modifier.padding(top = 12.dp))
                TextButton(onClick = { viewModel.loadTodos() }, enabled = !state.loading) { Text("Actualizar") }
            }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.todos.isEmpty() && !state.loading) {
                Text(if (state.error != null) "No se han cargado tareas." else "Aún no hay tareas. Agrega la primera.")
            }
            TodoList(state.todos, state.ownedIds, !state.loading,
                onToggle = viewModel::toggleDone,
                onEdit = { todo ->
                    viewModel.clearError()
                    editingId = todo.id; editTitle = todo.title; editDescription = todo.description
                }, onDelete = { deletingId = it.id }, modifier = Modifier.weight(1f))
        }
    }

    val editing: Todo? = state.todos.find { it.id == editingId }
    if (editing != null) AlertDialog(
        onDismissRequest = { if (!state.loading) editingId = null },
        title = { Text("Modificar tarea") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(editTitle, { editTitle = it }, label = { Text("Título") },
                    enabled = !state.loading, singleLine = true)
                OutlinedTextField(editDescription, { editDescription = it }, label = { Text("Descripción") },
                    enabled = !state.loading, maxLines = 4)
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                submittedCount = state.savedCount; pendingSave = true
                viewModel.updateTodo(editing, editTitle, editDescription)
            }, enabled = editTitle.isNotBlank() && !state.loading) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = { editingId = null }, enabled = !state.loading) { Text("Cancelar") } }
    )
    val deleting = state.todos.find { it.id == deletingId }
    if (deleting != null) AlertDialog(
        onDismissRequest = { deletingId = null }, title = { Text("Eliminar tarea") },
        text = { Text("¿Eliminar «${deleting.title}»?") },
        confirmButton = {
            TextButton(onClick = { viewModel.deleteTodo(deleting); deletingId = null }, enabled = !state.loading) {
                Text("Eliminar")
            }
        },
        dismissButton = { TextButton(onClick = { deletingId = null }) { Text("Cancelar") } }
    )
}
