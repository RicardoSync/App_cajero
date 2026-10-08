package softwarescobedo.com.mx.app_1.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import softwarescobedo.com.mx.app_1.data.ClienteSearchResult
import softwarescobedo.com.mx.app_1.data.MetodoPago
import softwarescobedo.com.mx.app_1.data.ResultadoPagoData
import softwarescobedo.com.mx.app_1.data.SettingsRepository
import softwarescobedo.com.mx.app_1.network.ApiService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagoScreen() {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val apiService = remember { ApiService() }
    val scope = rememberCoroutineScope()

    // Búsqueda de clientes
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<ClienteSearchResult>>(emptyList()) }
    var selectedCliente by remember { mutableStateOf<ClienteSearchResult?>(null) }
    var clienteToPreview by remember { mutableStateOf<ClienteSearchResult?>(null) }

    // Métodos de pago
    var metodosPago by remember { mutableStateOf<List<MetodoPago>>(emptyList()) }
    var selectedMetodo by remember { mutableStateOf<MetodoPago?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Campos del formulario
    var montoInput by remember { mutableStateOf("") }
    var efectivoRecibidoInput by remember { mutableStateOf("") }
    var referenciaInput by remember { mutableStateOf("") }

    // Registro de pago
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resultadoExitoso by remember { mutableStateOf<ResultadoPagoData?>(null) }

    // Cálculo dinámico del cambio
    val montoDouble = montoInput.toDoubleOrNull() ?: 0.0
    val efectivoDouble = efectivoRecibidoInput.toDoubleOrNull() ?: 0.0
    val cambioCalculado = if (efectivoDouble >= montoDouble) efectivoDouble - montoDouble else 0.0
    val cambioFormateado = String.format(Locale.US, "%.2f", cambioCalculado)

    // Cargar métodos de pago al iniciar
    fun loadMetodosPago() {
        val settings = repository.getSettings()
        if (settings.token.isNotEmpty() && settings.subdominio.isNotEmpty()) {
            scope.launch {
                val result = apiService.getMetodosPago(settings)
                result.onSuccess { list ->
                    metodosPago = list
                    if (list.isNotEmpty()) {
                        selectedMetodo = list.first()
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadMetodosPago()
    }

    // Búsqueda en tiempo real de cliente con Debounce (350 ms)
    LaunchedEffect(searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }

        // Si ya hay un cliente seleccionado con ese nombre o ID, no repetir la búsqueda
        if (selectedCliente != null && (selectedCliente!!.nombreCompleto.equals(query, ignoreCase = true) || selectedCliente!!.id.toString() == query)) {
            return@LaunchedEffect
        }

        delay(350) // Esperar 350ms a que el usuario termine de teclear

        val settings = repository.getSettings()
        if (settings.token.isEmpty() || settings.subdominio.isEmpty()) return@LaunchedEffect

        isSearching = true
        errorMessage = null

        val result = apiService.buscarClientes(settings, query)
        isSearching = false
        result.onSuccess { list ->
            searchResults = list
            if (list.isEmpty()) {
                errorMessage = "No se encontraron clientes que coincidan con '$query'"
            }
        }.onFailure { ex ->
            searchResults = emptyList()
            errorMessage = ex.localizedMessage ?: "No se encontraron clientes"
        }
    }

    // Seleccionar cliente y pre-llenar monto
    fun selectCliente(cliente: ClienteSearchResult) {
        selectedCliente = cliente
        searchResults = emptyList()
        // Pre-llenar el monto con el precio mensual base o saldo positivo
        montoInput = if (cliente.precioMensualBase.isNotEmpty() && cliente.precioMensualBase != "0.00") {
            cliente.precioMensualBase
        } else {
            val s = cliente.saldoActual.replace("-", "").trim()
            if (s.isEmpty() || s == "0.00") "100.00" else s
        }

        // Generar referencia por defecto si está vacía
        if (referenciaInput.isEmpty()) {
            val timeStamp = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
            referenciaInput = "REC-$timeStamp"
        }
    }

    // Ejecutar registro de pago
    fun handleRegistrarPago() {
        val cliente = selectedCliente ?: run {
            errorMessage = "Selecciona un cliente para continuar."
            return
        }
        val metodo = selectedMetodo ?: run {
            errorMessage = "Selecciona un método de pago."
            return
        }
        val monto = montoInput.trim()
        if (monto.isEmpty() || monto == "0" || monto == "0.00") {
            errorMessage = "Ingresa un monto válido."
            return
        }

        val settings = repository.getSettings()
        isSubmitting = true
        errorMessage = null

        val ref = if (referenciaInput.trim().isEmpty()) {
            "REC-${System.currentTimeMillis() % 100000}"
        } else {
            referenciaInput.trim()
        }

        scope.launch {
            val result = apiService.registrarPago(
                settings = settings,
                idCliente = cliente.id,
                monto = monto,
                metodo = metodo.nombre,
                referencia = ref
            )
            isSubmitting = false

            result.onSuccess { response ->
                if (response.success && response.data != null) {
                    resultadoExitoso = response.data
                } else {
                    errorMessage = response.mensaje.ifEmpty { "Error al registrar el pago" }
                }
            }.onFailure { ex ->
                errorMessage = ex.localizedMessage ?: "Error de conexión al registrar pago."
            }
        }
    }

    // Resetear formulario para nuevo pago
    fun resetForm() {
        selectedCliente = null
        clienteToPreview = null
        searchResults = emptyList()
        searchQuery = ""
        montoInput = ""
        efectivoRecibidoInput = ""
        referenciaInput = ""
        resultadoExitoso = null
        errorMessage = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Registrar Pago de Cliente",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // paso 1: Buscar Cliente
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "1. Buscar Cliente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Nombre o ID del cliente") },
                    placeholder = { Text("Ej. Ricardo Escobedo o 122") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null)
                    },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp).width(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                searchResults = emptyList()
                                errorMessage = null
                            }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                            }
                        } else {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Lista de resultados encontrados
                if (searchResults.isNotEmpty()) {
                    Text(
                        text = "Resultados encontrados (${searchResults.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        searchResults.forEach { cliente ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { clienteToPreview = cliente },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedCliente?.id == cliente.id) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cliente.nombreCompleto,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "Base: $${cliente.precioMensualBase}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Text(
                                        text = "ID: #${cliente.id} | Saldo: $${cliente.saldoActual} MXN",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    if (cliente.nombreComunidad.isNotEmpty()) {
                                        Text(
                                            text = "Comunidad: ${cliente.nombreComunidad}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    if (cliente.fechaUltimoPago.isNotEmpty()) {
                                        Text(
                                            text = "Último pago: ${formatFechaSimple(cliente.fechaUltimoPago)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Cliente Seleccionado
                if (selectedCliente != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = selectedCliente!!.nombreCompleto,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "ID Cliente: #${selectedCliente!!.id}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (selectedCliente!!.nombreComunidad.isNotEmpty()) {
                                        Text(
                                            text = "Comunidad: ${selectedCliente!!.nombreComunidad}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (selectedCliente!!.fechaUltimoPago.isNotEmpty()) {
                                        Text(
                                            text = "Último Pago: ${formatFechaSimple(selectedCliente!!.fechaUltimoPago)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            IconButton(onClick = { selectedCliente = null }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Quitar selección")
                            }
                        }
                    }
                }
            }
        }

        // Paso 2 y 3: Método de Pago, Monto y Calculadora de Cambio
        AnimatedVisibility(visible = selectedCliente != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "2. Detalles del Pago",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Select Método de Pago
                    ExposedDropdownMenuBox(
                        expanded = isDropdownExpanded,
                        onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedMetodo?.nombre ?: "Selecciona método de pago",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Método de Pago") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                            leadingIcon = { Icon(imageVector = Icons.Default.CreditCard, contentDescription = null) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            metodosPago.forEach { metodo ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(text = metodo.nombre, fontWeight = FontWeight.Bold)
                                            if (metodo.descripcion.isNotEmpty()) {
                                                Text(text = metodo.descripcion, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedMetodo = metodo
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Input Monto a pagar
                    OutlinedTextField(
                        value = montoInput,
                        onValueChange = { montoInput = it },
                        label = { Text("Monto a Pagar (MXN)") },
                        placeholder = { Text("250.00") },
                        leadingIcon = { Icon(imageVector = Icons.Default.AttachMoney, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Input Efectivo Recibido (Calculadora de cambio para el cajero)
                    OutlinedTextField(
                        value = efectivoRecibidoInput,
                        onValueChange = { efectivoRecibidoInput = it },
                        label = { Text("Efectivo Entregado por Cliente (Cálculo de cambio)") },
                        placeholder = { Text("Ej. 500.00") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Payments, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Visualización del Cambio a Entregar
                    if (efectivoDouble > 0) {
                        if (efectivoDouble >= montoDouble) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Cambio a Entregar:",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "$$cambioFormateado MXN",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Monto insuficiente (Faltan $${String.format(Locale.US, "%.2f", montoDouble - efectivoDouble)} MXN)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    // Input Referencia
                    OutlinedTextField(
                        value = referenciaInput,
                        onValueChange = { referenciaInput = it },
                        label = { Text("Referencia de Pago") },
                        placeholder = { Text("Ej. REC-00123") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Description, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Botón Procesar Pago
                    Button(
                        onClick = { handleRegistrarPago() },
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp).width(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Registrando Pago...")
                        } else {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirmar y Registrar Pago")
                        }
                    }
                }
            }
        }

        // Mensajes de Error
        if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Modal de Éxito tipo Transferencia de Banco con información del cambio
    if (resultadoExitoso != null && selectedCliente != null) {
        ExitoPagoDialog(
            data = resultadoExitoso!!,
            cliente = selectedCliente!!,
            metodo = selectedMetodo?.nombre ?: "Efectivo",
            efectivoRecibido = efectivoRecibidoInput,
            cambioEntregado = if (efectivoDouble >= montoDouble && efectivoDouble > 0) cambioFormateado else null,
            onDismiss = { resetForm() },
            onOpenPdf = { url ->
                try {
                    val settings = repository.getSettings()
                    val baseUrl = settings.urlBase.trim().removeSuffix("/")
                    val fullPdfUrl = if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
                        url
                    } else {
                        "$baseUrl/${url.removePrefix("/")}"
                    }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullPdfUrl))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback
                }
            }
        )
    }

    // Modal de Resumen y Confirmación de Cliente
    if (clienteToPreview != null) {
        ResumenClienteDialog(
            cliente = clienteToPreview!!,
            onDismiss = { clienteToPreview = null },
            onConfirm = { cliente ->
                selectCliente(cliente)
                clienteToPreview = null
            }
        )
    }
}

@Composable
private fun ResumenClienteDialog(
    cliente: ClienteSearchResult,
    onDismiss: () -> Unit,
    onConfirm: (ClienteSearchResult) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .height(48.dp)
                        .width(48.dp)
                )
                Text(
                    text = "Resumen del Cliente",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = cliente.nombreCompleto,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ItemReciboRow(label = "ID Cliente", value = "#${cliente.id}")
                        ItemReciboRow(
                            label = "Comunidad",
                            value = cliente.nombreComunidad.ifEmpty { "No especificada" },
                            isHighlight = cliente.nombreComunidad.isNotEmpty()
                        )
                        if (cliente.telefono.isNotEmpty()) {
                            ItemReciboRow(label = "Teléfono", value = cliente.telefono)
                        }
                        ItemReciboRow(label = "Saldo Actual", value = "$${cliente.saldoActual} MXN")
                        ItemReciboRow(label = "Precio Mensual Base", value = "$${cliente.precioMensualBase} MXN")
                        ItemReciboRow(
                            label = "Último Pago",
                            value = formatFechaSimple(cliente.fechaUltimoPago)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onConfirm(cliente) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirmar Cliente")
                }

                OutlinedButton(
                    onClick = { onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar")
                }
            }
        }
    )
}

@Composable
private fun ExitoPagoDialog(
    data: ResultadoPagoData,
    cliente: ClienteSearchResult,
    metodo: String,
    efectivoRecibido: String,
    cambioEntregado: String?,
    onDismiss: () -> Unit,
    onOpenPdf: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icono grande de transferencia bancaria exitosa
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(64.dp).width(64.dp)
                )

                Text(
                    text = "¡Pago Registrado Exitosamente!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "ID de Pago: #${data.idPago}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )

                HorizontalDivider()

                // Tarjeta de recibo bancario
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ItemReciboRow(label = "Cliente", value = cliente.nombreCompleto)
                        ItemReciboRow(label = "ID Cliente", value = "#${cliente.id}")
                        if (cliente.nombreComunidad.isNotEmpty()) {
                            ItemReciboRow(label = "Comunidad", value = cliente.nombreComunidad)
                        }
                        ItemReciboRow(label = "Monto Pagado", value = "$${data.montoPagado} MXN", isHighlight = true)
                        ItemReciboRow(label = "Método", value = metodo)

                        if (efectivoRecibido.isNotEmpty()) {
                            ItemReciboRow(label = "Efectivo Recibido", value = "$$efectivoRecibido MXN")
                        }

                        if (cambioEntregado != null) {
                            ItemReciboRow(label = "Cambio Entregado", value = "$$cambioEntregado MXN", isHighlight = true)
                        }

                        ItemReciboRow(label = "Nuevo Saldo", value = "$${data.nuevoSaldo} MXN")
                        ItemReciboRow(label = "Próximo Pago", value = data.proximoPago)
                    }
                }

                // Chips de estado (Internet / WhatsApp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (data.internetReactivado) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Internet Activo") },
                            icon = { Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    if (data.whatsappEnviado) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("WhatsApp Enviado") },
                            icon = { Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null) }
                        )
                    }
                }

                HorizontalDivider()

                // Botón Bajar PDF Recibo
                Button(
                    onClick = { onOpenPdf(data.urlReciboPdf) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bajar / Ver Recibo PDF")
                }

                // Botón Nuevo Pago / Cerrar
                OutlinedButton(
                    onClick = { onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Realizar Nuevo Pago")
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun ItemReciboRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isHighlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatFechaSimple(rawFecha: String): String {
    if (rawFecha.isBlank() || rawFecha.equals("null", ignoreCase = true)) {
        return "Sin registros previos"
    }
    return try {
        val cleanFecha = rawFecha.trim()
        val inputFormat = if (cleanFecha.contains(":")) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        }
        val date = inputFormat.parse(cleanFecha) ?: return rawFecha
        val localeEs = Locale.forLanguageTag("es-MX")
        val outputFormat = SimpleDateFormat("dd MMM yyyy", localeEs)
        outputFormat.format(date)
            .replace(".", "")
            .split(" ")
            .joinToString(" ") { word ->
                if (word.isNotEmpty() && word[0].isLowerCase()) {
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeEs) else it.toString() }
                } else word
            }
    } catch (e: Exception) {
        rawFecha
    }
}
