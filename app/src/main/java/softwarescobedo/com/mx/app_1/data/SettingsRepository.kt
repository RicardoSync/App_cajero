package softwarescobedo.com.mx.app_1.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "cajero_app_settings"
        private const val DEFAULT_URL_BASE = "https://miwispro.net/"
        private const val KEY_URL_BASE = "key_url_base"
        private const val KEY_SUBDOMINIO = "key_subdominio"
        private const val KEY_TOKEN = "key_token"
        private const val KEY_USER_ID = "key_user_id"
    }

    /**
     * READ: Obtiene la configuración almacenada localmente.
     */
    fun getSettings(): AppSettings {
        val savedUrl = prefs.getString(KEY_URL_BASE, DEFAULT_URL_BASE)
        return AppSettings(
            urlBase = if (savedUrl.isNullOrBlank()) DEFAULT_URL_BASE else savedUrl.cleanSpaces(),
            subdominio = prefs.getString(KEY_SUBDOMINIO, "")?.cleanSpaces() ?: "",
            token = prefs.getString(KEY_TOKEN, "")?.cleanSpaces() ?: "",
            userId = prefs.getString(KEY_USER_ID, "")?.cleanSpaces() ?: ""
        )
    }

    /**
     * CREATE / UPDATE: Guarda o actualiza la configuración en la base de datos/almacenamiento local,
     * eliminando previamente todos los espacios de los campos de entrada.
     */
    fun saveSettings(settings: AppSettings): Boolean {
        val cleanUrl = settings.urlBase.cleanSpaces()
        val finalUrl = if (cleanUrl.isEmpty()) DEFAULT_URL_BASE else cleanUrl
        val cleanSubdomain = settings.subdominio.cleanSpaces()
        val cleanToken = settings.token.cleanSpaces()
        val cleanUserId = settings.userId.cleanSpaces()

        return prefs.edit()
            .putString(KEY_URL_BASE, finalUrl)
            .putString(KEY_SUBDOMINIO, cleanSubdomain)
            .putString(KEY_TOKEN, cleanToken)
            .putString(KEY_USER_ID, cleanUserId)
            .commit()
    }

    /**
     * DELETE: Elimina la configuración almacenada localmente.
     */
    fun deleteSettings(): Boolean {
        return prefs.edit()
            .remove(KEY_URL_BASE)
            .remove(KEY_SUBDOMINIO)
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .commit()
    }

    /**
     * Check: Verifica si existen datos de token, subdominio o ID usuario guardados.
     */
    fun hasSettings(): Boolean {
        val settings = getSettings()
        return settings.token.isNotEmpty() || settings.userId.isNotEmpty() || settings.subdominio.isNotEmpty()
    }

    private fun String.cleanSpaces(): String {
        return this.replace("\\s+".toRegex(), "")
    }
}
