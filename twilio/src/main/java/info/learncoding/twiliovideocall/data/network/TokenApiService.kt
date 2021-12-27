package info.learncoding.twiliovideocall.data.network

import info.learncoding.twiliovideocall.data.model.Token
import retrofit2.http.GET
import retrofit2.http.Url

interface TokenApiService {
    @GET
    suspend fun fetchToken(
        @Url url: String
    ): Token
}