package unidesk.com.br.keymanager.keytool

sealed class KeytoolResult<out T> {
    data class Success<T>(val data: T) : KeytoolResult<T>()
    data class Error(val message: String, val exitCode: Int) : KeytoolResult<Nothing>()
}