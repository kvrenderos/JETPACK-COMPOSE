package com.example.todocompose

import com.example.todocompose.data.model.Todo
import com.example.todocompose.data.model.TodoRequest
import com.example.todocompose.data.model.network.TodoApiService
import com.example.todocompose.repository.OwnedTodoStore
import com.example.todocompose.repository.TodoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

private class MemoryStore : OwnedTodoStore {
    var ids = emptySet<String>()
    override fun read() = ids
    override fun write(ids: Set<String>) { this.ids = ids }
}

@OptIn(ExperimentalCoroutinesApi::class)
class Guide12Test {
    @Test fun retrofitCrudPreservesBasePathAndServerId() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            val api = Retrofit.Builder().baseUrl(server.url("/academic/v1/"))
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(TodoApiService::class.java)
            val store = MemoryStore()
            val repo = TodoRepository(api, store)
            val json = """{"id":"42","title":"Prueba","description":"Detalle","done":false}"""
            server.enqueue(MockResponse().setBody("[$json]"))
            assertEquals("42", repo.fetchTodos().single().id)
            assertEquals("/academic/v1/to-do", server.takeRequest().path)
            server.enqueue(MockResponse().setBody(json))
            val created = repo.addTodo("Prueba", "Detalle")
            val post = server.takeRequest()
            assertEquals("POST", post.method)
            assertFalse(post.body.readUtf8().contains("\"id\""))
            assertTrue("42" in store.ids)
            server.enqueue(MockResponse().setBody(json.replace("false", "true")))
            assertTrue(repo.updateTodo(created.copy(done = true)).done)
            val put = server.takeRequest()
            assertEquals("PUT", put.method)
            assertEquals("/academic/v1/to-do/42", put.path)
            assertTrue(put.body.readUtf8().contains("\"done\":true"))
            server.enqueue(MockResponse().setResponseCode(204))
            repo.deleteTodo("42")
            assertEquals("DELETE", server.takeRequest().method)
            assertTrue(store.ids.isEmpty())
        } finally { server.shutdown() }
    }

    @Test fun otherUsersRecordsCannotBeModifiedOrDeleted() = runTest {
        val fake = FakeApi()
        val repo = TodoRepository(fake, MemoryStore())
        try { repo.updateTodo(fake.todo); fail("Se permitió modificar una tarea ajena") }
        catch (_: IllegalArgumentException) { }
        try { repo.deleteTodo(fake.todo.id); fail("Se permitió eliminar una tarea ajena") }
        catch (_: IllegalArgumentException) { }
        assertEquals(0, fake.mutations)
    }

    @Test fun failedDeleteKeepsOwnership() = runTest {
        val store = MemoryStore().apply { ids = setOf("42") }
        val repo = TodoRepository(FakeApi().apply { failDelete = true }, store)
        try { repo.deleteTodo("42"); fail("Se esperaba HTTP 500") }
        catch (_: HttpException) { }
        assertEquals(setOf("42"), store.ids)
    }

    @Test fun viewModelCrudAndFailureKeepExistingData() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val api = FakeApi()
            val store = MemoryStore()
            val vm = TodoViewModel(TodoRepository(api, store))
            advanceUntilIdle()
            assertEquals(1, vm.state.value.todos.size)
            vm.addTodo("   ", "")
            advanceUntilIdle()
            assertNotNull(vm.state.value.error)
            assertEquals(0, api.mutations)
            vm.addTodo("  Nueva  ", "  Detalle  ")
            advanceUntilIdle()
            val created = vm.state.value.todos.last()
            assertEquals("Nueva", created.title)
            assertEquals(2, vm.state.value.todos.size)
            vm.toggleDone(created)
            advanceUntilIdle()
            assertTrue(vm.state.value.todos.last().done)
            vm.updateTodo(vm.state.value.todos.last(), "Editada", "Otro detalle")
            advanceUntilIdle()
            assertEquals("Editada", vm.state.value.todos.last().title)
            api.failAdd = true
            vm.addTodo("No se guarda", "")
            advanceUntilIdle()
            assertEquals(2, vm.state.value.todos.size)
            assertNotNull(vm.state.value.error)
            assertFalse(vm.state.value.loading)
            vm.deleteTodo(vm.state.value.todos.last())
            advanceUntilIdle()
            assertEquals(listOf(api.todo), vm.state.value.todos)
            assertTrue(vm.state.value.ownedIds.isEmpty())
        } finally { Dispatchers.resetMain() }
    }

    private class FakeApi : TodoApiService {
        val todo = Todo("shared", "Compartida", "No tocar", false)
        var mutations = 0
        var failAdd = false
        var failDelete = false
        override suspend fun getTodos() = listOf(todo)
        override suspend fun addTodo(todo: TodoRequest): Todo {
            mutations++
            if (failAdd) throw IOException("Sin red")
            return Todo("42", todo.title, todo.description, todo.done)
        }
        override suspend fun updateTodo(id: String, todo: TodoRequest): Todo {
            mutations++
            return Todo(id, todo.title, todo.description, todo.done)
        }
        override suspend fun deleteTodo(id: String): Response<Unit> {
            mutations++
            return if (failDelete) Response.error(500, okhttp3.ResponseBody.create(null, "error"))
                else Response.success(Unit)
        }
    }
}
