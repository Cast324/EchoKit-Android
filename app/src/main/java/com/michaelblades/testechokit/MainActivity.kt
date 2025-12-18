package com.michaelblades.testechokit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.michaelblades.testechokit.client.EchoKitClient
import com.michaelblades.testechokit.ui.theme.TestEchoKitTheme

class MainActivity : ComponentActivity() {

    private lateinit var client: EchoKitClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        client = EchoKitClient(
            context = applicationContext,
            baseURL = "http://192.168.1.32:8080/",
            apiKey = "***REMOVED***"
        )
        setContent {
            val viewModel = remember {
                IdeasViewModel(client)
            }
            IdeasListScreen(viewModel = viewModel)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TestEchoKitTheme {
        Greeting("Android")
    }
}