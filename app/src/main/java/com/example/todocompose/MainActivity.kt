package com.example.todocompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.todocompose.data.model.network.TodoApiService
import com.example.todocompose.repository.PreferencesOwnedTodoStore
import com.example.todocompose.repository.TodoRepository
import com.example.todocompose.ui.theme.TodoComposeTheme
import com.example.todocompose.ui.theme.composables.TodoScreen
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val factory = viewModelFactory {
            initializer {
                val api = Retrofit.Builder()
                    .baseUrl(BuildConfig.TODO_API_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(TodoApiService::class.java)
                val store = PreferencesOwnedTodoStore(
                    applicationContext.getSharedPreferences("todo_ownership", MODE_PRIVATE)
                )
                TodoViewModel(TodoRepository(api, store))
            }
        }
        val viewModel = ViewModelProvider(this, factory)[TodoViewModel::class.java]
        setContent { TodoComposeTheme { TodoScreen(viewModel) } }
    }
}
