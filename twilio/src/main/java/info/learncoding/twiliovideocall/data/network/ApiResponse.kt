package info.learncoding.twiliovideocall.data.network

sealed class ApiResponse<out T> {
    data class Success<out T>(val data: T) : ApiResponse<T>()
    data class Error(val msg: String) : ApiResponse<Nothing>()
}