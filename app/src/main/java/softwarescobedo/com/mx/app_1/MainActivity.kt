package softwarescobedo.com.mx.app_1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import softwarescobedo.com.mx.app_1.navigation.Screen
import softwarescobedo.com.mx.app_1.ui.components.AppBottomBar
import softwarescobedo.com.mx.app_1.ui.screens.AjustesScreen
import softwarescobedo.com.mx.app_1.ui.screens.HistorialScreen
import softwarescobedo.com.mx.app_1.ui.screens.HomeScreen
import softwarescobedo.com.mx.app_1.ui.screens.PagoScreen
import softwarescobedo.com.mx.app_1.ui.theme.APP_1Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            APP_1Theme {
                MainAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentScreen.title,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            AppBottomBar(
                currentScreen = currentScreen,
                onScreenSelected = { screen ->
                    currentScreen = screen
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Modifier.padding(innerPadding)
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (currentScreen) {
            Screen.Home -> androidx.compose.foundation.layout.Box(modifier = contentModifier) { HomeScreen() }
            Screen.Pago -> androidx.compose.foundation.layout.Box(modifier = contentModifier) { PagoScreen() }
            Screen.Historial -> androidx.compose.foundation.layout.Box(modifier = contentModifier) { HistorialScreen() }
            Screen.Ajustes -> androidx.compose.foundation.layout.Box(modifier = contentModifier) { AjustesScreen(snackbarHostState = snackbarHostState) }
        }
    }
}