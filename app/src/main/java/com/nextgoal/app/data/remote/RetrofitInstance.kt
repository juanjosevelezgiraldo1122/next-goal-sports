package com.nextgoal.app.data.remote

import com.nextgoal.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Adds the free football-data.org API token (X-Auth-Token header) to every request.
 * Get your own free key at https://www.football-data.org/client/register and put it
 * in local.properties as FOOTBALL_DATA_API_KEY=xxxxx (see local.properties.example).
 */
private class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("X-Auth-Token", BuildConfig.FOOTBALL_DATA_API_KEY)
            .build()
        return chain.proceed(request)
    }
}

object RetrofitInstance {

    private const val BASE_URL = "https://api.football-data.org/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        redactHeader("X-Auth-Token")
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val api: FootballApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FootballApiService::class.java)
    }
}
