package com.michaelblades.testechokit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.michaelblades.echokit.EchoKitClient
import com.michaelblades.echokit.EchoKitTheme
import com.michaelblades.echokit.ui.IdeaDetailScreen
import com.michaelblades.echokit.ui.IdeaDetailViewModel
import com.michaelblades.echokit.ui.IdeasListScreen
import com.michaelblades.echokit.ui.IdeasViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Initialize EchoKit client
        val client = EchoKitClient(
            baseURL = "REPLACE_ME_BASE_URL",
            apiKey = "REPLACE_ME_API_KEY",
            context = applicationContext
        )

        setContent {
            EchoKitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EchoKitApp(client = client)
                }
            }
        }
    }
}

@Composable
fun EchoKitApp(client: EchoKitClient) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "ideas_list"
    ) {
        composable("ideas_list") {
            val viewModel: IdeasViewModel = viewModel(
                factory = IdeasViewModelFactory(client)
            )

            IdeasListScreen(
                viewModel = viewModel,
                onIdeaClick = { ideaId ->
                    navController.navigate("idea_detail/$ideaId")
                }
            )
        }

        composable(
            route = "idea_detail/{ideaId}",
            arguments = listOf(navArgument("ideaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ideaId = backStackEntry.arguments?.getString("ideaId") ?: return@composable
            val viewModel: IdeaDetailViewModel = viewModel(
                factory = IdeaDetailViewModelFactory(client, ideaId)
            )

            IdeaDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

// ViewModel Factories
class IdeasViewModelFactory(
    private val client: EchoKitClient
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IdeasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return IdeasViewModel(client) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class IdeaDetailViewModelFactory(
    private val client: EchoKitClient,
    private val ideaId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IdeaDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return IdeaDetailViewModel(client, ideaId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
