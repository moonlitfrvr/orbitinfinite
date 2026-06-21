package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.OrbitRepository
import com.example.ui.OrbitApp
import com.example.ui.theme.OrbitTheme
import com.example.viewmodel.OrbitViewModel
import com.example.viewmodel.OrbitViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room Database, Repo and ViewModel
        val database = AppDatabase.getDatabase(this)
        val repository = OrbitRepository(database)
        val viewModelFactory = OrbitViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[OrbitViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            OrbitTheme {
                OrbitApp(viewModel = viewModel)
            }
        }
    }
}
