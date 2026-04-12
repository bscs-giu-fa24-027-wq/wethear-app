package com.weather.app.data.repository

import com.weather.app.data.api.WeatherApiService
import com.weather.app.data.model.ForecastResponse
import com.weather.app.data.model.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WeatherRepository is the single source of truth for weather data.
 *
 * It sits between the ViewModel and the network layer (Retrofit).
 * Using a repository:
 *  - Keeps ViewModels clean (no network code there)
 *  - Makes it easy to swap the data source later (e.g. add a local cache)
 *  - Runs all network work on the IO dispatcher so the main thread is never blocked
 *
 * @param apiService The Retrofit-backed API service (injected so it can be mocked in tests)
 * @param apiKey     The OpenWeatherMap API key
 */
class WeatherRepository(
    private val apiService: WeatherApiService,
    private val apiKey: String
) {

    /**
     * Fetch current weather for the given [cityName].
     *
     * @return [Result.success] wrapping [WeatherResponse], or [Result.failure] with an exception.
     */
    suspend fun getCurrentWeather(cityName: String): Result<WeatherResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCurrentWeather(
                    city   = cityName.trim(),
                    apiKey = apiKey
                )
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Fetch 5-day forecast for the given [cityName].
     */
    suspend fun getForecast(cityName: String): Result<ForecastResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getForecast(
                    city   = cityName.trim(),
                    apiKey = apiKey
                )
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Fetch current weather by GPS coordinates.
     */
    suspend fun getCurrentWeatherByLocation(lat: Double, lon: Double): Result<WeatherResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCurrentWeatherByLocation(
                    lat    = lat,
                    lon    = lon,
                    apiKey = apiKey
                )
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Fetch 5-day forecast by GPS coordinates.
     */
    suspend fun getForecastByLocation(lat: Double, lon: Double): Result<ForecastResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getForecastByLocation(
                    lat    = lat,
                    lon    = lon,
                    apiKey = apiKey
                )
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
