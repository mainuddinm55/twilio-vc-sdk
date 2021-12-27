package info.learncoding.twiliovideocall.data.repository

import info.learncoding.twiliovideocall.data.network.ApiResponse
import info.learncoding.twiliovideocall.data.network.TokenApiService

class VideoCallRepository(private val tokenApiService: TokenApiService) {

    suspend fun fetchToken(url: String): ApiResponse<String> {
        return try {
            val response = tokenApiService.fetchToken(url)
            if (response.token != null) ApiResponse.Success(response.token)
            else ApiResponse.Error("Token does not found")
        } catch (e: Exception) {
            ApiResponse.Error(e.localizedMessage ?: "Something went wrong")
        }
    }
}