package softwarescobedo.com.mx.app_1.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import softwarescobedo.com.mx.app_1.data.AppSettings
import softwarescobedo.com.mx.app_1.data.CajeroStats
import softwarescobedo.com.mx.app_1.data.ClienteSearchResult
import softwarescobedo.com.mx.app_1.data.MetodoPago
import softwarescobedo.com.mx.app_1.data.PagoHistorial
import softwarescobedo.com.mx.app_1.data.RespuestaRegistroPago
import softwarescobedo.com.mx.app_1.data.ResultadoPagoData
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ApiService {

    private fun formatBaseUrl(rawUrl: String): String {
        var baseUrl = rawUrl.trim()
        if (baseUrl.isEmpty()) {
            return "https://miwispro.net"
        }
        if (!baseUrl.startsWith("http://", ignoreCase = true) && !baseUrl.startsWith("https://", ignoreCase = true)) {
            val isIpOrLocal = baseUrl.matches(Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(?::\\d+)?(?:/.*)?$")) || baseUrl.startsWith("localhost", ignoreCase = true)
            baseUrl = if (isIpOrLocal) "http://$baseUrl" else "https://$baseUrl"
        }
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.dropLast(1)
        }
        return baseUrl
    }

    suspend fun getCajeroStats(settings: AppSettings): Result<CajeroStats> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = formatBaseUrl(settings.urlBase)
                val fullUrl = "$baseUrl/api/app_cajero_stats.php?token=${settings.token}&subdominio=${settings.subdominio}&id_usuario=${settings.userId}"

                val url = URL(fullUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doInput = true
                    setRequestProperty("Accept", "application/json")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(responseText)
                    if (jsonObject.optBoolean("success", false) && jsonObject.has("data")) {
                        val dataObj = jsonObject.getJSONObject("data")
                        val stats = CajeroStats(
                            totalRecibos = dataObj.optInt("total_recibos", 0),
                            dineroRecaudado = dataObj.optString("dinero_recaudado", "0.00")
                        )
                        Result.success(stats)
                    } else {
                        Result.failure(Exception(jsonObject.optString("message", "Error al obtener estadísticas")))
                    }
                } else {
                    Result.failure(Exception("Error HTTP: ${connection.responseCode}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getHistorialPagos(settings: AppSettings): Result<List<PagoHistorial>> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = formatBaseUrl(settings.urlBase)
                val fullUrl = "$baseUrl/api/get_historial_pagos.php?token=${settings.token}&subdominio=${settings.subdominio}&id_usuario=${settings.userId}"

                val url = URL(fullUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doInput = true
                    setRequestProperty("Accept", "application/json")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(responseText)
                    if (jsonObject.optBoolean("success", false) && jsonObject.has("data")) {
                        val dataArray = jsonObject.getJSONArray("data")
                        val listaPagos = mutableListOf<PagoHistorial>()

                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            listaPagos.add(
                                PagoHistorial(
                                    id = item.optInt("id", 0),
                                    idCliente = item.optInt("id_cliente", 0),
                                    idFactura = if (item.isNull("id_factura")) null else item.optInt("id_factura"),
                                    idPaquete = if (item.isNull("id_paquete")) null else item.optInt("id_paquete"),
                                    idServicioExtra = if (item.isNull("id_servicio_extra")) null else item.optInt("id_servicio_extra"),
                                    idUsuario = item.optInt("id_usuario", 0),
                                    montoPagado = item.optString("monto_pagado", "0.00"),
                                    cargosExtra = item.optDouble("cargos_extra", 0.0),
                                    metodoPago = item.optString("metodo_pago", ""),
                                    fechaPago = item.optString("fecha_pago", ""),
                                    periodoCubierto = item.optString("periodo_cubierto", ""),
                                    nombreCliente = item.optString("nombre_cliente", ""),
                                    nombrePlan = item.optString("nombre_plan", ""),
                                    nombreCajero = item.optString("nombre_cajero", "")
                                )
                            )
                        }
                        Result.success(listaPagos.sortedByDescending { it.id })
                    } else {
                        Result.failure(Exception(jsonObject.optString("message", "Error al obtener historial")))
                    }
                } else {
                    Result.failure(Exception("Error HTTP: ${connection.responseCode}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun buscarClientes(settings: AppSettings, query: String): Result<List<ClienteSearchResult>> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = formatBaseUrl(settings.urlBase)
                val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
                val fullUrl = "$baseUrl/api/app_cajero_buscar_cliente.php?token=${settings.token}&subdominio=${settings.subdominio}&query=$encodedQuery"

                val url = URL(fullUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doInput = true
                    setRequestProperty("Accept", "application/json")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(responseText)
                    if (jsonObject.optBoolean("success", false) && jsonObject.has("data")) {
                        val dataArray = jsonObject.getJSONArray("data")
                        val listaClientes = mutableListOf<ClienteSearchResult>()

                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            listaClientes.add(
                                ClienteSearchResult(
                                    id = item.optInt("id", 0),
                                    nombreCompleto = item.optString("nombre_completo", ""),
                                    saldoActual = item.optString("saldo_actual", "0.00"),
                                    precioMensualBase = item.optString("precio_mensual_base", "0.00")
                                )
                            )
                        }
                        Result.success(listaClientes)
                    } else {
                        Result.failure(Exception(jsonObject.optString("message", "Sin resultados de clientes")))
                    }
                } else {
                    Result.failure(Exception("Error HTTP: ${connection.responseCode}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getMetodosPago(settings: AppSettings): Result<List<MetodoPago>> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = formatBaseUrl(settings.urlBase)
                val fullUrl = "$baseUrl/api/get_metodos_pago.php?token=${settings.token}&subdominio=${settings.subdominio}"

                val url = URL(fullUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doInput = true
                    setRequestProperty("Accept", "application/json")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(responseText)
                    if (jsonObject.optBoolean("success", false) && jsonObject.has("data")) {
                        val dataArray = jsonObject.getJSONArray("data")
                        val listaMetodos = mutableListOf<MetodoPago>()

                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            listaMetodos.add(
                                MetodoPago(
                                    id = item.optInt("id", 0),
                                    nombre = item.optString("nombre", ""),
                                    descripcion = item.optString("descripcion", ""),
                                    activo = item.optInt("activo", 1)
                                )
                            )
                        }
                        Result.success(listaMetodos.filter { it.activo == 1 })
                    } else {
                        Result.failure(Exception(jsonObject.optString("message", "Error al obtener métodos de pago")))
                    }
                } else {
                    Result.failure(Exception("Error HTTP: ${connection.responseCode}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun registrarPago(
        settings: AppSettings,
        idCliente: Int,
        monto: String,
        metodo: String,
        referencia: String
    ): Result<RespuestaRegistroPago> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = formatBaseUrl(settings.urlBase)
                val encodedMetodo = URLEncoder.encode(metodo, "UTF-8")
                val encodedReferencia = URLEncoder.encode(referencia, "UTF-8")
                val fullUrl = "$baseUrl/api/registrar_pago.php?token=${settings.token}&subdominio=${settings.subdominio}&id=$idCliente&monto=$monto&metodo=$encodedMetodo&referencia=$encodedReferencia&id_usuario=${settings.userId}"

                val url = URL(fullUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                    doInput = true
                    setRequestProperty("Accept", "application/json")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(responseText)
                    val success = jsonObject.optBoolean("success", false)
                    val mensaje = jsonObject.optString("mensaje", "")

                    var resultadoData: ResultadoPagoData? = null
                    if (jsonObject.has("data") && !jsonObject.isNull("data")) {
                        val dataObj = jsonObject.getJSONObject("data")
                        resultadoData = ResultadoPagoData(
                            idPago = dataObj.optInt("id_pago", 0),
                            urlReciboPdf = dataObj.optString("url_recibo_pdf", ""),
                            montoPagado = dataObj.optString("monto_pagado", "0.00"),
                            nuevoSaldo = dataObj.optString("nuevo_saldo", "0.00"),
                            proximoPago = dataObj.optString("proximo_pago", ""),
                            internetReactivado = dataObj.optBoolean("internet_reactivado", false),
                            whatsappEnviado = dataObj.optBoolean("whatsapp_enviado", false)
                        )
                    }

                    Result.success(
                        RespuestaRegistroPago(
                            success = success,
                            mensaje = mensaje,
                            data = resultadoData
                        )
                    )
                } else {
                    Result.failure(Exception("Error HTTP: ${connection.responseCode}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
