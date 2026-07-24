package softwarescobedo.com.mx.app_1.data

data class ResultadoPagoData(
    val idPago: Int,
    val urlReciboPdf: String,
    val montoPagado: String,
    val nuevoSaldo: String,
    val proximoPago: String,
    val internetReactivado: Boolean,
    val whatsappEnviado: Boolean
)

data class RespuestaRegistroPago(
    val success: Boolean,
    val mensaje: String,
    val data: ResultadoPagoData?
)
