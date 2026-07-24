package softwarescobedo.com.mx.app_1.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : Screen(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home
    )

    object Pago : Screen(
        route = "pago",
        title = "Pago",
        icon = Icons.Default.ShoppingCart
    )

    object Historial : Screen(
        route = "historial",
        title = "Historial",
        icon = Icons.AutoMirrored.Filled.List
    )

    object Ajustes : Screen(
        route = "ajustes",
        title = "Ajustes",
        icon = Icons.Default.Settings
    )

    companion object {
        val items: List<Screen>
            get() = listOf(Home, Pago, Historial, Ajustes)
    }
}
