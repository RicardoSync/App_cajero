package softwarescobedo.com.mx.app_1.data

data class PagoHistorial(
    val id: Int,
    val idCliente: Int,
    val idFactura: Int?,
    val idPaquete: Int?,
    val idServicioExtra: Int?,
    val idUsuario: Int,
    val montoPagado: String,
    val cargosExtra: Double,
    val metodoPago: String,
    val fechaPago: String,
    val periodoCubierto: String,
    val nombreCliente: String,
    val nombrePlan: String,
    val nombreCajero: String
)
