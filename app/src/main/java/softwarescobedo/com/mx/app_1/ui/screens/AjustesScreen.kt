package softwarescobedo.com.mx.app_1.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import softwarescobedo.com.mx.app_1.data.AppSettings
import softwarescobedo.com.mx.app_1.data.SettingsRepository

@Composable
fun AjustesScreen(
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    var urlBase by remember { mutableStateOf("https://miwispro.net/") }
    var subdominio by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }

    var isTokenVisible by remember { mutableStateOf(false) }
    var isDataSaved by remember { mutableStateOf(false) }

    // READ: Cargar la configuración almacenada al iniciar
    fun loadSettings() {
        val settings = repository.getSettings()
        urlBase = settings.urlBase
        subdominio = settings.subdominio
        token = settings.token
        userId = settings.userId
        isDataSaved = repository.hasSettings()
    }

    LaunchedEffect(Unit) {
        loadSettings()
    }

    // Limpia espacios en blanco de un string
    fun cleanSpaces(text: String): String = text.replace("\\s+".toRegex(), "")

    // CREATE / UPDATE: Guardar datos en almacenamiento local (sin espacios)
    fun handleSave() {
        // Eliminar espacios de todos los campos
        val cleanUrl = cleanSpaces(urlBase)
        val cleanSubdomain = cleanSpaces(subdominio)
        val cleanTok = cleanSpaces(token)
        val cleanUser = cleanSpaces(userId)

        // Si el usuario ingresó una URL sin esquema (sin http:// ni https://), inferir protocolo
        val formattedUrl = if (cleanUrl.isNotEmpty() && !cleanUrl.startsWith("http://", ignoreCase = true) && !cleanUrl.startsWith("https://", ignoreCase = true)) {
            val isIpOrLocal = cleanUrl.matches(Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(?::\\d+)?(?:/.*)?$")) || cleanUrl.startsWith("localhost", ignoreCase = true)
            if (isIpOrLocal) "http://$cleanUrl" else "https://$cleanUrl"
        } else {
            cleanUrl
        }

        val settings = AppSettings(
            urlBase = if (formattedUrl.isEmpty()) "https://miwispro.net/" else formattedUrl,
            subdominio = cleanSubdomain,
            token = cleanTok,
            userId = cleanUser
        )

        val success = repository.saveSettings(settings)
        if (success) {
            // Actualizar inputs visibles con los valores sin espacios
            urlBase = settings.urlBase
            subdominio = settings.subdominio
            token = settings.token
            userId = settings.userId
            isDataSaved = repository.hasSettings()
            
            scope.launch {
                snackbarHostState.showSnackbar("Configuración guardada (espacios eliminados).")
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Error al guardar la configuración.")
            }
        }
    }

    // DELETE: Eliminar datos locales
    fun handleDelete() {
        val success = repository.deleteSettings()
        if (success) {
            urlBase = "https://miwispro.net/"
            subdominio = ""
            token = ""
            userId = ""
            isDataSaved = false
            scope.launch {
                snackbarHostState.showSnackbar("Configuración local eliminada.")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Parámetros de Conexión",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // 1. Input: URL Base (Soporta HTTP y HTTPS)
        OutlinedTextField(
            value = urlBase,
            onValueChange = { urlBase = it },
            label = { Text("1.- URL Base (HTTP / HTTPS)") },
            placeholder = { Text("Ej. http://187.77.203.50/miwispro/ o https://miwispro.net/") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // 2. Input: Subdominio
        OutlinedTextField(
            value = subdominio,
            onValueChange = { subdominio = it },
            label = { Text("2.- Subdominio") },
            placeholder = { Text("Ej. miempresa") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // 3. Input: API Token
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("3.- API Token") },
            placeholder = { Text("Ingrese el Token de la API") },
            singleLine = true,
            visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                    Icon(
                        imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Conmutar visibilidad del token"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // 4. Input: ID Usuario
        OutlinedTextField(
            value = userId,
            onValueChange = { userId = it },
            label = { Text("4.- ID usuario") },
            placeholder = { Text("Ej. 1024") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Acciones CRUD (Guardar / Actualizar, Recargar, Eliminar)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Botón Guardar / Actualizar (CREATE / UPDATE)
            Button(
                onClick = { handleSave() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isDataSaved) "Actualizar" else "Guardar")
            }

            // Botón Recargar (READ)
            OutlinedButton(
                onClick = {
                    loadSettings()
                    scope.launch {
                        snackbarHostState.showSnackbar("Datos recargados desde la BD local.")
                    }
                }
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Recargar")
            }

            // Botón Eliminar / Limpiar (DELETE)
            Button(
                onClick = { handleDelete() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Borrar")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "© 2026 MiWISPro | Software Escobedo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
