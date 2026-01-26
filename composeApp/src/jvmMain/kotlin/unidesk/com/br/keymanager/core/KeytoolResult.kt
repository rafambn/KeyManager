package unidesk.com.br.keymanager.core
sealed class KeytoolResult<out T> {
    data class Success<T>(val data: T) : KeytoolResult<T>()
    data class Error(val message: String, val exitCode: Int) : KeytoolResult<Nothing>()
}
