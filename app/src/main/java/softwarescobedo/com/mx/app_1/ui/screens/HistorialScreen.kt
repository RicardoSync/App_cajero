package softwarescobedo.com.mx.app_1.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import softwarescobedo.com.mx.app_1.data.PagoHistorial
import softwarescobedo.com.mx.app_1.data.SettingsRepository
import softwarescobedo.com.mx.app_1.network.ApiService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen() {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val apiService = remember { ApiService() }
    val scope = rememberCoroutineScope()

    var listaOriginal by remember { mutableStateOf<List<PagoHistorial>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Estados de Filtros (Ocultos por defecto tras menú de 3 barritas / Tune)
    var isFilterPanelOpen by remember { mutableStateOf(false) }
    var queryCliente by remember { mutableStateOf("") }
    var queryFecha by remember { mutableStateOf("") }

    // Estado del selector de fecha (Calendario)
    var showDatePicker by remember { mutableStateOf(false) }
    val dateInteractionSource = remember { MutableInteractionSource() }

    LaunchedEffect(dateInteractionSource) {
        dateInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                showDatePicker = true
            }
        }
    }

    // Estado del modal de detalle de pago
    var selectedPago by remember { mutableStateOf<PagoHistorial?>(null) }

    fun fetchHistorial() {
        val settings = repository.getSettings()
        if (settings.token.isEmpty() || settings.userId.isEmpty() || settings.subdominio.isEmpty()) {
            errorMessage = "Configura primero los parámetros en la pantalla de Ajustes."
            return
        }

        isLoading = true
        errorMessage = null

        scope.launch {
            val result = apiService.getHistorialPagos(settings)
            isLoading = false
            result.onSuccess { data ->
                listaOriginal = data
            }.onFailure { ex ->
                errorMessage = ex.localizedMessage ?: "Error al obtener el historial de pagos."
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchHistorial()
    }

    // Filtrar lista según criterios del usuario
    val listaFiltrada = remember(listaOriginal, queryCliente, queryFecha) {
        listaOriginal.filter { pago ->
            val matchCliente = queryCliente.isBlank() ||
                    pago.nombreCliente.contains(queryCliente, ignoreCase = true) ||
                    pago.idCliente.toString().contains(queryCliente.trim())

            val matchFecha = queryFecha.isBlank() ||
                    pago.fechaPago.contains(queryFecha.trim(), ignoreCase = true)

            matchCliente && matchFecha
        }
    }

    val hasActiveFilters = queryCliente.isNotBlank() || queryFecha.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Cabecera superior con contador y botón de filtro (3 barritas / Tune)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Historial de Pagos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${listaFiltrada.size} pagos registrados",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Row {
                IconButton(onClick = { fetchHistorial() }, enabled = !isLoading) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp).width(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Recargar")
                    }
                }

                // Botón de 3 barritas / Ajustes de filtro (Tune)
                IconButton(
                    onClick = { isFilterPanelOpen = !isFilterPanelOpen }
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filtros de búsqueda",
                        tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Panel de Filtros Desplegable (Oculto por defecto para no ocupar espacio)
        AnimatedVisibility(visible = isFilterPanelOpen) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filtros de Búsqueda",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (hasActiveFilters) {
                            TextButton(
                                onClick = {
                                    queryCliente = ""
                                    queryFecha = ""
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Limpiar")
                            }
                        }
                    }

                    // 1. Filtro por Cliente (Nombre o ID)
                    OutlinedTextField(
                        value = queryCliente,
                        onValueChange = { queryCliente = it },
                        label = { Text("Nombre o ID del cliente") },
                        placeholder = { Text("Ej. Daniel o 51") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null)
                        },
                        trailingIcon = {
                            if (queryCliente.isNotEmpty()) {
                                IconButton(onClick = { queryCliente = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 2. Filtro por Fecha (Selector con calendario)
                    OutlinedTextField(
                        value = queryFecha,
                        onValueChange = { },
                        readOnly = true,
                        interactionSource = dateInteractionSource,
                        label = { Text("Fecha de pago") },
                        placeholder = { Text("Seleccionar fecha") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Seleccionar fecha",
                                modifier = Modifier.clickable { showDatePicker = true }
                            )
                        },
                        trailingIcon = {
                            if (queryFecha.isNotEmpty()) {
                                IconButton(onClick = { queryFecha = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar fecha")
                                }
                            } else {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Abrir calendario")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Chip indicador de filtros activos
        if (hasActiveFilters && !isFilterPanelOpen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = { isFilterPanelOpen = true },
                    label = { Text("Filtros activos: ${if (queryCliente.isNotBlank()) "Cliente '$queryCliente' " else ""}${if (queryFecha.isNotBlank()) "Fecha '$queryFecha'" else ""}") },
                    icon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) }
                )
            }
        }

        // Manejo de Error
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
                    OutlinedButton(onClick = { fetchHistorial() }) {
                        Text("Reintentar")
                    }
                }
            }
        }

        // Lista de Pagos (Ordenados del más reciente al más viejo)
        if (isLoading && listaOriginal.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (listaFiltrada.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ListAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (hasActiveFilters) "No se encontraron pagos con los filtros aplicados" else "No hay registros de pago en el historial",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = listaFiltrada,
                    key = { it.id }
                ) { pago ->
                    ItemPagoCard(
                        pago = pago,
                        onClick = { selectedPago = pago }
                    )
                }
            }
        }
    }

    // Modal AlertDialog con la información detallada del pago al hacer clic
    if (selectedPago != null) {
        DetallePagoDialog(
            pago = selectedPago!!,
            onDismiss = { selectedPago = null }
        )
    }

    // Diálogo Selector de Fecha (Calendario)
    if (showDatePicker) {
        val initialMillis = remember(queryFecha) {
            if (queryFecha.isNotBlank()) {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    sdf.parse(queryFecha.trim())?.time
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            queryFecha = formatter.format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ItemPagoCard(
    pago: PagoHistorial,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pago.nombreCliente,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ID Cliente: ${pago.idCliente}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Text(
                    text = "$${pago.montoPagado} MXN",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = pago.nombrePlan,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Método: ${pago.metodoPago}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = pago.fechaPago,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun DetallePagoDialog(
    pago: PagoHistorial,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        icon = {
            Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = {
            Text(
                text = "Detalle del Pago #${pago.id}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider()

                ItemDetalleRow(label = "Cliente", value = "${pago.nombreCliente} (ID: ${pago.idCliente})")
                ItemDetalleRow(label = "Monto Pagado", value = "$${pago.montoPagado} MXN")
                ItemDetalleRow(label = "Método de Pago", value = pago.metodoPago)
                ItemDetalleRow(label = "Fecha de Pago", value = pago.fechaPago)
                ItemDetalleRow(label = "Periodo Cubierto", value = pago.periodoCubierto)
                ItemDetalleRow(label = "Plan / Paquete", value = pago.nombrePlan)
                ItemDetalleRow(label = "Cajero que Cobró", value = pago.nombreCajero)

                if (pago.cargosExtra > 0) {
                    ItemDetalleRow(label = "Cargos Extra", value = "$${pago.cargosExtra} MXN")
                }

                HorizontalDivider()
            }
        },
        confirmButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
private fun ItemDetalleRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
