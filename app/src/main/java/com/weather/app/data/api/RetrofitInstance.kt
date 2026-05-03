package com.weather.app.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton that provides a configured Retrofit instance pointing at the
 * OpenWeatherMap Data 2.5 base URL.
 *
 * How it works:
 *  1. An [OkHttpClient] is built with a 30-second timeout and (in debug builds)
 *     an [HttpLoggingInterceptor] that prints full request/response bodies.
 *  2. [Retrofit] wraps that client and uses Gson to convert JSON responses into
 *     Kotlin data classes automatically.
 *  3. [weatherApiService] is a lazy singleton so the heavy Retrofit setup only
 *     runs once.
 */
object RetrofitInstance {

    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"

    /** OkHttp client shared by all requests. */
    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /** The Retrofit instance – only created once thanks to `by lazy`. */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /** Ready-to-use API service interface. */
    val weatherApiService: WeatherApiService by lazy {
        retrofit.create(WeatherApiService::class.java)
    }
}
