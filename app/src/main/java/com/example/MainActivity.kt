package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    
    enableEdgeToEdge()
    setContent {
      val isDark = when (viewModel.appTheme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
      }
      
      MyApplicationTheme(darkTheme = isDark) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          MainScreen(viewModel = viewModel)
        }
      }
    }
  }
}
