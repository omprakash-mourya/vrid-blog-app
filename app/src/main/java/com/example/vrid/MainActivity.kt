package com.example.vrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.vrid.repository.BlogRepository
import com.example.vrid.ui.BlogDetailScreen
import com.example.vrid.ui.BlogListScreen
import com.example.vrid.ui.BlogViewModel
import com.example.vrid.ui.theme.VridTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController: NavHostController = rememberNavController()
            val blogViewModel: BlogViewModel = viewModel {
                BlogViewModel(BlogRepository(applicationContext))
            }

            VridTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "blogList",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("blogList") {
                            BlogListScreen(navController, blogViewModel)
                        }
                        composable("blogDetail/{id}") { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("id") ?: ""
                            BlogDetailScreen(id, blogViewModel, navController)
                        }
                    }
                }
            }
        }
    }
}

