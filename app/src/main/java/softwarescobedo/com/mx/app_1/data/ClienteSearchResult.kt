package softwarescobedo.com.mx.app_1.data

data class ClienteSearchResult(
    val id: Int,
    val nombreCompleto: String,
    val saldoActual: String,
    val precioMensualBase: String,
    val telefono: String = "",
    val comunidad: String = "",
    val nombreComunidad: String = "",
    val fechaUltimoPago: String = ""
)

