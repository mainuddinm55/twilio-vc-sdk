package info.learncoding.twiliovideocall.di

import android.content.Context
import com.twilio.audioswitch.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import info.learncoding.twiliovideocall.data.network.TokenApiService
import info.learncoding.twiliovideocall.data.repository.VideoCallRepository
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AppModule {

    @Provides
    @Singleton
    @TwilioSDK
    fun provideContext(@ApplicationContext context: Context) = context

    @Singleton
    @Provides
    @TwilioSDK
    fun provideCache(@TwilioSDK context: Context): Cache {
        val cacheSize = 5L * 1024L * 1024L // 5 MB
        return Cache(File(context.cacheDir, "${context.packageName}.cache"), cacheSize)
    }

    @Provides
    @Singleton
    @TwilioSDK
    fun provideHttpClient(@TwilioSDK cache: Cache): OkHttpClient {
        return OkHttpClient.Builder().apply {
            cache(cache)
            readTimeout(1, TimeUnit.MINUTES)
            writeTimeout(1, TimeUnit.MINUTES)
            connectTimeout(1, TimeUnit.MINUTES)
            callTimeout(1, TimeUnit.MINUTES)
            if (BuildConfig.DEBUG) {
                val httpLoggingInterceptor = HttpLoggingInterceptor()
                val logging = httpLoggingInterceptor.apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
                addInterceptor(logging)
            }
        }.build()
    }

    @Provides
    @Singleton
    @TwilioSDK
    fun provideTokenApiService(@TwilioSDK httpClient: OkHttpClient): TokenApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.maya-apa.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
            .create(TokenApiService::class.java)
    }

    @Provides
    @Singleton
    @TwilioSDK
    fun provideVideoCallRepository(@TwilioSDK tokenApiService: TokenApiService): VideoCallRepository {
        return VideoCallRepository(tokenApiService)
    }
}