package com.weather.app.data.api

import com.weather.app.data.model.ForecastResponse
import com.weather.app.data.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface defining the OpenWeatherMap API endpoints used by this app.
 *
 * Base URL: https://api.openweathermap.org/data/2.5/
 */
interface WeatherApiService {

    /**
     * Fetch current weather for a city name.
     *
     * @param city     City name (e.g. "London")
     * @param apiKey   Your OpenWeatherMap API key
     * @param units    "metric" returns Celsius, "imperial" returns Fahrenheit
     */
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("q")     city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse

    /**
     * Fetch 5-day / 3-hour forecast for a city name.
     *
     * @param city     City name
     * @param apiKey   Your OpenWeatherMap API key
     * @param units    "metric" returns Celsius
     */
    @GET("forecast")
    suspend fun getForecast(
        @Query("q")     city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): ForecastResponse

    /**
     * Fetch current weather by geographic coordinates (used for GPS-based location).
     *
     * @param lat    Latitude
     * @param lon    Longitude
     * @param apiKey Your OpenWeatherMap API key
     * @param units  "metric" for Celsius
     */
    @GET("weather")
    suspend fun getCurrentWeatherByLocation(
        @Query("lat")   lat: Double,
        @Query("lon")   lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse

    /**
     * Fetch 5-day forecast by geographic coordinates.
     */
    @GET("forecast")
    suspend fun getForecastByLocation(
        @Query("lat")   lat: Double,
        @Query("lon")   lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): ForecastResponse
}
